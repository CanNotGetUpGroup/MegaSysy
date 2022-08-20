package pass.passes;

import analysis.AliasAnalysis;
import analysis.DominatorTree;
import ir.*;
import ir.Module;
import org.antlr.v4.runtime.misc.Pair;
import pass.FunctionPass;
import ir.instructions.Instructions.*;
import analysis.DominatorTree.TreeNode;

import java.util.*;

/**
 * 对于指向非数组元素的alloca指令：
 * 删除未被使用的alloca
 * 直接处理掉只store过一次，或所有store和load在一个基本块的alloca指令
 * 计算插入phi指令的位置
 * 分析CFG，为phi指令设置incomingValue以及对应基本块
 */
public class Mem2Reg extends FunctionPass {
    private final HashMap<BasicBlock, Integer> BBNumbers = new HashMap<>();
    private final HashMap<Pair<Integer, Integer>, PHIInst> PhiNodes = new HashMap<>();
    private final HashMap<AllocaInst, Integer> AllocaLookup = new HashMap<>();
    private int NumPHIInsert = 0;
    private final HashMap<PHIInst, Integer> PhiToAllocaMap = new HashMap<>();
    private ArrayList<AllocaInst> allocaInsts;
    private final Set<BasicBlock> Visited = new HashSet<>();
    private int Version = 0;

    public Mem2Reg() {
        super();
    }

    @Override
    public String getName() {
        return "Mem2Reg";
    }

    @Override
    public void runOnFunction(Function F) {
        BBNumbers.clear();
        PhiNodes.clear();
        AllocaLookup.clear();
        PhiToAllocaMap.clear();
        Visited.clear();
         AliasAnalysis.gepToArrayIdx.clear();

        DominatorTree DT = F.getAndUpdateDominatorTree();
        promoteMem2Reg(F, DT);
        Module.getInstance().rename(F);
    }

    public static FunctionPass createMem2Reg() {
        return new Mem2Reg();
    }

    public void promoteMem2Reg(Function F, DominatorTree DT) {
        ArrayList<AllocaInst> allocaInsts = new ArrayList<>();
        BasicBlock BB = F.getEntryBB();

        while (true) {
            allocaInsts.clear();
            for (Instruction I : BB.getInstList()) {
                if (I instanceof AllocaInst) {
                    AllocaInst AI = (AllocaInst) I;
                    if (isAllocaPromotable(AI)) {
                        allocaInsts.add(AI);
                    }
                }
            }
            if (allocaInsts.isEmpty()) {
                break;
            }
            promoteMemoryToRegister(allocaInsts, DT);
        }
    }

    public void promoteMemoryToRegister(ArrayList<AllocaInst> AllocaInsts, DominatorTree DT) {
        allocaInsts = AllocaInsts;
        if (allocaInsts.isEmpty()) {
            return;
        }
        for (int i = 0; i < allocaInsts.size(); i++) {
            AllocaInst AI = allocaInsts.get(i);

            // 删除未使用的ALLOC
            if (AI.getUseList().isEmpty()) {
                AI.remove();
                allocaInsts.remove(i);
                i--;
                continue;
            }

            analyzeAlloca(AI);

            // 如果只有一个定义基本块（只有一个STORE语句，且只存在一个基本块中），
            // 那么被这个定义基本块所支配的所有LOAD都要被替换为STORE语句中的那个右值。
            if (AI.definingBlocks.size() == 1 && rewriteAlloca(AI, DT)) {
                allocaInsts.remove(i);
                i--;
                continue;
            }

            // 如果某个ALLOC出来的局部变量的读或者写都只存在一个基本块中，那么我们就
            // 没必要去遍历所有的CFG的，因为这个store语句支配了这些LOAD，
            // 所以可以用STORE的右值直接替换使用LOAD指令的那些值。
            if (AI.onlyUsedInOne && promoteAlloca(AI, DT)) {
                allocaInsts.remove(i);
                i--;
                continue;
            }

            Set<BasicBlock> LiveBB = new HashSet<>();
            computeLiveBB(AI, LiveBB);

            // 计算插入phi位置
            ArrayList<BasicBlock> PHIBasicBlocks = new ArrayList<>();
            IDFCalculate(DT, AI.definingBlocks, LiveBB, PHIBasicBlocks);

            if (BBNumbers.size() == 0) {
                int ID = 0;
                for (BasicBlock basicBlock : DT.Parent.getBbList()) {
                    BBNumbers.put(basicBlock, ID++);
                }
            }
            AllocaLookup.put(AI, i);
            // 升序排列，便于处理
            PHIBasicBlocks.sort(new Comparator<BasicBlock>() {
                @Override
                public int compare(BasicBlock o1, BasicBlock o2) {
                    return BBNumbers.get(o1) - BBNumbers.get(o2);
                }
            });
            Version = 0;
            for (BasicBlock BB : PHIBasicBlocks) {
                QueuePhiNode(BB, i);
            }
        }
        if (allocaInsts.isEmpty()) {
            return;
        }

        ArrayList<Value> Values = new ArrayList<>();
        for (AllocaInst allocaInst : allocaInsts) {
            Values.add(Constants.UndefValue.get(allocaInst.getAllocatedType()));
        }
        Stack<RenamePassData> RenamePassWorkList = new Stack<>();
        RenamePassWorkList.add(new RenamePassData(DT.Parent.getEntryBB(), null, Values));
        do {
            RenamePassData RPD = RenamePassWorkList.pop();
            RenamePass(RPD.BB, RPD.Pred, RPD.Values, RenamePassWorkList);
        } while (!RenamePassWorkList.isEmpty());

        Visited.clear();
        // 清除alloca指令
        for (Instruction A : allocaInsts) {
            if (A.getUseSize() != 0)
                A.replaceAllUsesWith(Constants.UndefValue.get(A.getType()));
            A.remove();
        }

        // boolean EliminatedAPHI = true;
        // while (EliminatedAPHI) {
        // EliminatedAPHI = false;
        //
        // for (Map.Entry<Pair<Integer, Integer>, PHIInst> cur : PhiNodes.entrySet()) {
        // PHIInst PN = cur.getValue();
        // Value V = SimplifyPHI(PN,DT);
        // if (V != null) {
        // PN.replaceAllUsesWith(V);
        // PN.remove();
        // PhiNodes.remove(cur.getKey());
        // EliminatedAPHI = true;
        // }
        // }
        // }

        // 处理遍历不到的基本块
        for (Map.Entry<Pair<Integer, Integer>, PHIInst> I : PhiNodes.entrySet()) {
            PHIInst SomePHI = I.getValue();
            BasicBlock BB = SomePHI.getParent();
            if (BB.front() != SomePHI)
                continue;

            if (SomePHI.getNumOperands() == BB.getPredecessorsNum())
                continue;

            ArrayList<BasicBlock> Preds = BB.getPredecessors();

            Preds.sort(new Comparator<BasicBlock>() {
                @Override
                public int compare(BasicBlock o1, BasicBlock o2) {
                    return BBNumbers.get(o1) - BBNumbers.get(o2);
                }
            });

            for (int i = 0; i < SomePHI.getNumOperands(); i++) {
                Preds.remove(SomePHI.getIncomingBlock(i));
            }

            int NumBadPreds = SomePHI.getNumOperands();
            Iterator<Instruction> BBIt = BB.getInstList().iterator();
            Instruction BBI = BBIt.next();
            while (BBI instanceof PHIInst &&
                    SomePHI.getNumOperands() == NumBadPreds) {
                SomePHI = (PHIInst) BBI;
                BBI = BBIt.next();
                Value UndefVal = Constants.UndefValue.get(SomePHI.getType());
                for (BasicBlock Pred : Preds)
                    SomePHI.addIncoming(UndefVal, Pred);
            }
        }
        PhiNodes.clear();
    }

    public static Value SimplifyPHI(PHIInst PN, DominatorTree DT) {
        ArrayList<Value> IncomingValues = PN.getOperandList();
        Value CommonValue = null;
        boolean HasUndefInput = false;
        for (Value Incoming : IncomingValues) {
            if (Incoming == PN)
                continue;
            if (Constants.UndefValue.isUndefValue(Incoming)) {
                HasUndefInput = true;
                continue;
            }
            if (CommonValue != null && Incoming != CommonValue)
                return null; // Not the same, bail out.
            CommonValue = Incoming;
        }

        if (CommonValue == null)
            return Constants.UndefValue.get(PN.getType());

        // TODO:处理Undef
        // if (HasUndefInput) {
        // // If we have a PHI node like phi(X, undef, X), where X is defined by some
        // // instruction, we cannot return X as the result of the PHI node unless it
        // // dominates the PHI block.
        // return valueDominatesPHI(CommonValue, PN, DT) ? CommonValue : null;
        // }

        return CommonValue;
    }

    public static boolean valueDominatesPHI(Value V, PHIInst P, DominatorTree DT) {
        if (!(V instanceof Instruction))
            return true;
        Instruction I = (Instruction) (V);
        if (I.getParent() == null || P.getParent() == null || I.getParent().getParent() == null)
            return false;

        // If we have a DominatorTree then do a precise test.
        // if (DT!=null)
        // return DT.dominates(I, P);

        // Otherwise, if the instruction is in the entry block and is not an invoke,
        // then it obviously dominates all phi nodes.
        return I.getParent().isEntryBlock();
    }

    /**
     * 挑出来store instruction，把要存储的值，与alloca instruction关联起来，方便以后塞进ϕ-instruction 的参数中
     * 挑出来load instruction，看情况替换成前面store instruction要存储的值，或者替换成ϕ-instruction
     * 最后删除store，load指令
     * 
     * @param BB
     * @param Pred
     * @param IncomingVals
     */
    public void RenamePass(BasicBlock BB, BasicBlock Pred, ArrayList<Value> IncomingVals,
            Stack<RenamePassData> Worklist) {
        while (true) {
            /*
             * 如果块中有 φ 指令，则遍历所有先前添加的 φ（注意程序中原来可能也有 φ，这里要和原来的 φ 区分开来）：
             * 假设某个前驱到当前基本块有 NumEdges 条边，则为 φ 指令添加 NumEdges 个来源，值为 IncomingVals[L]，同时设置
             * IncomingVals[L] = Phi
             */
            if (BB.getInstList().getFirst().getVal() instanceof PHIInst) {
                PHIInst APN = (PHIInst) BB.getInstList().getFirst().getVal();
                if (PhiToAllocaMap.containsKey(APN)) {
                    int NewPHINumOperands = APN.getNumOperands();
                    int NumEdges = 0;
                    for (BasicBlock suc : Pred.getSuccessors()) {
                        if (suc == BB) {
                            NumEdges++;
                        }
                    }
                    assert NumEdges >= 1;
                    Iterator<Instruction> PNI = BB.getInstList().iterator();
                    Instruction I = PNI.next();
                    do {
                        int AllocaNo = PhiToAllocaMap.get(APN);

                        // 则为 φ 指令添加 NumEdges 个来源
                        for (int i = 0; i != NumEdges; ++i)
                            APN.addIncoming(IncomingVals.get(AllocaNo), Pred);

                        // 设置 IncomingVals[L] = Phi
                        IncomingVals.set(AllocaNo, APN);
                        // 处理下一个phi
                        I = PNI.next();
                        if (!(I instanceof PHIInst)) {
                            break;
                        }
                        APN = (PHIInst) (I);
                    } while (APN.getNumOperands() == NewPHINumOperands);
                }
            }

            if (!Visited.add(BB)) {
                return;
            }
            /*
             * 如果当前基本块没有重复访问过，则对于基本块内的每条指令
             * 如果当前指令是 load，找到对应的 alloca L，然后替换成对应 store 进去的值，删除这条 load，并将所有 users 里的 load
             * 替换成值 IncomingVals[L]
             * 如果当前指令是 store，找到对应的 alloca L，删除这条 store，并更新数组内的版本 IncomingVals[L] = V
             */
            for (Instruction II : BB.getInstList()) {
                if (II.isTerminator()) {
                    break;
                }
                if (II instanceof LoadInst) {
                    if (!(II.getOperand(0) instanceof AllocaInst)) {
                        continue;
                    }
                    AllocaInst Src = (AllocaInst) (II.getOperand(0));
                    Integer AI = AllocaLookup.getOrDefault(Src, null);
                    if (AI == null)
                        continue;
                    Value V = IncomingVals.get(AI);

                    II.replaceAllUsesWith(V);
                    II.remove();
                } else if (II instanceof StoreInst) {
                    if (!(II.getOperand(1) instanceof AllocaInst)) {
                        continue;
                    }
                    AllocaInst Dest = (AllocaInst) (II.getOperand(1));
                    Integer ai = AllocaLookup.getOrDefault(Dest, null);
                    if (ai == null)
                        continue;
                    IncomingVals.set(ai, II.getOperand(0));
                    II.remove();
                }
            }

            if (BB.getSuccessorsNum() == 0)
                return;

            BasicBlock I = BB.getSuccessor(0);
            Set<BasicBlock> VisitedSuccs = new HashSet<>();

            VisitedSuccs.add(I);
            Pred = BB;
            BasicBlock oldBB = BB;
            BB = I;

            for (int i = 1; i < oldBB.getSuccessorsNum(); i++) {
                I = oldBB.getSuccessor(i);
                if (VisitedSuccs.add(I))
                    Worklist.add(new RenamePassData(I, Pred, new ArrayList<>(IncomingVals)));
            }
        }
    }

    static class RenamePassData {
        public BasicBlock BB;
        public BasicBlock Pred;
        ArrayList<Value> Values;

        public RenamePassData(BasicBlock BB, BasicBlock pred, ArrayList<Value> values) {
            this.BB = BB;
            Pred = pred;
            Values = values;
        }
    }

    public void QueuePhiNode(BasicBlock BB, int AllocaNo) {
        Pair<Integer, Integer> f = new Pair<>(BBNumbers.get(BB), AllocaNo);
        if (PhiNodes.containsKey(f)) {
            return;
        }
        PHIInst PN = PHIInst.create(allocaInsts.get(AllocaNo).getAllocatedType(), BB.getPredecessorsNum(),
                "%" + allocaInsts.get(AllocaNo).getVarName() + "." + String.valueOf(Version++),
                BB.getInstList().getFirst().getVal());

        NumPHIInsert++;
        PhiToAllocaMap.put(PN, AllocaNo);
        PhiNodes.put(f, PN);
    }

    /**
     * IDF（iterated Dominator Frontier），时间复杂度O(n)
     * 参考：https://blog.csdn.net/dashuniuniu/article/details/103389157
     */
    public static void IDFCalculate(DominatorTree DT, ArrayList<BasicBlock> DefBlocks, Set<BasicBlock> LiveBB,
            ArrayList<BasicBlock> IDFBlocks) {
        PriorityQueue<Pair<TreeNode, Pair<Integer, Integer>>> PQ = new PriorityQueue<>((o1, o2) -> o2.b.b - o1.b.b);
        DT.updateDFSNumbers();
        Stack<TreeNode> WorkList = new Stack<>();
        Set<TreeNode> visitedPQ = new HashSet<>();
        Set<TreeNode> visitedWorkList = new HashSet<>();

        for (BasicBlock BB : DefBlocks) {
            TreeNode node = DT.getNode(BB);
            PQ.add(new Pair<>(node, new Pair<>(node.level, node.getDFSInNum())));
            visitedWorkList.add(node);
        }

        while (!PQ.isEmpty()) {
            Pair<TreeNode, Pair<Integer, Integer>> RootPair = PQ.poll();
            TreeNode Root = RootPair.a;
            int RootLevel = RootPair.b.a;

            WorkList.add(Root);
            while (!WorkList.isEmpty()) {
                TreeNode node = WorkList.pop();
                BasicBlock BB = node.BB;
                for (var child : BB.getSuccessors()) {
                    TreeNode childNode = DT.getNode(child);
                    int childLevel = childNode.level;
                    if (childLevel > RootLevel) {
                        continue;
                    }
                    if (visitedPQ.contains(childNode)) {
                        continue;
                    }
                    visitedPQ.add(childNode);
                    if (LiveBB != null && !LiveBB.contains(child)) {
                        continue;
                    }
                    IDFBlocks.add(child);
                    if (!DefBlocks.contains(child)) {
                        PQ.add(new Pair<>(childNode, new Pair<>(childLevel, childNode.getDFSInNum())));
                    }
                }
                for (var domChild : node.Children) {
                    if (!visitedWorkList.contains(domChild)) {
                        visitedWorkList.add(domChild);
                        WorkList.push(domChild);
                    }
                }
            }
        }
    }

    /**
     * 由于存在alloca指令，不算严格形式的SSA，不能直接用alloca的def-use链计算存活基本块，需要找到alloca所有在
     * store前使用的load(因为store相当于一次定义覆盖，store之后的load就是store的存活基本块了)，
     * 这些load所在的基本块就是存活基本块，然后再把它的前驱加入进来（前驱当然也是存活的，alloca总是在根基本块）
     */
    public void computeLiveBB(AllocaInst AI, Set<BasicBlock> LiveBB) {
        ArrayList<BasicBlock> copyUsing = new ArrayList<>(AI.usingBlocks);

        // 检查load前是否store过
        for (int i = 0, e = copyUsing.size(); i != e; i++) {
            BasicBlock basicBlock = copyUsing.get(i);
            // 若当前基本块无定义，说明alloca能在此处存活
            if (!AI.definingBlocks.contains(basicBlock)) {
                continue;
            }
            for (Instruction I : basicBlock.getInstList()) {
                if (I instanceof StoreInst) {
                    StoreInst SI = (StoreInst) I;
                    if (SI.getOperand(1) != AI) {
                        continue;
                    }
                    copyUsing.set(i, copyUsing.get(e - 1));
                    copyUsing.remove(e - 1);
                    --i;
                    --e;
                    break;
                }
                if (I instanceof LoadInst) {
                    LoadInst LI = (LoadInst) I;
                    if (LI.getOperand(0) == AI) {
                        break;
                    }
                }
            }
        }

        // 将前驱加入进来
        while (!copyUsing.isEmpty()) {
            BasicBlock basicBlock = copyUsing.get(copyUsing.size() - 1);
            copyUsing.remove(copyUsing.size() - 1);
            if (LiveBB.contains(basicBlock)) {
                continue;
            }
            LiveBB.add(basicBlock);

            for (BasicBlock pre : basicBlock.getPredecessors()) {
                if (AI.definingBlocks.contains(pre)) {
                    continue;
                }
                copyUsing.add(pre);
            }
        }
    }

    public boolean promoteAlloca(AllocaInst AI, DominatorTree DT) {
        ArrayList<Pair<Integer, StoreInst>> storeIndex = new ArrayList<>();

        for (Use use : AI.getUseList()) {
            User U = use.getU();
            if (U instanceof StoreInst) {
                StoreInst SI = (StoreInst) U;
                storeIndex.add(new Pair<>(SI.getInstNode().index(), SI));
            }
        }
        storeIndex.sort(Comparator.comparingInt(o -> o.a));

        ArrayList<Use> copyUseList = new ArrayList<>(AI.getUseList());
        Stack<Instruction> delete = new Stack<>();
        for (Use use : copyUseList) {
            User U = use.getU();
            if (!(U instanceof LoadInst)) {
                continue;
            }
            LoadInst LI = (LoadInst) U;
            int Idx = LI.getInstNode().index();
            // 二分查找最接近Idx且在load之前的storeInst
            int l = 0, r = storeIndex.size() - 1, mid;
            while (l <= r) {
                mid = (l + r) / 2;
                if (storeIndex.get(mid).a <= Idx) {
                    l = mid + 1;
                } else {
                    r = mid - 1;
                }
            }
            if (r == -1) {
                if (storeIndex.isEmpty()) {// 不存在store，load得到undef值
                    LI.replaceAllUsesWith(Constants.UndefValue.get(LI.getType()));
                } else {// load前面没有store但是后面有
                    return false;
                }
            } else {
                Value val = storeIndex.get(r).b.getOperand(0);
                if (LI.equals(val)) {
                    val = Constants.UndefValue.get(LI.getType());
                }
                LI.replaceAllUsesWith(val);
            }
            delete.push(LI);
        }
        while (!delete.isEmpty()) {
            delete.pop().remove();
        }
        // 移除storeInst
        while (!AI.getUseList().isEmpty()) {
            StoreInst SI = (StoreInst) AI.userBack();
            SI.remove();
        }
        AI.remove();
        return true;
    }

    public boolean rewriteAlloca(AllocaInst AI, DominatorTree DT) {
        StoreInst SI = AI.onlyStore;
        BasicBlock SB = SI.getParent();
        int storeIdx = -1;

        AI.usingBlocks.clear();
        ArrayList<Use> copyUseList = new ArrayList<>(AI.getUseList());
        for (Use use : copyUseList) {
            User U = use.getU();
            Instruction I = (Instruction) U;
            if (I.equals(SI)) {
                continue;
            }
            LoadInst LI = (LoadInst) I;
            // store的是instruction，需要判断支配关系；若不是instruction，则不用检查
            if (SI.getOperand(0) instanceof Instruction) {
                // 如果load和store在同一基本块，则需要比较二者先后顺序
                if (LI.getParent().equals(SB)) {
                    if (storeIdx == -1) {
                        storeIdx = SB.getInstList().indexOf(SI);
                    }
                    // store在load之后，load不受store支配
                    if (storeIdx > SB.getInstList().indexOf(LI)) {
                        AI.usingBlocks.add(SB);
                        continue;
                    }
                }
                // TODO：在不同基本块中，需要检查store的基本块是否支配load基本块
                else if (!DT.dominates(SB, LI.getParent())) {
                    AI.usingBlocks.add(LI.getParent());
                    continue;
                }
            }
            Value val = SI.getOperand(0);
            // TODO:未定义情况（SI.getOperand(0)==LI）
            if (LI.equals(val)) {
                val = Constants.UndefValue.get(LI.getType());
            }
            LI.replaceAllUsesWith(val);
            LI.remove();
        }
        if (!AI.usingBlocks.isEmpty()) {
            return false;
        }
        AI.onlyStore.remove();
        AI.remove();
        return true;
    }

    /**
     * 对于步骤2中选出来的可promotable的那些局部变量，扫描一次当前函数，
     * 看下哪些基本块在使用这些ALLOC，哪些基本块定义了这个ALLOC
     * （这里的使用是指LOAD，定义是通过STORE语句）
     */
    public void analyzeAlloca(AllocaInst AI) {
        AI.resetAnalyzeInfo();
        for (Use use : AI.getUseList()) {
            Instruction I = (Instruction) use.getU();
            if (I instanceof StoreInst) {
                StoreInst SI = (StoreInst) I;
                AI.definingBlocks.add(SI.getParent());
                AI.onlyStore = SI;
            } else {
                LoadInst LI = (LoadInst) I;
                AI.usingBlocks.add(LI.getParent());
            }
            if (AI.onlyUsedInOne) {
                if (AI.onlyBlock == null) {
                    AI.onlyBlock = I.getParent();
                } else if (AI.onlyBlock != I.getParent()) {
                    AI.onlyUsedInOne = false;
                }
            }
        }
    }

    /**
     * 如果满足下面的2个条件，就是可promotable：
     * 1.这个ALLOC出来的临时变量不在任何volatile指定中使用
     * 2.这个变量是直接通过LOAD或者STORE进行访问的，比如，不会被取址。
     */
    public static boolean isAllocaPromotable(AllocaInst AI) {
        Type Aty = AI.getAllocatedType();
        // 不处理数组
        if (!(Aty.isIntegerTy() || Aty.isFloatTy())) {
            return false;
        }
        for (Use use : AI.getUseList()) {
            User U = use.getU();
            if (U instanceof LoadInst) {

            } else if (U instanceof StoreInst) {
                StoreInst SI = (StoreInst) U;
                if (SI.getOperand(0).equals(AI)) {
                    return false;
                }
            }
            // else if(U instanceof GetElementPtrInst){
            // GetElementPtrInst GEP=(GetElementPtrInst)U;
            // if(!GEP.allIndicesZero()){
            // return false;
            // }
            // }
            else {
                return false;
            }
        }
        return true;
    }
}

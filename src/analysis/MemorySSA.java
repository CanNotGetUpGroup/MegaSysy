package analysis;

import ir.*;
import analysis.MemoryAccess.*;
import ir.instructions.Instructions;
import pass.passes.Mem2Reg;
import pass.test.testPass;

import java.io.IOException;
import java.util.*;

/**
 * 粒度不够细，没有精确到具体的array（如对b[i]赋值后，导致无法识别a[i]的版本）
 * TODO:1.精确到数组2.精确到数组元素
 */
public class MemorySSA {
    private final HashMap<Value, MemoryAccess> ValueToMemAcc;
    private final HashMap<Value,HashMap<BasicBlock,MemoryAccess>> PointerToPhi;
    public final HashMap<BasicBlock, LinkedList<MemoryAccess>> BlockToMemAccList;//基本块中储存的MemoryAccess
    public final HashMap<BasicBlock, LinkedList<MemoryAccess>> BlockToMemDefList;//基本块中储存的MemoryDef和MemoryPhi
    private final MemoryAccess LiveOnEntry;
    private int ID;
    private final Function F;
    private final DominatorTree DT;

    private final HashMap<Instructions.AllocaInst,Integer> AllocaLookup= new HashMap<>();
    private final HashMap<Instructions.PHIInst,Integer> PhiToAllocaMap=new HashMap<>();
    private ArrayList<Instructions.AllocaInst> allocaInsts;
    private final HashMap<BasicBlock,Integer> BBNumbers= new HashMap<>();
    private final HashMap<Instructions.CallInst,ArrayList<Value>> CI2Pointers=new HashMap<>();
    private final HashMap<Value,ArrayList<BasicBlock>> Pointer2Defs=new HashMap<>();
//    private final Set<BasicBlock> Visited=new HashSet<>();

    public MemorySSA(Function F, DominatorTree DT) {
        this.F = F;
        this.DT = DT;
        ValueToMemAcc = new HashMap<>();
        PointerToPhi = new HashMap<>();
        BlockToMemAccList = new HashMap<>();
        BlockToMemDefList = new HashMap<>();
        LiveOnEntry = new MemoryAccess(Instruction.Ops.MemDef, F.getEntryBB());
        ID=1;
        buildMemorySSA();
    }

    /**
     * 判断dominator是否支配dominated，若在不同基本块，则通过Dominator Tree判断，
     * 若在同一基本块，则调用localDominates()
     */
    public boolean dominates(MemoryAccess dominator, MemoryAccess dominated) {
        if (dominator == dominated) return true;
        if (isLiveOnEntry(dominated)) return false;
        if (dominator.getBB() != dominated.getBB()) {
            return DT.dominates(dominator.getBB(), dominated.getBB());
        }
        return localDominates(dominator, dominated);
    }

    //TODO:同一基本块支配判断
    public boolean localDominates(MemoryAccess dominator, MemoryAccess dominated) {
        if(dominator==dominated) return true;
        if(isLiveOnEntry(dominated)) return false;
        if(isLiveOnEntry(dominator)) return true;
        //否则比较二者顺序，在前面的支配后面的
        BasicBlock BB=dominator.getBB();
        LinkedList<MemoryAccess> defs=BlockToMemDefList.get(BB);
        return defs.indexOf(dominator)<defs.indexOf(dominated);
    }

    public void buildMemorySSA() {
        //先生成MemoryDef和MemoryUse，但不为他们指定definingAccess
        for (BasicBlock BB : F.getBbList()) {
            LinkedList<MemoryAccess> Accesses = null;
            LinkedList<MemoryAccess> Defs = null;
            for (Instruction I : BB.getInstList()) {
                MemoryDefOrUse MUD = createNewAccess(I);
                if (MUD == null)
                    continue;
                if (Accesses == null)
                    Accesses = getOrAddAccessList(BB);
                Accesses.add(MUD);
                if ((MUD instanceof MemoryDef)) {
                    if (Defs == null)
                        Defs = getOrAddDefList(BB);
                    Defs.add(MUD);
                }
            }
        }
        for(GlobalVariable g:F.getParent().getGlobalVariables()){
            ArrayList<BasicBlock> PHIBasicBlocks = new ArrayList<>();
            placePHINodes(DT, g, PHIBasicBlocks);
        }
        for(Instruction I:F.getEntryBB().getInstList()){
            if(!(I instanceof Instructions.AllocaInst)) break;
            ArrayList<BasicBlock> PHIBasicBlocks = new ArrayList<>();
            placePHINodes(DT, I, PHIBasicBlocks);
        }

        Set<BasicBlock> Visited = new HashSet<>();
        HashMap<Value,MemoryAccess> IncomingValues=new HashMap<>();
        for(GlobalVariable g:F.getParent().getGlobalVariables()){
            IncomingValues.put(g,LiveOnEntry);
        }
        for(Instruction I:F.getEntryBB().getInstList()){
            if(!(I instanceof Instructions.AllocaInst)) break;
            IncomingValues.put(I,LiveOnEntry);
        }

//        Stack<RenamePassData> RenamePassWorkList=new Stack<>();
//        RenamePassWorkList.add(new RenamePassData(DT.Parent.getEntryBB(),null,IncomingValues));
//        do{
//            RenamePassData RPD=RenamePassWorkList.pop();
//            RenamePass(RPD.BB,RPD.Pred,RPD.Val,RenamePassWorkList);
//        }while(!RenamePassWorkList.isEmpty());

        RenamePass(DT, IncomingValues, Visited);

        //TODO：将无法到达的基本块设为LiveOnEntry
    }

    /**
     * 计算支配树边界，找到插入MemPhi指令的位置，参考mem2reg的IDFCalculate方法
     */
    public void placePHINodes(DominatorTree DT, Value pointer, ArrayList<BasicBlock> IDFBlocks) {
        ArrayList<BasicBlock> DefiningBlocks=Pointer2Defs.get(pointer);
        if(DefiningBlocks==null) return;
        Mem2Reg.IDFCalculate(DT, DefiningBlocks, null, IDFBlocks);

        if(BBNumbers.size()==0){
            int ID=0;
            for(BasicBlock basicBlock:DT.Parent.getBbList()){
                BBNumbers.put(basicBlock,ID++);
            }
        }
        //升序排列，便于处理
        IDFBlocks.sort(new Comparator<BasicBlock>() {
            @Override
            public int compare(BasicBlock o1, BasicBlock o2) {
                return BBNumbers.get(o1)-BBNumbers.get(o2);
            }
        });

        //插入phi
        for (BasicBlock BB : IDFBlocks) {
            MemoryPhi memPhi = new MemoryPhi(BB, ID++);
            memPhi.setPointer(pointer);
            getOrAddAccessList(BB).addFirst(memPhi);
            getOrAddDefList(BB).addFirst(memPhi);
//            ValueToMemAcc.put(BB, memPhi);
            HashMap<BasicBlock,MemoryAccess> bbPhis = PointerToPhi.getOrDefault(pointer,new HashMap<>());
            bbPhis.put(BB,memPhi);
            PointerToPhi.put(pointer,bbPhis);
        }
    }

    static class RenamePassData {
        public BasicBlock BB;
        public BasicBlock Pred;
        HashMap<Value,MemoryAccess> Val;

        public RenamePassData(BasicBlock BB, BasicBlock pred, HashMap<Value,MemoryAccess> val) {
            this.BB = BB;
            Pred = pred;
            Val = val;
        }
    }

    /**
     * 添加MemPHI的IncomingVal，参考mem2reg的RenamePass
     */
    public void RenamePass(DominatorTree DT, HashMap<Value,MemoryAccess> IncomingVal, Set<BasicBlock> Visited) {
        DominatorTree.TreeNode Root = DT.Root;
        boolean AlreadyVisited = !Visited.add(Root.BB);
        if (AlreadyVisited)
            return;
        renameBlock(Root.BB, IncomingVal);
        renameSuccessorPhis(Root.BB, IncomingVal);
        dfsRename(new RenamePassData(Root.BB,null, IncomingVal),Visited);
    }
//    public void RenamePass(BasicBlock BB,BasicBlock Pred,HashMap<Value,MemoryAccess> IncomingVals,Stack<RenamePassData> Worklist){
//        while(true){
//            /*
//             * 如果块中有 φ 指令，则遍历所有先前添加的 φ（注意程序中原来可能也有 φ，这里要和原来的 φ 区分开来）：
//             * 假设某个前驱到当前基本块有 NumEdges 条边，则为 φ 指令添加 NumEdges 个来源，值为 IncomingVals[L]，同时设置 IncomingVals[L] = Phi
//             */
//            if (BlockToMemDefList.get(BB)!=null && BlockToMemDefList.get(BB).getFirst() instanceof MemoryPhi) {
//                MemoryPhi APN = (MemoryPhi) BlockToMemDefList.get(BB).getFirst();
//                int NewPHINumOperands = APN.getNumOperands();
//                int NumEdges = 0;
//                for (BasicBlock suc :
//                        Pred.getSuccessors()) {
//                    if (suc == BB) {
//                        NumEdges++;
//                    }
//                }
//                assert NumEdges >= 1;
//                Iterator<MemoryAccess> PNI = BlockToMemDefList.get(BB).iterator();
//                Instruction I;
//                do {
//
//                    // 则为 φ 指令添加 NumEdges 个来源
//                    for (int i = 0; i != NumEdges; ++i)
//                        APN.addIncoming(IncomingVals.get(APN.getPointer()), Pred);
//
//                    // 设置 IncomingVals[L] = Phi
//                    IncomingVals.put(APN.getPointer(), APN);
//                    // 处理下一个phi
//                    I = PNI.next();
//                    if (!(I instanceof MemoryPhi)) {
//                        break;
//                    }
//                    APN = (MemoryPhi) (I);
//                } while (APN.getNumOperands() == NewPHINumOperands);
//
//            }
//
//            if (!Visited.add(BB)) {
//                return;
//            }
//            renameBlock(BB,IncomingVals);
//
//            if (BB.getSuccessorsNum() == 0)
//                return;
//
//            BasicBlock I = BB.getSuccessor(0);
//            Set<BasicBlock> VisitedSuccs = new HashSet<>();
//
//            VisitedSuccs.add(I);
//            Pred = BB;
//            BasicBlock oldBB=BB;
//            BB = I;
//
//            for (int i = 1; i < oldBB.getSuccessorsNum(); i++) {
//                I = oldBB.getSuccessor(i);
//                if (VisitedSuccs.add(I))
//                    Worklist.add(new RenamePassData(I, Pred, new HashMap<>(IncomingVals)));
//            }
//        }
//    }

    public void dfsRename(RenamePassData RPD, Set<BasicBlock> Visited) {
        BasicBlock Pred = RPD.BB;
        if (Pred.getSuccessorsNum()!=0) {
            for (BasicBlock BB : Pred.getSuccessors()) {
                HashMap<Value,MemoryAccess> IncomingVal = new HashMap<>(RPD.Val);
                boolean AlreadyVisited = !Visited.add(BB);
                if (AlreadyVisited) {
                    LinkedList<MemoryAccess> BlockDefs=BlockToMemDefList.get(BB);
                    if (BlockDefs != null) {
                        for(MemoryAccess MA:BlockDefs){
                            //处理callInst
                            if(MA instanceof MemoryDef){
                                if(((MemoryDef)MA).getMemoryInstruction() instanceof Instructions.CallInst){
                                    Instructions.CallInst CI=(Instructions.CallInst)((MemoryDef)MA).getMemoryInstruction();
                                    for(Value v:CI2Pointers.get(CI)){
                                        IncomingVal.put(v,MA);
                                    }
                                    continue;
                                }
                            }
                            IncomingVal.put(MA.getPointer(),MA);
                        }
                    }
                    continue;
                } else{
                    renameBlock(BB, IncomingVal);
                }
                renameSuccessorPhis(BB, IncomingVal);
                dfsRename(new RenamePassData(BB,Pred,IncomingVal),Visited);
            }
        }
    }

    private void renameBlock(BasicBlock BB, HashMap<Value,MemoryAccess> IncomingVal) {
        LinkedList<MemoryAccess> accList = BlockToMemAccList.get(BB);
        if (accList != null && !accList.isEmpty()) {
            for (MemoryAccess MA : accList) {
                if (MA instanceof MemoryDefOrUse) {
                    MemoryDefOrUse MUD = (MemoryDefOrUse) MA;
                    //call单独处理
                    if(MUD.getMemoryInstruction() instanceof Instructions.CallInst) {
                        Instructions.CallInst CI=(Instructions.CallInst)MUD.getMemoryInstruction();
                        MemoryAccess newDef=LiveOnEntry;
                        for(Value v:CI2Pointers.get(CI)){
                            //找到call定义的pointer中版本最后的
                            if(IncomingVal.get(v).getID()>newDef.getID()) newDef=IncomingVal.get(v);
                            IncomingVal.put(v,MA);
                        }
                        if(MUD.getDefiningAccess()==null){
                            MUD.setDefiningAccess(newDef);
                        }
                        continue;
                    }
                    if (MUD.getDefiningAccess() == null) {
                        MUD.setDefiningAccess(IncomingVal.get(MUD.getPointer()));
                    }
                    if (MUD instanceof MemoryDef) {
                        IncomingVal.put(MUD.getPointer(),MA);
                    }
                } else {
                    IncomingVal.put(MA.getPointer(),MA);
                }
            }
        }
    }

    private void renameSuccessorPhis(BasicBlock BB, HashMap<Value,MemoryAccess> IncomingVal) {
        for (BasicBlock Succ : BB.getSuccessors()) {
            LinkedList<MemoryAccess> accList = BlockToMemAccList.get(Succ);
            if (accList == null || accList.isEmpty() || !(accList.getFirst() instanceof MemoryPhi))
                continue;
            for(MemoryAccess MA:accList){
                if(!(MA instanceof MemoryPhi)){
                    break;
                }
                MemoryPhi Phi = (MemoryPhi) MA;
                Phi.addIncoming(IncomingVal.get(Phi.getPointer()), BB);
            }
        }
    }

    public LinkedList<MemoryAccess> getOrAddAccessList(BasicBlock BB) {
        if (BlockToMemAccList.containsKey(BB)) return BlockToMemAccList.get(BB);
        LinkedList<MemoryAccess> memoryAccesses = new LinkedList<>();
        BlockToMemAccList.put(BB, memoryAccesses);
        return memoryAccesses;
    }

    public LinkedList<MemoryAccess> getOrAddDefList(BasicBlock BB) {
        if (BlockToMemDefList.containsKey(BB)) return BlockToMemDefList.get(BB);
        LinkedList<MemoryAccess> memoryDefs = new LinkedList<>();
        BlockToMemDefList.put(BB, memoryDefs);
        return memoryDefs;
    }

    /**
     * TODO:新建I的MemoryAccess，可能需要用到Alias Analysis中的信息判断是否改写、读取了内存？
     * 这里暂时只通过load是否为参数以及callee的side effect
     */
    public MemoryDefOrUse createNewAccess(Instruction I) {
        MemoryDefOrUse ret = null;
        switch (I.getOp()) {
            case Store, Call -> {
                if(I.getOp().equals(Instruction.Ops.Call)){
                    if((!((Function)I.getOperand(0)).hasSideEffect())){
                        return null;
                    }
                    Instructions.CallInst CI=(Instructions.CallInst)I;
                    CI2Pointers.put(CI,new ArrayList<>());
                    for(GlobalVariable g:F.getParent().getGlobalVariables()){
                        if(AliasAnalysis.callAlias(g,CI)){
                            CI2Pointers.get(CI).add(g);
                        }
                    }
                    for(Instruction inst:F.getEntryBB().getInstList()){
                        if(!(inst instanceof Instructions.AllocaInst)) break;
                        if(AliasAnalysis.callAlias(inst,CI)){
                            CI2Pointers.get(CI).add(inst);
                        }
                    }
                    if(CI2Pointers.get(CI).isEmpty()){
                        return null;
                    }
                    ret = new MemoryDef(I, null, ID++);
                    for(Value v:CI2Pointers.get(CI)){
                        ArrayList<BasicBlock> defs=Pointer2Defs.getOrDefault(v,new ArrayList<>());
                        defs.add(I.getParent());
                        Pointer2Defs.put(v,defs);
                    }
                    break;
                }
                if(I.getOp().equals(Instruction.Ops.Store)){
                    if(AliasAnalysis.isParam(I.getOperand(1))){
                        return null;
                    }
                    ret = new MemoryDef(I, null, ID++);
                    Value ptr=AliasAnalysis.getPointerOrArgumentValue(I.getOperand(1));
                    ArrayList<BasicBlock> defs=Pointer2Defs.getOrDefault(ptr,new ArrayList<>());
                    defs.add(I.getParent());
                    Pointer2Defs.put(ptr,defs);
                    ret.setPointer(ptr);
                }
            }
            case Load -> {
                if(AliasAnalysis.isParam(I.getOperand(0))){
                    return null;
                }
                ret = new MemoryUse(I, null);
                Value ptr=AliasAnalysis.getPointerOrArgumentValue(I.getOperand(0));
                ret.setPointer(ptr);
            }
        }
        if (ret != null) {
            ValueToMemAcc.put(I, ret);
        }
        return ret;
    }

    public boolean isLiveOnEntry(MemoryAccess MA) {
        return MA == LiveOnEntry;
    }

    /**
     * 获得某指令对应的MemoryDef或MemoryUse
     */
    public MemoryDefOrUse getMemoryAccess(Instruction I) {
        return (MemoryDefOrUse) ValueToMemAcc.get(I);
    }

    /**
     * 获得某基本块开头的MemoryPhi
     */
    public MemoryPhi getMemoryAccess(BasicBlock BB,Value pointer) {
        if(PointerToPhi.get(pointer)==null) return null;
        return (MemoryPhi) PointerToPhi.get(pointer).get(BB);
    }

    public DominatorTree getDomTree() {
        return DT;
    }

    public Function getF() {
        return F;
    }

    public MemoryAccess getLiveOnEntry() {
        return LiveOnEntry;
    }

    /**
     * 输出MemorySSA
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(F).append("{\n");
        boolean init = true;
        for (BasicBlock BB : F.getBbList()) {
            if (!init) {
                sb.append("\n").append(BB).append("     ").append(BB.getComment() != null ? BB.getComment() : "").append("\n");
            } else {
                sb.append(BB).append("     ").append(BB.getComment() != null ? BB.getComment() : "").append("\n");
                init = false;
            }
            for(MemoryAccess m:BlockToMemDefList.getOrDefault(BB,new LinkedList<>())){
                if(!(m instanceof MemoryPhi)) break;
                sb.append("  ").append(m).append("\n");
            }
            for (Instruction I : BB.getInstList()) {
                if (getMemoryAccess(I) != null) {
                    sb.append("  ").append(getMemoryAccess(I)).append("\n");
                }
                sb.append("  ").append(I).append("     ").append(I.getComment() != null ? I.getComment() : "").append("\n");
            }
        }
        sb.append("}").append("\n");
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        testPass.initModule();
        ir.Module module=ir.Module.getInstance();
//        FileWriter fw=new FileWriter("src/pass/test/output.txt");
//        PrintWriter pw=new PrintWriter(fw);

//        PassManager.initialization();
//        PassManager.run(module);
//        pw.println(module.toLL());
//        pw.flush();
        for(Function F:module.getFuncList()){
            if(!F.isDefined()) continue;
            MemorySSA MSSA=new MemorySSA(F,F.getDominatorTree());
            System.out.println(MSSA);
        }
    }
}

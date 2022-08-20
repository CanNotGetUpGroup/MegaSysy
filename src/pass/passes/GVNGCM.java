package pass.passes;

import analysis.*;
import ir.*;
import ir.Module;
import ir.Instruction.Ops;
import ir.instructions.BinaryInstruction;
import ir.instructions.CmpInst;
import org.antlr.v4.runtime.misc.Pair;
import pass.ModulePass;
import ir.instructions.Instructions.*;

import java.util.*;

import pass.PassManager;
import util.Folder;
import util.IListIterator;

public class GVNGCM extends ModulePass {
    private boolean aggressive=false;//开启将alloca替换为参数的优化(需要在函数内联之后)
    public static boolean GCMOpen=true;

    private Set<Instruction> visInsts = new HashSet<>();
    private ArrayList<Instruction> deadInst = new ArrayList<>();
    //注意维护hashToValue和integerToValue的唯一性，对应的都是leader value
    private static final HashMap<Value, Integer> valueToInteger = new HashMap<>();
    private static final HashMap<Integer, Value> integerToValue = new HashMap<>();
    private static int nextValueNumber = 0;
    private static final HashMap<Integer, ArrayList<Value>> hashToValue = new HashMap<>();
    private static final HashMap<Value, Pair<Integer, ArrayList<Integer>>> valueToHash = new HashMap<>();
    private DominatorTree DT;
    private static final ArrayList<Integer> null_array=new ArrayList<>();
//    private final int[] prime=new int[]{61,43,17,13};

    public GVNGCM(boolean aggressive) {
        super();
        this.aggressive=aggressive;
    }

    @Override
    public void runOnModule(Module M) {
        for (Function F : M.getFuncList()) {
            // 非Builtin函数
            if (F.isDefined()) {
                functionGVNGCM(F);
            }
        }
    }

    public void clear() {
        valueToInteger.clear();
        integerToValue.clear();
        hashToValue.clear();
        valueToHash.clear();
        nextValueNumber = 0;
    }

    public void functionGVNGCM(Function F) {
        boolean shouldContinue = true;
        while (shouldContinue) {
            clear();
            MemorySSA.arraySSA=!aggressive;
//            MemorySSA.arraySSA=false;//TODO:暂时关闭了ArraySSA
            AliasAnalysis.runMemorySSA(F);
            shouldContinue = functionGVN(F);
            new DeadCodeEmit().functionDCE(F);
            if(GCMOpen)
                functionGCM(F);
            shouldContinue |= new SimplifyCFG(aggressive).run(F);
        }
        Module.getInstance().rename(F);
    }

    public boolean functionGVN(Function F) {
        boolean ret = false;

        //直接利用DT中的逆后序遍历信息
        DT = F.getAndUpdateDominatorTree();
        for(var aug:F.getArguments()){
            int num = lookUpOrAdd(aug);
            addToLeader(num, aug);
        }
        for (var node : DT.getReversePostOrder()) {
            ret |= basicBlockGVN(node.BB);
        }
        return ret;
    }

    public boolean basicBlockGVN(BasicBlock BB) {
        boolean ret = combinePhi(BB);
        IListIterator<Instruction, BasicBlock> It = (IListIterator<Instruction, BasicBlock>) BB.getInstList().iterator(), It_pre = (IListIterator<Instruction, BasicBlock>) BB.getInstList().iterator();
        Instruction I = It.next();
        while (It.hasNext()) {
            ret |= instructionGVN(I);
            if (deadInst.isEmpty()) {
                I = It.next();
                continue;
            }
            if (I != BB.getInstList().getFirst().getVal()) {
                It.previous();
                It.previous();
            }
//            System.out.println("remove "+deadInst.size()+" instructions");
            for (Instruction J : deadInst) {
                J.remove();
            }
            deadInst.clear();
            if (I == BB.getInstList().getFirst().getVal())
                It = (IListIterator<Instruction, BasicBlock>) BB.getInstList().iterator();
            else I = It.next();
        }
        return ret;
    }

    public boolean combinePhi(BasicBlock BB) {
        boolean ret = false;
        IListIterator<Instruction, BasicBlock> It = (IListIterator<Instruction, BasicBlock>) BB.getInstList().iterator();
        Instruction I = It.next();
        while (I instanceof PHIInst) {
            PHIInst PI = (PHIInst) I;
            I = It.next();
            IListIterator<Instruction, BasicBlock> Jt = BB.getInstList().iterator(I);
            Instruction J = Jt.next();
            while (true) {
                if (!(J instanceof PHIInst)) {
                    break;
                }
                if (PI.isSameWith(J)) {
                    ret = true;
                    J.replaceAllUsesWith(PI);
                    J.remove();
                    It = (IListIterator<Instruction, BasicBlock>) BB.getInstList().iterator();
                    I = It.next();
                    break;
                }
                J = Jt.next();
            }
        }
        return ret;
    }

    public boolean instructionGVN(Instruction I) {
        boolean ret = false;
        Value V = Folder.simplifyInstruction(I);
        if (V != null) {
            if(I instanceof PHIInst && I.getType().isPointerTy()){
                for(Use use:I.getUseList()){
                    Instruction u=(Instruction)use.getU();
                    if(AliasAnalysis.MSSA.getMemoryAccess(u)!=null){
                        MemoryAccess.MemoryDefOrUse MOU=AliasAnalysis.MSSA.getMemoryAccess(u);
                        if(MOU.getPointer().equals(I)){
                            MOU.setPointer(V);
                        }
                    }
                }
            }
            I.replaceAllUsesWith(V);
            addInstToDeadList(I);
            return true;
        }
        if (I.getOp() == Ops.Load) {
            LoadInst LI = (LoadInst) I;
            if (loadGVN(LI)) {
                return true;
            }
        } else if(I.getOp().equals(Ops.Call)){
            if(!((CallInst)I).withoutGEP()){
                return false;
            }
        }
        if (I.getType().isVoidTy()) return false;
        int now = nextValueNumber;
        int num = lookUpOrAdd(I);
        if (I.getOp().equals(Ops.Alloca) || I.isTerminator() || I.getOp().equals(Ops.PHI) || num > now) {
            addToLeader(num, I);
            return false;
        }
        Value replace = findLeader(num);
        if (replace == null) {
            addToLeader(num, I);
            return false;
        } else if (replace == I) {
            return false;
        }
        else if (!GCMOpen&&(replace instanceof Instruction)) {//TODO:等待GCM完成后删除
            Instruction RI = (Instruction) replace;
            if (!DT.dominates(RI.getParent(), I.getParent())) {
                return false;
            }
        }
        replace(I, replace);
        addInstToDeadList(I);

        return true;
    }

    /**
     * 直接读取常量数组或没有被store过的数组
     */
    public boolean loadGVN(LoadInst LI) {
        if (LI.getUseList().isEmpty()) {
            addInstToDeadList(LI);
            return true;
        }
        Value Address = LI.getOperand(0);
        //若不是数组，则得到null
        ArrayList<Value> arrayIdx = AliasAnalysis.getArrayInfo(Address);
        Value Pointer = arrayIdx.get(0);
        if (Pointer instanceof GlobalVariable) {
            PointerInfo PI = new PointerInfo(Pointer);
            PI.calculateInfo(Pointer);
            if (!PI.isStored()) {
                GetElementPtrInst gep = (GetElementPtrInst) Address;
                Constant C = gep.getConstantValue();
                if (C != null) {
                    replace(LI, C);
                    return true;
                }
            }
        }
        return false;
    }

    public int lookUpOrAdd(Value V) {
        if (valueToInteger.containsKey(V)) return valueToInteger.get(V);
        //只有Instruction需要通过编号判断是否等价
        if (!(V instanceof Instruction)) {
            hashToValue.put(nextValueNumber, new ArrayList<>(){{add(V);}});
            valueToHash.put(V, new Pair<>(nextValueNumber, null_array));
            valueToInteger.put(V, nextValueNumber);
            integerToValue.put(nextValueNumber,V);
            return nextValueNumber++;
        }
        Instruction I = (Instruction) V;
        //判断I的hash是否存在
        if (Instruction.isBinary(I.getOp()) || Instruction.isCmp(I.getOp()) ||
                I.getOp().equals(Ops.Load)
                ||(GCMOpen&&I.getOp().equals(Ops.GetElementPtr))
                || I.getOp().equals(Ops.Call)
        ) {
            Pair<Integer,ArrayList<Integer>> hash = getHash(I);
            if (hashToValue.containsKey(hash.a)) {
                //由于hash碰撞问题，hashToValue用一个列表储存
                for (Value leader : hashToValue.get(hash.a))
                    if (equal(I,hash.b,leader)) {
                        int leaderOrder = valueToInteger.get(leader);
                        valueToInteger.put(V, leaderOrder);
                        valueToHash.put(V, hash);
                        return leaderOrder;
                    }
                //发生碰撞
                hashToValue.get(hash.a).add(I);
                valueToHash.put(V, hash);
            } else {
                hashToValue.put(hash.a, new ArrayList<>(){{add(V);}});
                valueToHash.put(V, hash);
                valueToInteger.put(V, nextValueNumber);
                integerToValue.put(nextValueNumber,V);
                return nextValueNumber++;
            }
        }
        hashToValue.put(nextValueNumber, new ArrayList<>(){{add(V);}});
        valueToHash.put(V, new Pair<>(nextValueNumber, null_array));
        valueToInteger.put(V, nextValueNumber);
        addToLeader(nextValueNumber,V);
        return nextValueNumber++;
    }

    public boolean equal(Value v,ArrayList<Integer> array, Value leader) {
        if (v == leader) return true;
        ArrayList<Integer> leaderArr = valueToHash.get(leader).b;
        if (array.size() != leaderArr.size()) return false;
        for (int i = 0; i < array.size(); i++) {
            if (!Objects.equals(array.get(i), leaderArr.get(i))) {
                return false;
            }
        }
        return true;
    }

    public void replace(Instruction I, Value repl) {
        if (I == repl) return;
        I.replaceAllUsesWith(repl);
    }

    public void addToLeader(int N, Value V) {
        integerToValue.put(N, V);
    }

    public Value findLeader(int N) {
        return integerToValue.get(N);
    }

    public void addInstToDeadList(Instruction I) {
        deadInst.add(I);
        if (valueToInteger.containsKey(I)) {
            int num = valueToInteger.remove(I);
            if (integerToValue.get(num) == I) {
                integerToValue.remove(num);
                hashToValue.remove(valueToHash.get(I).a);
            }
        }
    }

    public Pair<Integer,ArrayList<Integer>> getHash(Value V) {
        //非指令Value可直接获取Hash
        if (!(V instanceof Instruction)) {
            return new Pair<>(lookUpOrAdd(V),null_array);
        }
        if (valueToHash.containsKey(V)) return valueToHash.get(V);
        Instruction I = (Instruction) V;
        if (Instruction.isBinary(I.getOp())) {
            return getHash((BinaryInstruction) I);
        } else if (Instruction.isCmp(I.getOp())) {
            return getHash((CmpInst) I);
        } else if (I.getOp().equals(Ops.GetElementPtr)) {
            return getHash((GetElementPtrInst) I);
        } else if (I.getOp().equals(Ops.Load)) {
            return getHash((LoadInst) I);
        } else if (I.getOp().equals(Ops.Call)) {
            return getHash((CallInst) I);
        }
        //其余情况不计算hash
        return new Pair<>(lookUpOrAdd(I),null_array);
    }

    public Pair<Integer,ArrayList<Integer>> getHash(BinaryInstruction BI) {
        Value L = BI.getOperand(0), R = BI.getOperand(1);
        int lhs = getHash(L).a, rhs = getHash(R).a;
        ArrayList<Integer> array = new ArrayList<>();
        if (Instruction.isCommutative(BI.getOp()) && lhs > rhs) {
            array.add(rhs);
            array.add(lhs);
        } else {
            array.add(lhs);
            array.add(rhs);
        }
        return new Pair<>(hashCombine(BI.getOp(), BI.getType(), array),array);
    }

    public Pair<Integer,ArrayList<Integer>> getHash(CmpInst Cmp) {
        Value L = Cmp.getOperand(0), R = Cmp.getOperand(1);
        int lhs = getHash(L).a, rhs = getHash(R).a;
        ArrayList<Integer> array = new ArrayList<>();
        if (lhs > rhs) {
            array.add(CmpInst.getSwappedPre(Cmp.getPredicate()).ordinal());
            array.add(rhs);
            array.add(lhs);
        } else {
            array.add(Cmp.getPredicate().ordinal());
            array.add(lhs);
            array.add(rhs);
        }
        return new Pair<>(hashCombine(Cmp.getOp(), Cmp.getType(), array),array);
    }

    public Pair<Integer,ArrayList<Integer>> getHash(GetElementPtrInst GEP) {
        ArrayList<Integer> array = new ArrayList<>();
        ArrayList<Value> arrayIdx = GEP.getArrayIdx();
        //需要区分argument还是它的alloca
        if(aggressive){
            if(arrayIdx.get(1).equals(Constants.ConstantInt.get(1))&&
                    arrayIdx.get(2).equals(Constants.ConstantInt.get(0))&&
                    AliasAnalysis.isParamOrArgument(arrayIdx.get(0)))
                return getHash(arrayIdx.get(0));
        }
        for (Value v : arrayIdx) {
            array.add(getHash(v).a);
        }
//        for(Value v:GEP.getOperandList()){
//            array.add(getHash(v).a);
//        }
        return new Pair<>(hashCombine(GEP.getOp(), GEP.getType(), array),array);
    }

    public Pair<Integer,ArrayList<Integer>> getHash(LoadInst LI) {
        Value Address = LI.getOperand(0);
        if(aggressive&&AliasAnalysis.isParam(Address)){
            return getHash(AliasAnalysis.getParam(Address));
        }
        MemoryAccess MA = AliasAnalysis.MSSA.getMemoryAccess(LI);
        ArrayList<Integer> array = new ArrayList<>();
        Pair<Integer,ArrayList<Integer>> loadHash=getHash(Address);
        array.add(loadHash.a);
        if (MA != null){
            MemoryAccess definingAccess=((MemoryAccess.MemoryUse) MA).getDefiningAccess();
            array.add(definingAccess.getID());
            if(definingAccess instanceof MemoryAccess.MemoryDef&&
                    ((MemoryAccess.MemoryDef) definingAccess).getMemoryInstruction() instanceof StoreInst){
                //ArraySSA不能保证store的一定是load的位置
                StoreInst SI=(StoreInst)((MemoryAccess.MemoryDef)definingAccess).getMemoryInstruction();
                Pair<Integer,ArrayList<Integer>> storeHash=getHash(SI.getOperand(1));
                if(loadHash.a.equals(storeHash.a)){
                    boolean same=(loadHash.b.size()==storeHash.b.size());
                    for(int i=0;i<loadHash.b.size();i++){
                        if(!loadHash.b.get(i).equals(storeHash.b.get(i))){
                            same=false;
                            break;
                        }
                    }
                    if(same){
                        if(PassManager.debug){
//                            System.out.println(LI+" is replaced with "+SI.getOperand(0));
                        }
                        return getHash(SI.getOperand(0));
                    }
                }
            }
        }
        return new Pair<>(hashCombine(LI.getOp(), LI.getType(), array),array);
    }

    public Pair<Integer,ArrayList<Integer>> getHash(CallInst CI) {
        if(!CI.withoutGEP()){
            return new Pair<>(CI.hashCode(),null_array);
        }
        ArrayList<Integer> array = new ArrayList<>();
        array.add(getHash(CI.getOperand(0)).a);
        for (int i = 1; i < CI.getNumOperands(); i++) {
            array.add(getHash(CI.getOperand(i)).a);
        }
        MemoryAccess MA = AliasAnalysis.MSSA.getMemoryAccess(CI);
        if (MA != null)
            array.add(MA.getID());
        return new Pair<>(hashCombine(CI.getOp(), CI.getType(), array),array);
    }

    /**
     * 最简单的乘质数相加
     */
    public int hashCombine(Ops op, Type ty, ArrayList<Integer> operands) {
        int prime=31;
        int result = 1;
        result = prime * result + op.ordinal();
        result = prime * result + ty.getHashcode();
        for(Integer v:operands){
            result = prime * result + v;
        }
        return result;
//        return Objects.hash(op, ty, operands);
    }

    public void functionGCM(Function F) {

        // TODO: CALL without GEP
        F.getLoopInfo().computeLoopInfo(F);
        ArrayList<Instruction> insts = new ArrayList<>();
        for (BasicBlock BB : F.getBbList()) {
            for (Instruction I : BB.getInstList()) {
                insts.add(I);
            }
        }
        visInsts.clear();
        for (Instruction I : insts) {
            scheduleEarly(I, F);
        }
        visInsts.clear();
        for (Instruction I : insts) {
            scheduleLate(I, F);
        }
    }

    public void scheduleEarly(Instruction I, Function F) {
        DominatorTree DT = F.getDominatorTree();
        if (!visInsts.contains(I)) {
            visInsts.add(I);

            if (scheduleAble(I)) {
                I.getInstNode().remove();
                F.getEntryBB().getInstList().insertBeforeEnd(I.getInstNode());
            }
            if (Instruction.isBinary(I.getOp()) || Instruction.isCmp(I.getOp())
                    || I.getOp().equals(Ops.Load) || I.getOp().equals(Ops.GetElementPtr)) {
                for (Value op : I.getOperandList()) {
                    if (op instanceof Instruction) {
                        Instruction opInst = (Instruction) op;
                        scheduleEarly(opInst, F);
                        if (DT.getNode(opInst.getParent()).level > DT.getNode(I.getParent()).level) {
                            I.getInstNode().remove();
                            opInst.getParent().getInstList().insertBeforeEnd(I.getInstNode());
                        }
                    }
                }
                //MemoryUse和MemoryDef
                MemoryAccess MA=AliasAnalysis.MSSA.getMemoryAccess(I);
                if(MA!=null){
                    BasicBlock BB=MA.getParent();
                    if(MA instanceof MemoryAccess.MemoryUse){
                        MemoryAccess.MemoryUse MU=(MemoryAccess.MemoryUse)MA;
                        BB=MU.getDefiningAccess().getParent();
                    }
                    if(DT.getNode(BB).level>DT.getNode(I.getParent()).level){
                        I.getInstNode().remove();
                        BB.getInstList().insertBeforeEnd(I.getInstNode());
                    }
                }
            }

            if (I.getOp().equals(Ops.Call) && ((CallInst) I).withoutGEP()) {
                // skip op0
                for (var i = 1; i < I.getNumOperands(); i++) {
                    Value op = I.getOperand(i);
                    if (op instanceof Instruction) {
                        Instruction opInst = (Instruction) op;
                        scheduleEarly(opInst, F);
                        if (DT.getNode(opInst.getParent()).level > DT.getNode(I.getParent()).level) {
                            I.getInstNode().remove();
                            opInst.getParent().getInstList().insertBeforeEnd(I.getInstNode());
                        }
                    }
                }
            }
        }
    }

    public void scheduleLate(Instruction I, Function F) {
        DominatorTree DT = F.getDominatorTree();

        if (scheduleAble(I) && !visInsts.contains(I)) {
            visInsts.add(I);

            BasicBlock curBB = null;
            for (Use use : I.getUseList()) {
                User user = use.getU();
                if (user instanceof Instruction) {
                    Instruction userInst = (Instruction) user;
                    scheduleLate(userInst, F);
                    BasicBlock userBB = userInst.getParent();
                    if (userInst.getOp().equals(Ops.PHI)) {
                        int index = 0;
                        for (Value val : userInst.getOperandList()) { // PHI incoming vals
                            if (val instanceof Instruction && val.getUseList().contains(use)) {
//                                这里想要获取的是phi节点val对应的基本块？(index跟getPredecessor并不对应)
//                                userBB = userInst.getParent().getPredecessor(index);
                                userBB=((PHIInst)userInst).getIncomingBlock(index);
                                curBB = (curBB == null) ? userBB : DT.findSharedParent(DT.getNode(curBB), DT.getNode(userBB)).BB;
                            }
                            index++;
                        }
                    } else if(userInst.getOp().equals(Ops.MemPHI)){
                        int index = 0;
                        for (Value value : (userInst).getOperandList()) {
                            if (value.getUseList().contains(use)) {
                                userBB = ((MemoryAccess.MemoryPhi)userInst).getIncomingBlock(index);
                                curBB = (curBB == null) ? userBB : DT.findSharedParent(DT.getNode(curBB), DT.getNode(userBB)).BB;
                            }
                            index++;
                        }
                    } else {
                        curBB = (curBB == null) ? userBB : DT.findSharedParent(DT.getNode(curBB), DT.getNode(userBB)).BB;
                    }
                }
            }
            // upper:current lower:curBB
            BasicBlock minBB = curBB;
            int minLoopDepth = F.getLoopInfo().getLoopDepthForBB(minBB);
            while (curBB != I.getParent()) {
                if(DT.getNode(curBB)==null||curBB==DT.Root.BB){
//                    System.out.println("curBB shouldn't be null!");
                }
                curBB = DT.getNode(curBB).IDom.BB;
                int curLoopDepth = F.getLoopInfo().getLoopDepthForBB(curBB);
                if (curLoopDepth < minLoopDepth) {
                    minBB = curBB;
                    minLoopDepth = curLoopDepth;
                }
//                if(DT.getNode(curBB).equals(DT.Root)) break;
            }
            I.getInstNode().remove();
            minBB.getInstList().insertBeforeEnd(I.getInstNode());

            // 找当前BB最后位置(先找MemorySSA，然后选取二者中的前者)
            Instruction mayReturn=minBB.getInstList().getLast().getVal();
            for (Instruction inst : minBB.getInstList()) {
                if (!inst.getOp().equals(Ops.PHI)) {
                    if(AliasAnalysis.MSSA.getMemoryAccess(inst)!=null){
                        MemoryAccess.MemoryDefOrUse MA=AliasAnalysis.MSSA.getMemoryAccess(inst);
                        if(MA.getOperandList().contains(I)){
                            mayReturn=MA.getMemoryInstruction();
                            break;
                        }
                    }
                    if (inst.getOperandList().contains(I)) {
                        mayReturn=inst;
                        break;//此处应该break
                    }
                }
            }
            if(I!=mayReturn){
                I.getInstNode().remove();
                minBB.getInstList().insertBefore(I.getInstNode(),mayReturn.getInstNode());
            }
        }
    }

    public boolean scheduleAble(Instruction I) {
        return Instruction.isBinary(I.getOp()) || Instruction.isCmp(I.getOp()) ||
                I.getOp().equals(Ops.Load) || I.getOp().equals(Ops.GetElementPtr)
                || (I.getOp().equals(Ops.Call) && ((CallInst) I).withoutGEP());
    }

    @Override
    public String getName() {
        return "GVNGCM";
    }
}

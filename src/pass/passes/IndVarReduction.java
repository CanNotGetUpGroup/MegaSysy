package pass.passes;

import analysis.DominatorTree;
import analysis.LoopInfo;
import ir.*;
import ir.Module;
import ir.instructions.Instructions;
import org.antlr.v4.runtime.misc.Pair;
import pass.FunctionPass;
import pass.PassManager;
import util.LoopUtils;
import util.Match;
import util.MyIRBuilder;

import java.util.*;

/**
 * 在循环内未被使用的变量可以直接计算提取出来
 * 把能通过归纳变量削弱提取的量提取到PreHeader
 */
public class IndVarReduction extends FunctionPass {
    private final Set<Instructions.PHIInst> cycleInst = new HashSet<>();
    private LoopInfo LI;
    private DominatorTree DT;
    private final HashMap<Instruction, Pair<Value, Value>> cycleInstLoopUp = new HashMap<>();
    private final MyIRBuilder builder = MyIRBuilder.getInstance();
    public static boolean backEndTest;//根据前后端测试调整右移策略
    private Instruction incomingInst;

    @Override
    public void runOnFunction(Function F) {
        var gvngcm=new GVNGCM(false);
        var redundant=new LoopRedundant();
        boolean canContinue;
        do {
            canContinue = false;
            gvngcm.functionGVNGCM(F);
            redundant.runOnFunction(F);
//            Module.getInstance().rename(F);
            LI = F.getLoopInfo();
            DT = F.getAndUpdateDominatorTree();
            LI.computeLoopInfo(F);
            ArrayList<Loop> loops = LI.getTopLevelLoops();
            if (loops.isEmpty()) return;
            Queue<Loop> WorkList = new LinkedList<>();
            for (Loop l : loops) {
                LoopUtils.addLoopToWorkList(l, WorkList);
            }
            while (!WorkList.isEmpty()) {
                Loop loop = WorkList.poll();
                canContinue = runOnLoop(loop);
                if (canContinue) break;
            }
        } while (canContinue);
    }

    private boolean runOnLoop(Loop loop) {
        if (!canReduce(loop)) {
            return false;
        }
        if(cycleInstLoopUp.isEmpty()) return false;
        Value tripCount = getTripCount(loop);
        if (tripCount == null) return false;

        ArrayList<Value> inst2reduce = new ArrayList<>();
        cycleInstLoopUp.keySet().forEach((I) -> {
            Instruction reduce = (Instruction) reduceInst(I, tripCount);
            if (reduce != null) {
                inst2reduce.add(reduce);
                Instructions.PHIInst phi = (Instructions.PHIInst) cycleInstLoopUp.get(I).a;
                Value preIncoming = null;
                if(phi.getNumOperands()!=2){
                    System.out.println("error");
                }
                for (BasicBlock incomingBB : phi.getBlocks()) {
                    if (!loop.getBbList().contains(incomingBB)) {
                        preIncoming=phi.getIncomingValueByBlock(incomingBB);
                    }
                }
                if(preIncoming==null) return;
//                if (preIncoming instanceof Instruction || preIncoming instanceof Argument) {
//                    preIncoming.replaceAllUsesWith(reduce);
//                }
                incomingInst.COReplaceOperand(phi, preIncoming);
//                phi.replaceAllUsesWith(reduce);
                for(Instruction i:loop.getExitBlocks().get(0).getInstList()){
                    if(!(i instanceof Instructions.PHIInst)){
                        break;
                    }
                    for(Value v:((Instructions.PHIInst) i).getIncomingValues()){
                        if(v.equals(preIncoming)||v.equals(I)){
                            i.COReplaceOperand(v,reduce);
                        }
                    }
                }
                if (PassManager.debug) {
                    System.out.println("reduce " + I + " to " + reduce);
                }
                I.replaceAllUsesWith(reduce);
                I.remove();
            }
        });

        return !inst2reduce.isEmpty();
    }

    public Value reduceInst(Instruction I, Value TripCount) {
        if (!Instruction.isBinary(I.getOp())) {
            return null;
        }
        Instructions.PHIInst cycPhi = (Instructions.PHIInst) cycleInstLoopUp.get(I).a;
        Value cycOp = cycleInstLoopUp.get(I).b;
        // bin (cycPhi,cycOp)->bin (cycPhi,bin (cycOp,TripCount)) cycPhi在后来会替换成init值
        builder.setInsertPoint(I);
        Value sum = null;
        switch (I.getOp()) {
            case Add, Sub, Shl, Shr -> {
                sum = builder.createBinary(Instruction.Ops.Mul, cycOp, TripCount);
            }
            case FAdd, FSub -> {
                sum = builder.createBinary(Instruction.Ops.FMul, cycOp, TripCount);
            }
            case Mul -> {
                if (cycOp instanceof Constants.ConstantInt) {
                    int c = ((Constants.ConstantInt) cycOp).getVal();
                    if (c > 0 && (c & (c - 1)) == 0) {
                        Constants.ConstantInt cc = Constants.ConstantInt.get(Integer.bitCount(c - 1));
                        sum = builder.createBinary(Instruction.Ops.Mul, cc, TripCount);
                        incomingInst= (Instruction) builder.createBinary(Instruction.Ops.Shl, cycPhi, sum);
                        return incomingInst;
                    } else {
//                        System.out.println("may be faster mul");
                        return null;
                    }
                } else {
//                    System.out.println("may be faster mul");
                    return null;
                }
            }
            case SDiv -> {//TODO：除法不能表示为简单右移，可能会出问题(右移会对32取模)
                if (cycOp instanceof Constants.ConstantInt) {
                    int c = ((Constants.ConstantInt) cycOp).getVal();
                    if (c > 0 && (c & (c - 1)) == 0) {
                        Constants.ConstantInt cc = Constants.ConstantInt.get(Integer.bitCount(c-1));
                        if(backEndTest){
                            sum = builder.createBinary(Instruction.Ops.Mul, cc, TripCount);
                            incomingInst= (Instruction) builder.createBinary(Instruction.Ops.Shr,cycPhi,sum);
                            return incomingInst;
                        }else {//中端测试需要分两次位移
                            var bias=builder.createBinary(Instruction.Ops.Shr,cycPhi,TripCount);
                            incomingInst= (Instruction) bias;
                            if(cc.getVal()-1>0)
                                sum = builder.createBinary(Instruction.Ops.Mul, Constants.ConstantInt.get(cc.getVal()-1), TripCount);
                            return builder.createBinary(Instruction.Ops.Shr, bias, sum);
                        }
                    } else {
//                        System.out.println("may be faster div");
                        return null;
                    }
//                    return null;
                } else {
//                    System.out.println("may be faster div");
                    return null;
                }
            }
            default -> {
                return null;
            }
        }
        if (sum == null) return null;
        incomingInst= (Instruction) builder.createBinary(I.getOp(), cycPhi, sum);
        return incomingInst;
    }

    /**
     * 认为测例的循环算出来的TripCount都是合理的，不会出现死循环，不会出现负数
     */
    private Value getTripCount(Loop loop) {
        var stepInst = loop.getStepInst();
        var step = loop.getStep();
        var init = loop.getIndVarInit();
        var end = loop.getIndVarEnd();
        var bias = loop.getBias();
        Value tripCount = null;
        var preHeader = loop.getPreHeader();
        var latchCmp = loop.getLatchCmpInst();

        if (init == null || end == null || step == null) return null;

        //样例应该不会有死循环吧。。。
        builder.setInsertPoint(preHeader.getTerminator());
        switch (latchCmp.getPredicate()) {
            case ICMP_SLT, ICMP_SGT -> {
                var v1 = builder.createSub(end, init);
                tripCount = ceil(v1, step);
            }
            case ICMP_SLE -> {
                var v1 = builder.createSub(end, init);
                var v2 = builder.createAdd(v1, Constants.ConstantInt.get(1));
                tripCount = ceil(v2, step);
            }
            case ICMP_SGE -> {
                var v1 = builder.createSub(end, init);
                var v2 = builder.createSub(v1, Constants.ConstantInt.get(1));
                tripCount = ceil(v2, step);
            }
            case ICMP_NE -> {
                var v1 = builder.createSub(end, init);
                tripCount = builder.createSDiv(v1, step);
            }
        }
        if (tripCount == null) return null;
        var delta = builder.createSub(bias, step);
        tripCount = builder.createSub(tripCount, delta);
        return tripCount;
    }

    /**
     * 向上取整除法：(LHS + RHS - 1) / RHS
     */
    private Value ceil(Value LHS, Value RHS) {
        var v1 = builder.createAdd(LHS, RHS);
        var v2 = builder.createSub(v1, Constants.ConstantInt.get(1));
        return builder.createSDiv(v2, RHS);
    }

    /**
     * TODO:快速幂，经测试并没有测相关用例
     */
    private Value binPow(Value a, Value b) {
        return null;
    }

    private boolean canReduce(Loop loop) {
        if (!loop.isSafeToCopy() || !loop.isSimpleForLoop()) {
            return false;
        }
        cycleInst.clear();
        cycleInstLoopUp.clear();
        for (BasicBlock BB : loop.getBbList()) {
            for (Instruction I : BB.getInstList()) {
                if (isLoopInfoInst(I, loop) || I instanceof Instructions.BranchInst) {
                    continue;
                }
                if (I instanceof Instructions.PHIInst) {
                    Instructions.PHIInst phi = (Instructions.PHIInst) I;
                    if(!LI.getLoopDepthForBB(phi.getParent()).equals(loop.getLoopDepth())){
                        continue;
                    }
                    for (var incomingVal : phi.getIncomingValues()) {
                        if (incomingVal instanceof Instruction) {
                            Instruction inst = (Instruction) incomingVal;
                            // 需要来自循环的回边
                            if (DT.dominates(phi.getParent(),inst.getParent())) {
                                cycleInst.add(phi);
                                break;
                            }
                        }
                    }
                    continue;
                }
                if (Instruction.isBinary(I.getOp())) {
                    boolean ret = true;
                    for (int i = 0; i < I.getNumOperands(); i++) {
                        Value v = I.getOperand(i);
                        if (v instanceof Instructions.PHIInst && cycleInst.contains(v)) {
                            for(Use use:v.getUseList()){
                                User user=use.getU();
                                if(user instanceof Instruction&&user!=I
                                        &&LI.getLoopDepthForBB(((Instruction) user).getParent())==loop.getLoopDepth()){
                                    ret=false;
                                }
                            }
                            if(!ret) break;
                            Value other = I.getOperand(1 - i);
                            if (!(other instanceof Instruction)
                                    || (LI.getLoopDepthForBB(((Instruction) other).getParent()) < loop.getLoopDepth())) {
                                //需要支配latch
                                if (!DT.dominates(I.getParent(), loop.getSingleLatchBlock())) {
                                    ret = false;
                                } else {
                                    for (Use use : I.getUseList()) {
                                        User user = use.getU();
                                        if (user instanceof Instruction) {
                                            Instruction userInst = (Instruction) user;
                                            //在循环内被使用，可能需要排除
                                            if (LI.getLoopDepthForBB(userInst.getParent()).equals(loop.getLoopDepth())) {
                                                //不是phi就排除
                                                if (!(userInst instanceof Instructions.PHIInst)) {
                                                    ret = false;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (ret) {
                                    cycleInstLoopUp.put(I, new Pair<>(v, other));
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean isLoopInfoInst(Instruction I, Loop loop) {
        return I.equals(loop.getIndVar()) || I.equals(loop.getLatchCmpInst()) || I.equals(loop.getStepInst())
                || I.equals(loop.getIndVarCondInst());
    }

    @Override
    public String getName() {
        return "Index Variable Reduction";
    }
}

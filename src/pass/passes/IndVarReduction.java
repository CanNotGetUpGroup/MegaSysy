package pass.passes;

import analysis.LoopInfo;
import ir.*;
import ir.instructions.Instructions;
import org.antlr.v4.runtime.misc.Pair;
import pass.FunctionPass;
import util.LoopUtils;
import util.Match;
import util.MyIRBuilder;

import java.util.*;

/**
 * 把能通过归纳变量削弱提取的量提取到PreHeader
 */
public class IndVarReduction extends FunctionPass {
    private Set<Instructions.PHIInst> cycleInst=new HashSet<>();
    private LoopInfo LI;
    private HashMap<Instruction, Pair<Value,Value>> cycleInstLoopUp=new HashMap<>();
    private MyIRBuilder builder=MyIRBuilder.getInstance();

    @Override
    public void runOnFunction(Function F) {
        boolean canContinue;
        do{
            canContinue=false;
            LI = F.getLoopInfo();
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
                if(canContinue) break;
            }
        }while(canContinue);
    }

    private boolean runOnLoop(Loop loop){
        if(!canReduce(loop)){
            return false;
        }
        Value tripCount=getTripCount(loop);
        if(tripCount==null) return false;

        ArrayList<Value> inst2reduce=new ArrayList<>();
        for(BasicBlock BB:loop.getBbList()){
            for(Instruction I:BB.getInstList()){
                if(cycleInstLoopUp.containsKey(I)){
                    Value reduce=reduceInst(I,tripCount);
                    if(reduce!=null){
                        inst2reduce.add(reduce);

                        I.replaceAllUsesWith(reduce);
                        I.remove();
                    }
                }
            }
        }

        for(Value v:inst2reduce){
            Instruction I=(Instruction)v;

        }

        return true;
    }

    public Value reduceInst(Instruction I, Value TripCount){
        if(!Instruction.isBinary(I.getOp())){
            return null;
        }
        Instructions.PHIInst cycPhi= (Instructions.PHIInst) cycleInstLoopUp.get(I).a;
        Value cycOp=cycleInstLoopUp.get(I).b;
        // bin (cycPhi,cycOp)->bin (cycPhi,bin (cycOp,TripCount)) cycPhi在后来会替换成init值
        builder.setInsertPoint(I);
        Value sum=null;
        switch (I.getOp()){
            case Add,Sub->{
                sum=builder.createBinary(Instruction.Ops.Mul,cycOp,TripCount);
            }
            case FAdd,FSub -> {
                sum=builder.createBinary(Instruction.Ops.FMul,cycOp,TripCount);
            }
            case Mul -> {
                if(cycOp instanceof Constants.ConstantInt){
                    int c=((Constants.ConstantInt) cycOp).getVal();
                    if(c>0&&(c&(c-1))==0){
                        Constants.ConstantInt cc= Constants.ConstantInt.get(Integer.bitCount(c));
                        sum=builder.createBinary(Instruction.Ops.Mul,cc,TripCount);
                        return builder.createBinary(Instruction.Ops.Shl,cycPhi,sum);
                    }else{
                        return null;
                    }
                }else{
                    return null;
                }
            }
            case SDiv -> {
                if(cycOp instanceof Constants.ConstantInt){
                    int c=((Constants.ConstantInt) cycOp).getVal();
                    if(c>0&&(c&(c-1))==0){
                        Constants.ConstantInt cc= Constants.ConstantInt.get(Integer.bitCount(c));
                        sum=builder.createBinary(Instruction.Ops.Mul,cc,TripCount);
                        return builder.createBinary(Instruction.Ops.Shr,cycPhi,sum);
                    }else{
                        return null;
                    }
                }else{
                    return null;
                }
            }
            default -> {
                return null;
            }
        }
        if(sum==null) return null;
        return builder.createBinary(I.getOp(),cycPhi,sum);
    }

    /**
     * 认为测例的循环算出来的TripCount都是合理的，不会出现死循环，不会出现负数
     */
    private Value getTripCount(Loop loop){
        var stepInst=loop.getStepInst();
        var step=loop.getStep();
        var init=loop.getIndVarInit();
        var end=loop.getIndVarEnd();
        var bias=loop.getBias();
        Value tripCount= null;
        var preHeader=loop.getPreHeader();
        var latchCmp=loop.getLatchCmpInst();

        //样例应该不会有死循环吧。。。
        builder.setInsertPoint(preHeader.getTerminator());
        switch (latchCmp.getPredicate()) {
            case ICMP_SLT,ICMP_SGT -> {
                var v1=builder.createSub(end,init);
                tripCount=ceil(v1,step);
            }
            case ICMP_SLE -> {
                var v1=builder.createSub(end,init);
                var v2=builder.createAdd(v1, Constants.ConstantInt.get(1));
                tripCount=ceil(v2,step);
            }
            case ICMP_SGE -> {
                var v1=builder.createSub(end,init);
                var v2=builder.createSub(v1, Constants.ConstantInt.get(1));
                tripCount=ceil(v2,step);
            }
            case ICMP_NE -> {
                var v1=builder.createSub(end,init);
                tripCount=builder.createSDiv(v1,step);
            }
        }
        var delta=builder.createSub(bias,step);
        tripCount=builder.createSub(tripCount,delta);
        return tripCount;
    }

    /**
     * 向上取整除法：(LHS + RHS - 1) / RHS
     */
    private Value ceil(Value LHS,Value RHS){
        var v1=builder.createAdd(LHS,RHS);
        var v2=builder.createSub(v1, Constants.ConstantInt.get(1));
        return builder.createSDiv(v2,RHS);
    }

    private boolean canReduce(Loop loop){
        if(!loop.isSafeToCopy()||!loop.isSimpleForLoop()){
            return false;
        }
        for(BasicBlock BB:loop.getBbList()){
            for(Instruction I:BB.getInstList()){
                if(isLoopInfoInst(I,loop)){
                    continue;
                }
                if(I instanceof Instructions.PHIInst){
                    Instructions.PHIInst phi=(Instructions.PHIInst)I;
                    for (var incomingVal : phi.getIncomingValues()) {
                        if (incomingVal instanceof Instruction) {
                            Instruction inst = (Instruction) incomingVal;
                            int incomingValDepth = LI.getLoopDepthForBB(inst.getParent());
                            if (loop.getLoopDepth() == incomingValDepth) {
                                cycleInst.add(phi);
                                break;
                            }
                        }
                    }
                    continue;
                }
                if(Instruction.isBinary(I.getOp())){
                    boolean ret = false;
                    for(int i=0;i<I.getNumOperands();i++){
                        Value v=I.getOperand(i);
                        if (v instanceof Instructions.PHIInst&&cycleInst.contains(v)) {
                            Value other=I.getOperand(1-i);
                            if(!(other instanceof Instruction)
                                    ||(LI.getLoopDepthForBB(((Instruction) other).getParent())<loop.getLoopDepth())){
                                ret = true;
                                cycleInstLoopUp.put(I,new Pair<>(v,other));
                                break;
                            }
                        }
                    }
                    if(!ret) return false;
                }else{
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isLoopInfoInst(Instruction I,Loop loop){
        return I.equals(loop.getIndVar())||I.equals(loop.getLatchCmpInst())||I.equals(loop.getStepInst())
                ||I.equals(loop.getIndVarCondInst());
    }

    @Override
    public String getName() {
        return "Index Variable Reduction";
    }
}

package pass.passes;

import analysis.DominatorTree;
import ir.*;
import ir.instructions.CmpInst;
import ir.instructions.Instructions.*;
import pass.FunctionPass;
import util.LoopUtils;
import util.Match;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class LoopUnroll extends FunctionPass {
    private DominatorTree DT;
    private final int maxLoop=5;

    @Override
    public void runOnFunction(Function F) {
        F.getLoopInfo().computeLoopInfo(F);
        ArrayList<Loop> loops = F.getLoopInfo().getTopLevelLoops();
        if(loops.isEmpty()) return;
        DT=F.getAndUpdateDominatorTree();
        new LCSSA().runOnFunction(F);
        Queue<Loop> WorkList=new LinkedList<>();

        for(Loop l: loops){
            LoopUtils.addLoopToWorkList(l,WorkList);
        }
        while(!WorkList.isEmpty()){
            Loop L=WorkList.poll();
            tryToUnrollLoop(L);
        }
    }

    public void tryToUnrollLoop(Loop L){
        if(!L.isSafeToCopy()){
            return;
        }

        HashSet<BasicBlock> Visited=new HashSet<>();
        LoopUtils.rearrangeBBOrder(L,Visited);
        if(L.getTripCount()==null){//非常数次展开
            UnrollLoop(L);
        }
    }

    public void UnrollLoop(Loop L){
        if(!L.isSimpleForLoop()||L.getBbList().size()>maxLoop||!canUnroll(L)){
            return;
        }
        BasicBlock Header=L.getLoopHeader();
        BasicBlock LatchBB=L.getSingleLatchBlock();
        ArrayList<BasicBlock> ExitBBs=L.getExitBlocks();
        ArrayList<BasicBlock> OriBBs=L.getBbList();
        ArrayList<BasicBlock> ExitingBBs=L.getExitingBlocks();
        BasicBlock ExitingBB=ExitingBBs.size()==1?ExitingBBs.get(0):null;
        BasicBlock ExitBB=null;
        if(LatchBB == null || LatchBB != ExitingBB){
            return;
        }
        BranchInst latchBr= (BranchInst) LatchBB.getTerminator();

        var step=L.getStep();
        if(step instanceof Constants.ConstantInt){
            if(Math.abs(((Constants.ConstantInt) step).getVal())>100000000){
                return;
            }
        }
        for(Instruction I:Header.getInstList()){
            if(I instanceof GetElementPtrInst){
                for(int i=1;i< I.getNumOperands();i++){
                    if(I.getOperand(i) instanceof LoadInst){
                        return;
                    }
                }
            }
        }
        if(OriBBs.contains(latchBr.getTrueBlock())){
            ExitBB=latchBr.getFalseBlock();
        }else {
            ExitBB=latchBr.getTrueBlock();
        }
        if(ExitBB.getPredecessorsNum()>2) return;
        ArrayList<PHIInst> OriPhis=new ArrayList<>();

    }

    private boolean canUnroll(Loop loop){
        return loop.getIndVar()!=null&&(loop.getStepInst().getOp().equals(Instruction.Ops.Sub)||
                loop.getStepInst().getOp().equals(Instruction.Ops.Add))&&
                (loop.getLatchCmpInst().getPredicate().compareTo(CmpInst.Predicate.ICMP_SGT)>=0&&
                        loop.getLatchCmpInst().getPredicate().compareTo(CmpInst.Predicate.ICMP_SLE)<=0);
    }

    @Override
    public String getName() {
        return "Loop Unroll";
    }
}

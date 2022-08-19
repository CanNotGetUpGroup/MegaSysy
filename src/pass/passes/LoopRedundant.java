package pass.passes;

import analysis.LoopInfo;
import ir.*;
import ir.instructions.Instructions;
import pass.FunctionPass;
import pass.PassManager;
import util.LoopUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

/**
 * TODO:为IR添加快速幂
 */
public class LoopRedundant extends FunctionPass {
    private LoopInfo LI;

    @Override
    public void runOnFunction(Function F) {
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
            removeUselessLoop(loop);
        }
        new SimplifyCFG(false).runOnFunction(F);
    }

    public void removeUselessLoop(Loop loop) {
        // 循环结构限制：一个 preHeader，一个 latchBlock，一个 exit，可能有多个 exitingBlock
        if(!loop.isSimpleForLoop()) return;
        var preHeader = loop.getPreHeader();
        var latchBlock = loop.getSingleLatchBlock();

        Instructions.BranchInst latchBr = (Instructions.BranchInst) latchBlock.getTerminator();
        if (latchBr.getNumOperands() == 1) {
            return;
        }
        BasicBlock exit = null;
        if (loop.getBbList().contains(latchBr.getTrueBlock())) {
            exit = latchBr.getFalseBlock();
        } else {
            exit = latchBr.getTrueBlock();
        }
        if (exit == null) {
            return;
        }

        HashSet<Instruction> loopInsts = new HashSet<>();

        for (var bb : loop.getBbList()) {
            for (var inst : bb.getInstList()) {
                if (canNotRedundant(inst)) {
                    return;
                }
                loopInsts.add(inst);
            }
        }

        for (var inst : exit.getInstList()) {
            if (!(inst instanceof Instructions.PHIInst)) {
                break;
            }
            // 循环内指令不被出口处 LCSSA Phi 使用
            for (var op : inst.getOperandList()) {
                if (op instanceof Instruction&&loopInsts.contains(op)) {
                    return;
                }
            }
            // phi 指令中来自循环和 preHeader 的 incomingVals 相同
            for (var i : loop.getExitingBlocks()) {
                if (((Instructions.PHIInst) inst).getIncomingValueByBlock(i) != ((Instructions.PHIInst) inst).getIncomingValueByBlock(preHeader)) {
                    return;
                }
            }
        }

        // 消除无用循环
        Instructions.BranchInst preBrInst = (Instructions.BranchInst) preHeader.getTerminator();
        if (preBrInst.getNumOperands() == 1) {
            return;
        }

        // preHeader只跳转到Exit
        int headerOpIndex = preBrInst.getOperandList().indexOf(loop.getLoopHeader());
        preBrInst.removeOperand(headerOpIndex);
        preBrInst.removeOperand(0);

        for (var inst : exit.getInstList()) {
            if (!(inst instanceof Instructions.PHIInst)) {
                break;
            }
            for(BasicBlock bb:loop.getExitingBlocks()){
                ((Instructions.PHIInst) inst).removeIncomingValue(bb,true);
            }
        }

        for (var bb : new ArrayList<>(loop.getBbList())) {
            for (var inst : (bb.getInstList())) {
                if(inst.getInstNode().getPrev()!=null){
                    inst.getInstNode().getPrev().getVal().remove();
                }
            }
            bb.remove();
        }

        if(PassManager.debug){
            System.out.println("remove Loop: header "+loop.getLoopHeader()+" latch "+loop.getSingleLatchBlock());
        }

        LI.removeLoop(loop);
    }

    /**
     * 不能有 Store/Ret/side effect Call
     */
    public static boolean canNotRedundant(Instruction inst){
        return inst.getOp().equals(Instruction.Ops.Store) || inst.getOp().equals(Instruction.Ops.Ret)
                || (inst.getOp().equals(Instruction.Ops.Call) && ((Instructions.CallInst) inst).getCalledFunction().hasSideEffect());
    }

    @Override
    public String getName() {
        return "Loop Redundant";
    }
}

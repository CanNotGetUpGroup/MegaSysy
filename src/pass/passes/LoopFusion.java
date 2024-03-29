package pass.passes;

import java.util.HashMap;
import java.util.HashSet;

import analysis.DominatorTree;
import analysis.LoopInfo;
import ir.Function;
import ir.Instruction;
import ir.Loop;
import ir.Constants.ConstantInt;
import ir.instructions.Instructions.PHIInst;
import ir.instructions.Instructions.BranchInst;
import pass.FunctionPass;

/**
 * 嵌套循环的loopFusion只考虑最里层的了
 * 算法参见【LLVM Developers’ Meeting
 * 2018】https://www.youtube.com/watch?v=UVZPtBGV8kQ
 * chrome-extension://ikhdkkncnoglghljlkmcimlnlhkeamad/pdf-viewer/web/viewer.html?file=https%3A%2F%2Fllvm.org%2Fdevmtg%2F2018-10%2Fslides%2FBarton-LoopFusion.pdf#=&zoom=page-width
 * https://www.youtube.com/redirect?event=video_description&redir_token=QUFFLUhqbklIVnNhdlZSVGgxdGdNRmxhalQ2dHlFd3RSd3xBQ3Jtc0tubTNHemM5RmpRalgzWXh4MWd2MFhrUFpkYjZobzR0a19IalBoMVpaTkM5bi1jT3dXZVdBSlRINkZ3NGRjdDhPUWxzaEhCR3ZJUWdrcjJfMWVfX2FHemhXdFFDSXItZUVZa2Vsd201QThIVVduY2JpYw&q=https%3A%2F%2Fllvm.org%2Fdevmtg%2F2015-04%2Fslides%2FLLVMEuro2015LoopFusionAmidComplexControlFlow.pdf&v=UVZPtBGV8kQ
 */
public class LoopFusion extends FunctionPass {

    private DominatorTree domInfo;

    @Override
    public String getName() {
        return "Loop Fusion";
    }

    @Override
    public void runOnFunction(Function F) {
        LoopInfo loopInfo = F.getLoopInfo();
        domInfo = new DominatorTree(F);
        loopInfo.computeLoopInfo(F);
        HashMap<Loop, Loop> fusionPairs = new HashMap<>();
        HashSet<Loop> fusionSet = new HashSet<>();

        for (var predLoop : loopInfo.getAllLoops()) {
            // 只考虑最里层循环
            if (predLoop.getSubLoops().isEmpty() && !fusionSet.contains(predLoop)) {
                if (canbeFusion(predLoop)) {
                    for (var succLoop : loopInfo.getAllLoops()) {
                        if (succLoop.getSubLoops().isEmpty() && !fusionSet.contains(succLoop)) {
                            if (canbeFusion(succLoop)) {
                                if (canbeFusion(predLoop, succLoop)) {
                                    fusionSet.add(succLoop);
                                    fusionSet.add(predLoop);
                                    fusionPairs.put(predLoop, succLoop);
                                }
                            }
                        }
                    }
                }
            }
        }
        // 遍历hashMap fusionPairs
        fusionPairs.forEach((k, v) -> {
            donefusion(k, v);
            loopInfo.computeLoopInfo(F);
        });
    }

    /**
     * Loops are not candidates for fusion if:
     * – They might throw an exception
     * – They contain volatile memory accesses
     * – They are not in simplified form
     * – Any of the necessary information is not available (preheader, header,
     * latch, exiting blocks, exit block)
     * 
     * 
     */
    private boolean canbeFusion(Loop loop) {
        if (!loop.isSimpleForLoop()) { // 条件放宽一点
            return false;
        }
        if (loop.getIndVar() == null || loop.getIndVarInit() == null || loop.getIndVarEnd() == null
                || loop.getIndVarCondInst() == null || loop.getStep() == null) {
            return false;
        }
        return true;
    }

    private boolean canbeFusion(Loop pred, Loop succ) {
        // 必须处于同一父循环下，且不为同一循环
        if (pred.getParentLoop() != succ.getParentLoop() || pred == succ) {
            return false;
        }
        // 前循环的exitblock(0)必须是后循环的preheader[trivial]
        var predExitBlock = pred.getExitBlocks().get(0);
        var succPreHeader = succ.getPreHeader();
        if (predExitBlock != succPreHeader) {
            return false;
        }
        // 迭代遍历（初始、结束）必须相同
        if (pred.getIndVarInit() != succ.getIndVarInit()
                || pred.getIndVarEnd() != succ.getIndVarEnd()
                || pred.getLatchCmpInst().getOp() != succ.getLatchCmpInst().getOp()
                || !pred.getIndVarCondInst().getOperandList().contains(pred.getIndVar())
                || !succ.getIndVarCondInst().getOperandList().contains(succ.getIndVar())) {
            return false;
        }
        // 步长必须相同，常量则必须相等
        if (pred.getStep() instanceof ConstantInt && succ.getStep() instanceof ConstantInt
                && ((ConstantInt) pred.getStep()).getVal() != ((ConstantInt) succ.getStep())
                        .getVal()) {
            return false;
        }
        // 步长必须相同，非常量则必须一样
        if (!(pred.getStep() instanceof ConstantInt) && pred.getStep() != succ.getStep()) {
            return false;
        }
        var predPreBrInst = pred.getLoopPrehead().getTerminator();
        var succPreBrInst = succPreHeader.getTerminator();
        // 一个模式匹配
        if (predPreBrInst.getNumOperands() != 3 || succPreBrInst.getNumOperands() != 3) {
            return false;
        }
        // 判断preheader的跳转判断条件是否一致
        if (!(predPreBrInst.getOperandList().get(0) instanceof Instruction)
                || !(succPreBrInst.getOperandList().get(0) instanceof Instruction)) {
            return false;
        }
        Instruction predPreCmpInst = (Instruction) predPreBrInst.getOperandList().get(0);
        Instruction succPreCmpInst = (Instruction) succPreBrInst.getOperandList().get(0);
        if (predPreCmpInst.getOperandList().get(0) != succPreCmpInst.getOperandList().get(0)
                || predPreCmpInst.getOperandList().get(1) != succPreCmpInst.getOperandList().get(1)) {
            return false;
        }
        // // 必须满足CFE
        // if (!domInfo.dominates(pred.getLoopHeader(), succ.getLoopHeader())
        // || !postDomInfo.dominates(succ.getLoopPrehead(),pred.getLoopPrehead())) {
        // return false;
        // }
        // System.out.println(
        // "find may fusioning loops, PRED:" + pred.getLoopHeader().getName() + " SUCC:
        // " +
        // succ.getLoopHeader().getName());
        return true;
    }

    private void donefusion(Loop pred, Loop succ) {
        HashSet<Instruction> opIrrelevantInstSet = new HashSet<>();
        HashSet<Instruction> userIrrelevantInstSet = new HashSet<>();
        HashSet<Instruction> irrelevantInstSet = new HashSet<>();
        HashSet<Instruction> relevantInstSet = new HashSet<>();
        var predPreHeader = pred.getPreHeader();
        var predHeader = pred.getLoopHeader();
        var succHeader = succ.getLoopHeader();
        var commonBB = succ.getPreHeader();
        // 判定common的inst是否可以提取到predpreheader
        // 先看inst的opInst行不行，opinst的def必须在predHeader之前（在predPreHeader里面是ok的）
        // operand 所在基本块都支配 predLoop header，取闭包
        for (var inst : commonBB.getInstList()) {
            if (!(inst instanceof BranchInst)) {
                boolean isIrrelevant = true;
                for (var op : inst.getOperandList()) {
                    if (!(op instanceof Instruction)) {
                        continue;
                    }
                    Instruction opInst = (Instruction) op;
                    if (!opIrrelevantInstSet.contains(op) && !domInfo.dominates(opInst.getParent(), predHeader)) {
                        isIrrelevant = false;
                    }
                }
                if (isIrrelevant) {
                    opIrrelevantInstSet.add(inst);
                }
            }
        }
        // 再看看inst的 user 都被 predLoop header 支配，取闭包
        for (var instnode = commonBB.getTerminator().getInstNode(); instnode != null; instnode = instnode.getPrev()) {
            var inst = instnode.getVal();
            if (!(inst instanceof BranchInst)) {
                boolean isIrrelevant = true;
                for (var use : inst.getUseList()) {
                    if (!(use.getU() instanceof Instruction)) {
                        continue;
                    }
                    Instruction userInst = (Instruction) use.getU();
                    if (!userIrrelevantInstSet.contains(userInst)
                            && !domInfo.dominates(predPreHeader, userInst.getParent())) {
                        isIrrelevant = false;
                    }
                    if (isIrrelevant) {
                        userIrrelevantInstSet.add(inst);
                    }
                }
            }
        }
        irrelevantInstSet.addAll(opIrrelevantInstSet);
        irrelevantInstSet.retainAll(userIrrelevantInstSet);
        commonBB.getInstList().forEach(inst -> {
            if (!(irrelevantInstSet.contains(inst))) {
                relevantInstSet.add(inst);
            }
        });

        var brInst = relevantInstSet.iterator().next();
        if (!(brInst instanceof BranchInst)) {
            return;
        }

        // irrelevantInst 移动到 predPreHeader
        for (var inst : irrelevantInstSet) {
            inst.getInstNode().remove();
            predPreHeader.getInstList().insertBeforeEnd(inst.getInstNode());
        }

        // System.out.println("fusion");

        // 用前一个循环的indvar替换后一个循环的indvar
        succHeader.getInstList().forEach(inst -> {
            inst.COReplaceOperand(succ.getIndVar(), pred.getIndVar());
        });
        // succHeader 的 phi 移到 predHeader 前面
        for (var inst : succHeader.getInstList()) {
            if (!(inst instanceof PHIInst)) {
                break;
            }
            inst.getInstNode().remove();
            predHeader.getInstList().insertAtHead(inst.getInstNode());
        }
        // 维护新的block前驱后继关系
        // pred preheader的branch Inst 原本指向common block的分支改为指向succ的exit block
        var predPreheaderBrInst = predPreHeader.getTerminator();
        var succExitBlock = succ.getExitBlocks().get(0);
        predPreheaderBrInst.COReplaceOperand(commonBB, succExitBlock);
        // pred 的branch Inst直接指向 succ的循环体（也就是header）
        var predBrInst = predHeader.getTerminator();
        predBrInst.removeAllOperand();
        predBrInst.addOperand(succHeader);
        // succ 的branch Inst原本指向自己的改为指向pred header
        var succBrInst = succHeader.getTerminator();
        succBrInst.COReplaceOperand(succHeader, predHeader);
    }
}

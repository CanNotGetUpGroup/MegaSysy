package pass.passes;

import java.util.ArrayList;
import java.util.HashSet;
import util.CloneMap;

import analysis.LoopInfo;
import ir.Constant;
import ir.Function;
import ir.Instruction;
import ir.Loop;
import ir.Type;
import ir.Instruction.Ops;
import ir.instructions.BinaryInstruction;
import pass.FunctionPass;

/**
 * 
 * 循环不变量：S是一个语句，已知循环
 * while C
 * do E
 * 当此循环满足: 在任何循环开始前，语句S和C都为真，而且在循环结束后，S仍为真，那么S就是循环不变量。
 * 
 * Algorithm:
 * [concept]
 * https://blog.csdn.net/Truman_Chan/article/details/117702612
 * [travil solution]
 * https://zhuanlan.zhihu.com/p/366794421
 * [used algorithm]
 * https://www.slidestalk.com/u70/Lecture5LICMandStrengthReduction0208201837108
 */
public class LICM extends FunctionPass {

    @Override
    public String getName() {
        return "LICM";
    }

    @Override
    public void runOnFunction(Function func) {
        System.out.println("Running pass : LICM");
        LoopInfo loopInfo = func.getLoopInfo();
        for (var topLoop : loopInfo.getTopLevelLoops()) {
            runOnLoop(topLoop);
        }
    }

    public void runOnLoop(Loop loop) {
        for (var subLoop : loop.getSubLoops()) {
            if (subLoop != null) {
                runOnLoop(subLoop);
            }
        }
        ArrayList<Instruction> Invariant = detectInvarint(loop); // 循环不变量标记
        // 将标记的循环不变量按顺序提升到循环外部
        for (var inst : Invariant) {
            if (inst instanceof BinaryInstruction) {
                BinaryInstruction binInst = (BinaryInstruction) inst;
                CloneMap cloneMap = new CloneMap();
                var copyInst = binInst.copy(cloneMap);
                inst.remove();
                loop.getLoopPrehead().getInstList().insertBeforeEnd(copyInst.getInstNode());
            }
        }
    }

    public ArrayList<Instruction> detectInvarint(Loop loop) {
        ArrayList<Instruction> Invariant = new ArrayList<>();
        boolean newInvariantAdded2Set = false;
        do {
            for (var block : loop.getBbList()) {
                for (Instruction inst : block.getInstList()) { // 对每一个block的每一个instruction遍历
                    if (inst instanceof BinaryInstruction) {
                        boolean isInvariant = true;
                        for (var i = 1; i < inst.getOperandList().size(); i++) { // 对每一个instruction的每一个use operand遍历
                            var op = ((BinaryInstruction) inst).getOperand(1);
                            Instruction opInst = (Instruction) op;
                            if (!(op instanceof Constant) && loop.getBbList().contains(opInst.getParent())
                                    && !Invariant.contains(opInst)) {
                                isInvariant = false;
                            }
                        }
                        if (isInvariant) {
                            Invariant.add(inst);
                            newInvariantAdded2Set = true;
                        }
                    }
                }
            }
        } while (newInvariantAdded2Set);
        return Invariant;
    }
}
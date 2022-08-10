package pass.passes;

import java.lang.reflect.InvocationHandler;
import java.security.DomainCombiner;
import java.util.ArrayList;
import java.util.HashSet;
import util.CloneMap;
import analysis.DominatorTree;
import analysis.LoopInfo;
import ir.Argument;
import ir.Constant;
import ir.Function;
import ir.Instruction;
import ir.Loop;
import ir.Type;
import ir.Instruction.Ops;
import ir.instructions.BinaryInstruction;
import ir.instructions.CmpInst;
import ir.instructions.Instructions.CallInst;
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

    DominatorTree DT = null;

    @Override
    public String getName() {
        return "LICM";
    }

    @Override
    public void runOnFunction(Function func) {
        LoopInfo loopInfo = func.getLoopInfo();
        DT = func.getAndUpdateDominatorTree();
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
        if (loop.getLoopPrehead() != null) {
            ArrayList<Instruction> Invariant = detectInvarint(loop); // 循环不变量标记
            ArrayList<Instruction> Invariantorder = new ArrayList<>(); // 循环不变量标记提升的顺序
            // 获取被标记的循环不变量的提升顺序
            while (Invariant.size() != Invariantorder.size()) {
                for (var inst : Invariant) {
                    if (Invariantorder.contains(inst)) {
                        continue;
                    }
                    if (inst instanceof BinaryInstruction) { // 目前只考虑了binary instruction
                        BinaryInstruction binInst = (BinaryInstruction) inst;
                        // 如果它的oprand是循环不变量且在loop中，那么需要先提升它的operand，此次先跳过
                        boolean canLift = true;
                        for (int i = 0; i < binInst.getNumOperands(); ++i) {
                            if (binInst.getOperand(i) instanceof Instruction) {
                                Instruction opInst = (Instruction) binInst.getOperand(i);
                                if (loop.getBbList().contains(opInst.getParent()) && !Invariantorder.contains(opInst)) {
                                    canLift = false;
                                }
                            }
                        }
                        if (canLift) {
                            Invariantorder.add(inst);
                        }
                    } else if (inst instanceof CmpInst) {
                        CmpInst cmpInst = (CmpInst) inst;
                        // 如果它的oprand是循环不变量且在loop中，那么需要先提升它的operand，此次先跳过
                        boolean canLift = true;
                        for (int i = 0; i < cmpInst.getNumOperands(); ++i) {
                            if (cmpInst.getOperand(i) instanceof Instruction) {
                                Instruction opInst = (Instruction) cmpInst.getOperand(i);
                                if (loop.getBbList().contains(opInst.getParent()) && !Invariantorder.contains(opInst)) {
                                    canLift = false;
                                }
                            }
                        }
                        if (canLift) {
                            Invariantorder.add(inst);
                        }
                    } else if (inst instanceof CallInst) {
                        CallInst callInst = (CallInst) inst;
                        // 如果它的oprand是循环不变量且在loop中，那么需要先提升它的operand，此次先跳过
                        boolean canLift = true;
                        for (int i = 1; i < callInst.getNumOperands(); ++i) {
                            if (callInst.getOperand(i) instanceof Instruction) {
                                Instruction opInst = (Instruction) callInst.getOperand(i);
                                if (loop.getBbList().contains(opInst.getParent()) && !Invariantorder.contains(opInst)) {
                                    canLift = false;
                                }
                            }
                        }
                        if (canLift) {
                            Invariantorder.add(inst);
                        }
                    }
                }
            }
            // 将标记的循环不变量按顺序提升到循环外部
            for (var inst : Invariantorder) {
                if (inst instanceof BinaryInstruction) {
                    inst.getInstNode().remove();
                    loop.getLoopPrehead().getInstList().insertBeforeEnd(inst.getInstNode());
                }
            }
        }
    }

    public ArrayList<Instruction> detectInvarint(Loop loop) {
        ArrayList<Instruction> Invariant = new ArrayList<>();
        boolean newInvariantAdded2Set;
        do {
            newInvariantAdded2Set = false;
            for (var block : loop.getBbList()) {
                for (Instruction inst : block.getInstList()) { // 对每一个block的每一个instruction遍历
                    if (inst instanceof BinaryInstruction) {
                        boolean isInvariant = true;
                        if (Invariant.contains(inst)) {
                            continue;
                        }
                        for (var i = 0; i < inst.getOperandList().size(); i++) { // 对每一个instruction的每一个use operand遍历
                            var op = inst.getOperand(i);
                            if (!(op instanceof Argument)) {
                                if (!(op instanceof Constant)) {
                                    if (op instanceof Instruction) {
                                        Instruction opInst = (Instruction) op;
                                        if (loop.getBbList().contains(opInst.getParent())
                                                && !Invariant.contains(opInst)) {
                                            isInvariant = false;
                                        }
                                    }
                                }
                            }
                        }
                        if (isInvariant) {
                            Invariant.add(inst);
                            System.out.println("add invariant: " + inst);
                            newInvariantAdded2Set = true;
                        }
                    } else if (inst instanceof CmpInst) {
                        boolean isInvariant = true;
                        if (Invariant.contains(inst)) {
                            continue;
                        }
                        for (var i = 0; i < inst.getOperandList().size(); i++) { // 对每一个instruction的每一个use operand遍历
                            var op = inst.getOperand(i);
                            if (!(op instanceof Argument)) {
                                if (!(op instanceof Constant)) {
                                    if (op instanceof Instruction) {
                                        Instruction opInst = (Instruction) op;
                                        if (loop.getBbList().contains(opInst.getParent())
                                                && !Invariant.contains(opInst)) {
                                            isInvariant = false;
                                        }
                                    }
                                }
                            }
                        }
                        if (isInvariant) {
                            Invariant.add(inst);
                            System.out.println("add invariant: " + inst);
                            newInvariantAdded2Set = true;
                        }
                    } else if (inst instanceof CallInst) {
                        boolean isInvariant = true;
                        if (Invariant.contains(inst)) {
                            continue;
                        }
                        if (((CallInst) inst).getCalledFunction().hasSideEffect() == true) {
                            isInvariant = false;
                        }
                        for (var i = 1; i < inst.getOperandList().size(); i++) { // 对每一个instruction的每一个use operand遍历
                            var op = inst.getOperand(i);
                            if (!(op instanceof Argument)) {
                                if (!(op instanceof Constant)) {
                                    if (op instanceof Instruction) {
                                        Instruction opInst = (Instruction) op;
                                        if (loop.getBbList().contains(opInst.getParent())
                                                && !Invariant.contains(opInst)) {
                                            isInvariant = false;
                                        }
                                    }
                                }
                            }
                        }
                        if (isInvariant) {
                            Invariant.add(inst);
//                            System.out.println("add invariant: " + inst);
                            newInvariantAdded2Set = true;
                        }
                    }
                }
            }
        } while (newInvariantAdded2Set);
        return Invariant;
    }
}
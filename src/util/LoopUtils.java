package util;

import ir.Instruction;
import ir.instructions.BinaryInstruction;

public class LoopUtils {

    private static final MyIRBuilder myIRBuilder = MyIRBuilder.getInstance();

    // return a new instruction but uses the old inst operands
    public static Instruction copyInstruction(Instruction inst) {
        Instruction copy = null;
        var ops = inst.getOperandList();
        if (inst instanceof BinaryInstruction) {
            copy = (Instruction) myIRBuilder.createBinary(inst.getOp(), ops.get(0), ops.get(1));
        }
        return copy;
    }

}
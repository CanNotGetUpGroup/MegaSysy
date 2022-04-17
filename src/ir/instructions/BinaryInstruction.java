package ir.instructions;

import ir.Instruction;
import ir.Type;

public class BinaryInstruction extends Instruction {
    public BinaryInstruction(Type type, String name, int numOperands) {
        super(type,name, numOperands);
    }
}

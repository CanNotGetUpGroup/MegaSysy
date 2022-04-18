package ir.instructions;

import ir.Instruction;
import ir.Type;

public class UnaryInstruction extends Instruction {
    public UnaryInstruction(Type type,String name, int numOperands) {
        super(type,name, numOperands);
    }

    public UnaryInstruction(Type type, int numOperands) {
        super(type, numOperands);
    }
}

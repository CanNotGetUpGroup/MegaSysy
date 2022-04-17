package ir.instructions;

import ir.Instruction;
import ir.Type;

public class CastInst extends Instruction {
    public CastInst(Type type, String name, int numOperands) {
        super(type,name ,numOperands);
    }
}

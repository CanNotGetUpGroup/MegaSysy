package ir.instructions;

import ir.Instruction;
import ir.Type;
import ir.Value;

public class UnaryInstruction extends Instruction {
    public UnaryInstruction(Type type,Ops op, Value V) {
        super(type,op, 1);
        if(V!=null) addOperand(V);
    }
}

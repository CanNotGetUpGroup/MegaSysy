package ir.instructions;

import ir.*;

import static ir.Instruction.Ops;

public class BinaryInstruction extends Instruction {
    public BinaryInstruction(Type type,Ops op, String name, Value S1,Value S2) {
        super(type,op, name, 2);
        addOperand(S1);
        addOperand(S2);
    }

    public BinaryInstruction(Type type, Ops op, Value S1,Value S2) {
        super(type,op, 2);
        addOperand(S1);
        addOperand(S2);
    }

    public static BinaryInstruction create(Ops op,Value S1,Value S2){
        assert S1.getType().equals(S2.getType());
        return new BinaryInstruction(S1.getType(),op,S1,S2);
    }

    // %14 = xor i1 %13, true
    public static BinaryInstruction createNot(Value Op){
        Constant C= Constants.ConstantInt.const1_1();
        return create(Ops.Xor,Op,C);
    }
}

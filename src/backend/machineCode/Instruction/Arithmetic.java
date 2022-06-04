package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;

public class Arithmetic extends MachineInstruction {
    private Register destReg;
    private Register op1;
    private MCOperand op2;
    private Type type;

    public enum Type{
        ADD,
        SUB,
        MUL,
        SDIV, // signed divide
    }

    public Arithmetic(MachineBasicBlock parent, Type type, Register destReg, Register op1, MCOperand op2){
        super(parent);

        assert type != Type.MUL && type != Type.SDIV || op2 instanceof Register;

        this.type = type;
        this.destReg = destReg;
        this.op1 = op1;
        this.op2 = op2;
    }

    public Arithmetic(MachineBasicBlock parent, Type type, Register op1, MCOperand op2){
        super(parent);
        this.type = type;
        this.destReg = op1;
        this.op1 = op1;
        this.op2 = op2;
    }

    @Override
    public String toString() {
        return type.toString() + "\t" + destReg.toString() + ", " + op1.toString() + ", " + op2.toString();
    }
}

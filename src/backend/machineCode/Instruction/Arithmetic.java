package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.ImmediateNumber;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;

public class Arithmetic extends MachineInstruction {
    private Register destReg;
    private MCOperand op1;
    private MCOperand op2;
    private Type type;

    // TODO: 取余数
    public enum Type {
        ADD,
        SUB,
        MUL,
        DIV, // for float div
        SDIV, // signed divide
        RSB, // reverse substract
        LSL, // Logical Shift Left
    }

    public Arithmetic(MachineBasicBlock parent, Type type, Register destReg, Register op1, MCOperand op2) {
        super(parent);

        if (op2 == null || op1 == null) {
            throw new RuntimeException("null operators");
        }

        this.type = type;
        this.destReg = destReg;
        this.op1 = op1;
        this.op2 = op2;
    }

    public Arithmetic(MachineBasicBlock parent, Type type, Register op1, MCOperand op2) {

        super(parent);

        if (op2 == null || op1 == null) {
            throw new RuntimeException("null operators");
        }

        this.type = type;
        this.destReg = op1;
        this.op1 = op1;
        this.op2 = op2;
    }

    public Arithmetic(MachineBasicBlock parent, Type type, Register op1, int op2) {
        super(parent);

        if (op1 == null) {
            throw new RuntimeException("null operators");
        }

        this.type = type;
        this.destReg = op1;
        this.op1 = op1;
        this.op2 = ImmediateNumber.getLegalOperand(parent, op2);
    }

    public Arithmetic(MachineBasicBlock parent, Type type, Register dest, Register op1, int op2) {
        super(parent);
        if (op1 == null) {
            throw new RuntimeException("null operators");
        }
        this.type = type;
        this.destReg = dest;
        this.op1 = op1;
        this.op2 = ImmediateNumber.getLegalOperand(parent, op2);
    }

    @Override
    public String toString() {
        return (isForFloat() ? "v" : "") + type.toString() + typeInfoString() + "\t" + destReg.toString() + ", " + op1.toString() + ", " + op2.toString();
    }

    @Override
    public Register getDest() {
        return destReg;
    }

    @Override
    public void setDest(Register dest) {
        this.destReg = dest;
    }

    @Override
    public MCOperand getOp1() {
        return op1;
    }

    @Override
    public void setOp1(MCOperand op) {
        this.op1 = op;
    }

    @Override
    public MCOperand getOp2() {
        return op2;
    }

    @Override
    public void setOp2(MCOperand op) {
        this.op2 = op;
    }
}

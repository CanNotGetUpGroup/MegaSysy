package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;

import java.util.ArrayList;
import java.util.List;

public class PushOrPop extends MachineInstruction {

    // TODO : 支持多个寄存器一起
    public PushOrPop(MachineBasicBlock parent, Type type, Register op) {
        super(parent);
        this.op = op;
        this.type = type;
    }

    public PushOrPop(MachineBasicBlock parent, PushOrPop inst) {
        super(parent, inst);
        this.op = inst.op;
        this.type = inst.type;
    }

    Register op;

    public Type getType() {
        return type;
    }

    Type type;

    public enum Type {
        Push,
        Pop
    }

    @Override
    public String toString() {
        return (isForFloat() ? "v" : "") + type.name() + condString() + '\t' + " { " + op.toString() + " }";
    }

    @Override
    public void setOp2(MCOperand op) {
        assert op instanceof Register;
        if (type == Type.Push) {
            this.op = (Register) op;
        }
    }

    @Override
    public MCOperand getOp2() {
        if (type == Type.Push)
            return this.op;
        return null;
    }

    @Override
    public void setOp1(MCOperand op) {
        throw new RuntimeException("Unfinished");
    }


    @Override
    public void setDest(Register dest) {
        if (type == Type.Pop)
            this.op = dest;
    }

    @Override
    public MachineInstruction setForFloat(boolean isForFloat) {
        setForFloat(isForFloat, new ArrayList<>());
        return this;
    }

    @Override
    public Register getDest() {
        if (type == Type.Pop)
            return this.op;
        return null;
    }
}

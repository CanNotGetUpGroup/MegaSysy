package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;

public class PushOrPop extends MachineInstruction {

    // TODO : 支持多个寄存器一起
    public PushOrPop(MachineBasicBlock parent, Type type, Register op) {
        super(parent);
        this.op = op;
        this.type = type;
    }

    Register op;
    Type type;

    public enum Type {
        Push,
        Pop
    }

    @Override
    public String toString() {
        return  type.name() + " { " + op.toString() + " }";
    }

    @Override
    public void setOp2(MCOperand op) {
        assert op instanceof Register;
        this.op =(Register) op;
    }

    @Override
    public MCOperand getOp2() {
        return this.op;
    }

    @Override
    public void setOp1(MCOperand op) {
        throw new RuntimeException("Unfinished");
    }


    @Override
    public void setDest(Register dest) {
        throw new RuntimeException("Unfinished");
    }
}

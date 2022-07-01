package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;

public class Push extends MachineInstruction {

    public Push(MachineBasicBlock parent, MCOperand op) {
        super(parent);
        this.op = op;
    }

    MCOperand op;

    @Override
    public String toString() {
        return "push" + op.toString();
    }

    @Override
    public void setOp1(MCOperand op) {
        throw new RuntimeException("Unfinished");
    }

    @Override
    public void setOp2(MCOperand op) {
        throw new RuntimeException("Unfinished");
    }
}

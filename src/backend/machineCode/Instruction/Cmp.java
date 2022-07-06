package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;

public class Cmp extends MachineInstruction {

    private MCOperand op1;
    private MCOperand op2;

    public Cmp(MachineBasicBlock parent, MCOperand op1, MCOperand op2) {
        super(parent);
        this.op1 = op1;
        this.op2 = op2;
    }

    @Override
    public MCOperand getOp1() {
        return op1;
    }

    @Override
    public void setOp1(MCOperand op1) {
        this.op1 = op1;
    }

    @Override
    public MCOperand getOp2() {
        return op2;
    }

    @Override
    public void setOp2(MCOperand op2) {
        this.op2 = op2;
    }

    @Override
    public String toString() {
        return "cmp " + op1.toString() + " , " + op2.toString();
    }
}

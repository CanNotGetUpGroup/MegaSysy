package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;

public class Move extends MachineInstruction {
    private Register dest;
    private MCOperand op;

    public Move(MachineBasicBlock parent, MCOperand dest, MCOperand op) {
        super(parent);
        assert dest instanceof Register;
        this.dest = (Register) dest;
        this.op = op;
    }

    @Override
    public Register getDest() {
        return dest;
    }

    @Override
    public void setDest(Register dest) {
        this.dest = dest;
    }

    @Override
    public void setOp1(MCOperand op) {
        throw new RuntimeException("Unfinished");
    }

    @Override
    public MCOperand getOp2() {
        return op;
    }

    @Override
    public void setOp2(MCOperand op) {
        this.op = op;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isForFloat()) sb.append("v");
        sb.append("mov");
        if (isForFloat()) sb.append(typeInfoString());
        sb.append( "\t").append(dest.toString() ).append(", ") .append(op);
        return sb.toString();
    }
}

package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;

import java.util.ArrayList;
import java.util.List;

public class Ubfx extends MachineInstruction {
    private Register dest;

    @Override
    public MCOperand getOp1() {
        return op;
    }

    @Override
    public void setOp1(MCOperand op) {
        assert op instanceof Register;
        this.op = (Register)op;
    }

    private Register op;
    private int lsb;
    private int width;

    public Ubfx(MachineBasicBlock parent, Register dest, Register op, int lsb, int width) {
        super(parent);
        this.dest = dest;
        this.op = op;
        this.lsb = lsb;
        this.width = width;
    }

    public Ubfx(MachineBasicBlock parent, Ubfx inst) {
        super(parent, inst);
        this.dest = inst.dest;
        this.op = inst.op;
        this.lsb = inst.lsb;
        this.width = inst.width;
    }

    @Override
    public MachineInstruction setForFloat(boolean isForFloat) {
        throw new RuntimeException(">>");
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
    public MCOperand getOp2() {
        return null;
    }

    @Override
    public void setOp2(MCOperand op) {
        throw new RuntimeException("No");
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ubfx");
        sb.append(condString());

        sb.append("\t").append(dest.toString())
                .append(", ").append(op)
                .append(", ").append(lsb)
                .append(", ").append(width);

        return sb.toString();
    }

    @Override
    public ArrayList<Register> getUse() {
        var ans = new ArrayList<Register>();
        ans.add(op);
        return ans;
    }

    @Override
    public ArrayList<Register> getDef() {
        var ans = new ArrayList<Register>();
        ans.add(dest);
        return ans;
    }
}

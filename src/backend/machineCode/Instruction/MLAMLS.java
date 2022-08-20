package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.*;

import java.util.ArrayList;
import java.util.List;

public class MLAMLS extends MachineInstruction {
    private Register destReg;
    private MCOperand op1;
    private MCOperand op2;
    private MCOperand op3;

    private boolean isMls = false;

    public void setMls(boolean isMls) {
        this.isMls = isMls;
    }

    public boolean isMls() {
        return isMls;
    }
    
    public MLAMLS(MachineBasicBlock parent, Register destReg, MCOperand op1, MCOperand op2, MCOperand op3) {
        super(parent);

        if (op3 == null || op2 == null || op1 == null) {
            throw new RuntimeException("null operators");
        }

        if(!(op3 instanceof Register) || !(op2 instanceof Register) ||! (op1 instanceof Register)) {
            throw new RuntimeException("mla/mls only support regs");
        }

        if(((Register)op3).isFloat() || ((Register)op2).isFloat() || ((Register)op1).isFloat()) {
            throw new RuntimeException("mla/mls dont support float");
        }

        this.destReg = destReg;
        this.op1 = op1;
        this.op2 = op2;
        this.op3 = op3;
        setForFloat(false);
    }

    public MLAMLS(MachineBasicBlock parent, MLAMLS src) {
        super(parent, src);

        this.destReg = src.getDest();
        this.op1 = src.getOp1();
        this.op2 = src.getOp2();
        this.op3 = src.getOp3();
        setForFloat(false);
        this.isMls = src.isMls();
    }

    @Override
    public MachineInstruction setForFloat(boolean isForFloat) {
        setForFloat(isForFloat, new ArrayList<>(List.of("f32")));
        return this;
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
        assert op1 instanceof Register;
        this.op1 = (Register)op;
    }

    @Override
    public MCOperand getOp2() {
        return op2;
    }

    @Override
    public void setOp2(MCOperand op) {
        assert op2 instanceof Register;
        this.op2 = (Register)op;
    }

    public MCOperand getOp3() {
        return op3;
    }

    public void setOp3(MCOperand op) {
        assert op3 instanceof Register;
        this.op3 = (Register)op;
    }

    @Override
    public String toString() {
        return  (isMls ? "MLS" : "MLA") + condString() + "\t"
                + destReg.toString() + ", " + op1.toString() + ", " + op2.toString() + ", " + op3.toString();
    }

    @Override
    public ArrayList<Register> getUse() {
        var ans = new ArrayList<Register>();

        ans.add((Register)op1);
        ans.add((Register)op2);
        ans.add((Register)op3);

        return ans;
    }

    @Override
    public ArrayList<Register> getDef() {
        var ans = new ArrayList<Register>();
        ans.add(destReg);
        return ans;
    }
}

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

    boolean isMls = false;

    public void setMls(boolean isMls) {
        this.isMls = isMls;
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

    @Override
    public MachineInstruction setForFloat(boolean isForFloat) {
        setForFloat(isForFloat, new ArrayList<>(List.of("f32")));
        return this;
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

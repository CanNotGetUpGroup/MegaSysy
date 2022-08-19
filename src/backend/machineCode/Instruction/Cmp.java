package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;

import java.util.ArrayList;
import java.util.List;

public class Cmp extends MachineInstruction {

    private Register op1;
    private MCOperand op2;
    private boolean isCmn = false;

    public void setCmn(boolean isCmn) {
        this.isCmn = isCmn;
    }

    public Cmp(MachineBasicBlock parent, Register op1, MCOperand op2) {
        super(parent);
        if(op2 instanceof  Register && op1.isFloat() != ((Register) op2).isFloat())
            throw  new RuntimeException("Unmatched operand");
        this.op1 = op1;
        this.op2 = op2;
        if (op1.isFloat())
            this.setForFloat(new ArrayList<>(List.of("F32")));
    }
    public Cmp(MachineBasicBlock parent, Cmp cmp) {
        super(parent, cmp);
        this.op1 = (Register) cmp.getOp1();
        this.op2 = cmp.getOp2();
    }

        @Override
    public MachineInstruction setForFloat(boolean isForFloat) {
        setForFloat(isForFloat, new ArrayList<>(List.of("f32")));
        return this;
    }

    @Override
    public MCOperand getOp1() {
        return op1;
    }

    @Override
    public void setOp1(MCOperand op1) {
        if (op1 instanceof Register)
            this.op1 = (Register) op1;
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
        return (isForFloat()? "v": "") + (isCmn ? "cmn":"cmp") + condString()+ typeInfoString() + " " + op1.toString() + " , " + op2.toString();

    }
}

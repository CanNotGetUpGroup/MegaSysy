package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;

public class Branch extends MachineInstruction {
    // 暂时没考虑过Thumb2指令集
    private boolean storeLR = false;
    private Register destReg;
    private MachineBasicBlock destBB;

    @Override
    public void setOp1(MCOperand op) {
        throw new RuntimeException("Unfinished");
    }

    @Override
    public void setOp2(MCOperand op) {
        throw new RuntimeException("Unfinished");
    }

    private enum Type{
        REG,
        LABEL,
    }
    private Type type;

    public Branch(MachineBasicBlock parent, MachineBasicBlock destBasicBlock, boolean storeLR){
        super(parent);
        this.destBB = destBasicBlock;
        this.storeLR = storeLR;
        type = Type.LABEL;
    }

    public Branch(MachineBasicBlock parent, Register destReg, boolean storeLR){
        super(parent);
        this.destReg = destReg;
        this.storeLR = storeLR;
        type = Type.REG;
    }

    @Override
    public String toString() {
        String inst = "BX";
        if(storeLR) inst += "L";
        return switch (type){
            case REG -> inst + "\t" + destReg.toString();
            case LABEL -> inst + "\t" + destBB.getLabel();
        };
    }


}

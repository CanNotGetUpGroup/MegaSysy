package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Register.Register;

public class Branch extends MachineInstruction {
    // 暂时没考虑过Thumb2指令集
    private boolean storeLR = false;
    private Register destReg;
    private MachineBasicBlock destBB;

    private enum Type{
        REG,
        LABEL,
    }
    private Type type;

    public Branch(MachineBasicBlock destBasicBlock, boolean storeLR){
        this.destBB = destBasicBlock;
        this.storeLR = storeLR;
        type = Type.LABEL;
    }

    public Branch(Register destReg, boolean storeLR){
        this.destReg = destReg;
        this.storeLR = storeLR;
        type = Type.REG;
    }

    @Override
    public String toString() {
        String inst = "B";
        if(storeLR) inst += "L";
        return switch (type){
            case REG -> inst + "\t" + destReg.toString();
            case LABEL -> inst + "\t" + destBB.getLabel();
        };
    }
}

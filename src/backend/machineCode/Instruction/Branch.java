package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;

public class Branch extends MachineInstruction {
    // 暂时没考虑过Thumb2指令集
    private boolean storeLR = false;
    private Register destReg;

    // TODO: 这里需要改改
    private MachineBasicBlock destBB;
    private MachineFunction destf;
    private Type type;

    public enum Type {
        Call,
        Ret,
        Block,
    }

    public Type getType() {
        return type;
    }

    @Override
    public void setOp1(MCOperand op) {
        throw new RuntimeException("Unfinished");
    }

    @Override
    public void setOp2(MCOperand op) {
        throw new RuntimeException("Unfinished");
    }

    private enum DestType {
        REG,
        LABEL,
        FUNC,
    }

    private DestType destType;

    public Branch(MachineBasicBlock parent, MachineBasicBlock destBasicBlock, boolean storeLR, Type type) {
        super(parent);
        this.destBB = destBasicBlock;
        this.storeLR = storeLR;
        destType = DestType.LABEL;
        this.type = type;
    }

    public Branch(MachineBasicBlock parent, MachineFunction f, boolean storeLR, Type type) {
        super(parent);
        this.destf = f;
        this.storeLR = storeLR;
        destType = DestType.FUNC;
        this.type = type;
    }


    public Branch(MachineBasicBlock parent, Register destReg, boolean storeLR, Type type) {
        super(parent);
        this.destReg = destReg;
        this.storeLR = storeLR;
        destType = DestType.REG;
        this.type = type;
    }

    @Override
    public String toString() {
        String inst = "B";
        if (storeLR) inst += "L";
        if (this.destType != DestType.LABEL)
            inst += "X";
        if (getCond() != null) inst += getCond();
        return switch (destType) {
            case REG -> inst + "\t" + destReg.toString();
            case LABEL -> inst + "\t" + destBB.getLabel();
            case FUNC -> inst + "\t" + destf.getLabel();
        };
    }


}

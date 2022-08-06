package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.MCRegister;
import backend.machineCode.Operand.Register;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Branch extends MachineInstruction {
    // 暂时没考虑过Thumb2指令集
    private boolean storeLR;
    private Register destReg;

    // TODO: 这里需要改改
    private MachineBasicBlock destBB;
    private MachineFunction destf;
    private String destStr;
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
        String
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

    public Branch(MachineBasicBlock parent, String f, boolean storeLR, Type type) {
        super(parent);
        this.destStr = f;
        this.storeLR = storeLR;
        destType = DestType.String;
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
    public MachineInstruction setForFloat(boolean isForFloat) {
        throw  new RuntimeException("Unfinished");
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
            case String -> inst + "\t" + destStr;
        };
    }

    @Override
    public ArrayList<Register> getUse() {
        var ans = new ArrayList<Register>();
        if (type == Type.Call) {
            ans.addAll(IntStream
                    .range(0, 4)
                    .mapToObj(i -> new MCRegister(Register.Content.Int, i))
                    .collect(Collectors.toSet()));
        }

        return ans;
    }

    @Override
    public Register getDef() {
        if(type == Type.Call)
            return new MCRegister(Register.Content.Int, 0);
        return null;
    }

    public static void main(String[] args) {

    }
}

package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.MCRegister;
import backend.machineCode.Operand.Register;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Branch extends MachineInstruction {
    // 暂时没考虑过Thumb2指令集
    private boolean storeLR;
    private Register destReg;

    // TODO: 这里需要改改
    private MachineBasicBlock destBB;

    public boolean isStoreLR() {
        return storeLR;
    }

    public Register getDestReg() {
        return destReg;
    }

    public MachineBasicBlock getDestBB() {
        return destBB;
    }

    public MachineFunction getDestf() {
        return destf;
    }

    public String getDestStr() {
        return destStr;
    }

    public DestType getDestType() {
        return destType;
    }

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

    public Branch(MachineBasicBlock parent, Branch br) {
        super(parent, br);
        this.destBB = br.getDestBB();
        this.destStr = br.destStr;
        this.destReg = br.getDestReg();
        this.storeLR = br.isStoreLR();
        destType = br.getDestType();
        this.type = br.getType();
        this.destf = br.destf;
    }

    @Override
    public MachineInstruction setForFloat(boolean isForFloat) {
        throw new RuntimeException("Unfinished");
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
        } + "  @" + this.getType();
    }

    @Override
    public ArrayList<Register> getUse() {
        var ans = new ArrayList<Register>();
        if (type == Type.Call) {
            var func = getDestf();
            ans.addAll(IntStream
                    .range(0, Math.min(func.getIntParaNum(), 4))
                    .mapToObj(i -> new MCRegister(Register.Content.Int, i))
                    .collect(Collectors.toSet()));

            ans.addAll(IntStream
                    .range(0, Math.max(func.getFloatParaNum(), 16))
                    .mapToObj(i -> new MCRegister(Register.Content.Float, i))
                    .collect(Collectors.toSet()));
        }

        return ans;
    }

    @Override
    public ArrayList<Register> getDef() {
        var ans = new ArrayList<Register>();
        if (type == Type.Call) {
            ans.addAll(IntStream
                    .range(0, 4)
                    .mapToObj(i -> new MCRegister(Register.Content.Int, i))
                    .collect(Collectors.toSet()));
            ans.add(new MCRegister(MCRegister.RegName.IP));
            ans.addAll(IntStream
                    .range(0, 16)
                    .mapToObj(i -> new MCRegister(Register.Content.Float, i))
                    .collect(Collectors.toSet()));
        }
        if (isStoreLR())
            ans.add(new MCRegister(Register.Content.Int, 13));

        return ans;
    }

}

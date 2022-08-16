package backend.machineCode.Instruction;

import backend.machineCode.Addressable;
import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.ImmediateNumber;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;
import backend.machineCode.Operand.StackOffsetNumber;
import ir.Instruction;

public class LoadImm extends MachineInstruction {

    public MCOperand getSrc() {
        return src;
    }

    private MCOperand src;
    private Register dest;

    public LoadImm(MachineBasicBlock parent, Register dest, MCOperand src) {
        super(parent);
        parent.getParent().getRegDefineMap().put(dest, this);
        this.src = src;
        this.dest = dest;
    }


    public LoadImm(MachineBasicBlock parent, Register dest, int src) {
        super(parent);
        parent.getParent().getRegDefineMap().put(dest, this);
        this.src = new ImmediateNumber(src);
        this.dest = dest;
    }

    public LoadImm(MachineBasicBlock parent, Register dest, float src) {
        super(parent);
        this.src = new ImmediateNumber(Float.floatToIntBits(src));
        this.dest = dest;
    }


    public LoadImm(MachineBasicBlock parent, LoadImm imm) {
        super(parent, imm);
        parent.getParent().getRegDefineMap().put(dest, this);
        this.src = imm.getSrc();
        this.dest = imm.getDest();
    }

    @Override
    public MCOperand getOp2() {
        return src;
    }

    @Override
    public void setOp2(MCOperand op) {
        this.src = op;
    }

    @Override
    public MachineInstruction setForFloat(boolean isForFloat) {
        throw new RuntimeException("Unfinished");
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
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (src instanceof Addressable) {
            sb.append("movw").append(condString()).append("\t").append(dest)
                    .append(", #:lower16:").append(((Addressable) src).getLabel()).append("\n")
                    .append("\tmovt").append(condString()).append("\t").append(dest)
                    .append(", #:upper16:").append(((Addressable) src).getLabel()).append("\n");
        } else if (src instanceof ImmediateNumber) {
            int value = ((ImmediateNumber) src).getValue();
            if (value <= 65535 && value >= 0 || ImmediateNumber.isLegalImm(value)) {
                if (dest.isFloat())
                    sb.append("v");
                sb.append("mov").append(condString());
                if (dest.isFloat()) sb.append(typeInfoString());
                sb.append("\t").append(dest).append(", ").append(value);
            } else if (ImmediateNumber.isLegalImm(~value)) {
                if (dest.isFloat())
                    sb.append("v");
                sb.append("mvn").append(condString());
                if (dest.isFloat()) sb.append(typeInfoString());
                sb.append("\t").append(dest).append(", ").append(~value);
            } else
                sb.append("movw").append(condString()).append("\t").append(dest).append(", ").append(value & 0xFFFF).append("\n")
                        .append("\tmovt").append(condString()).append("\t").append(dest).append(", ").append((value & 0xFFFF0000) >>> 16);
        } else if(src instanceof StackOffsetNumber){
            return "LoadImm\tstack" + ((StackOffsetNumber) src).getOffset();
        }
        else {
            throw new RuntimeException("Unknown type");
        }
        sb.append("  @LoadImm");
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println(65535);
        System.out.println(~65535);
        System.out.println(Integer.toHexString(65535));
        System.out.println(Integer.toHexString(~65535));
        System.out.println(ImmediateNumber.isLegalImm(65535));
        System.out.println(ImmediateNumber.isLegalImm(~65535));
    }
}

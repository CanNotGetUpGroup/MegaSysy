package backend.machineCode.Instruction;

import backend.machineCode.Addressable;
import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.ImmediateNumber;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;
import ir.Instruction;

public class LoadImm extends MachineInstruction {

    private MCOperand src;
    private Register dest;

    public LoadImm(MachineBasicBlock parent, Register dest, MCOperand src) {
        super(parent);
        this.src = src;
        this.dest = dest;
    }


    public LoadImm(MachineBasicBlock parent, Register dest, int src) {
        super(parent);
        this.src = new ImmediateNumber(src);
        this.dest = dest;
    }

    public LoadImm(MachineBasicBlock parent, Register dest, float src) {
        super(parent);
        this.src = new ImmediateNumber(Float.floatToIntBits(src));
        this.dest = dest;
    }


    @Override
    public MachineInstruction setForFloat(boolean isForFloat) {
       throw  new RuntimeException("Unfinished");
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
            sb.append("movw\t").append(dest)
                    .append(", #:lower16:").append(((Addressable) src).getLabel()).append("\n")
                    .append("\tmovt\t").append(dest)
                    .append(", #:upper16:").append(((Addressable) src).getLabel()).append("\n");
        } else if (src instanceof ImmediateNumber) {
            int value = ((ImmediateNumber) src).getValue();
            if (ImmediateNumber.isLegalImm(value)) {
                if (dest.isFloat())
                    sb.append("v");
                sb.append("mov");
                if (dest.isFloat()) sb.append(typeInfoString());
                sb.append("\t").append(dest).append(", ").append(value);
            } else if(ImmediateNumber.isLegalImm(~value)){
                if (dest.isFloat())
                    sb.append("v");
                sb.append("mvn");
                if (dest.isFloat()) sb.append(typeInfoString());
                sb.append("\t").append(dest).append(", ").append(~value);
            }
            else
                sb.append("movw\t").append(dest).append(", ").append(value & 0xFFFF).append("\n")
                        .append("\tmovt\t").append(dest).append(", ").append((value & 0xFFFF0000) >>> 16);
        } else {
            throw new RuntimeException("Unknown type");
        }

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

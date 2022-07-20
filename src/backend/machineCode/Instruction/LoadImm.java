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
                    .append("movt\t").append(dest)
                    .append(", #:upper16:").append(((Addressable) src).getLabel()).append("\n");
        } else if (src instanceof ImmediateNumber){
            int value = ((ImmediateNumber) src).getValue();
            sb.append("movw\t").append(dest).append(value & 0xFFFF).append("\n")
                    .append("movt\t").append(dest).append(value & 0xFFFF0000).append("\n");
        } else {
            throw new RuntimeException("Unknown type");
        }

        return sb.toString();
    }
}
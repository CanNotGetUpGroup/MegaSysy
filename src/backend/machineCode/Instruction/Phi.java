package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.Register;
import ir.instructions.Instructions;

public class Phi extends MachineInstruction {
    Instructions.PHIInst ir;
    Register dest;

    public Phi(MachineBasicBlock parent,Register dest, Instructions.PHIInst ir) {
        super(parent);
        this.dest = dest;
        this.ir = ir;
    }

    @Override
    public Register getDest() {
        return dest;
    }

    public Instructions.PHIInst getIr() {
        return ir;
    }

    @Override
    public void setDest(Register dest) {
        this.dest = dest;
    }

    @Override
    public MachineInstruction setForFloat(boolean isForFloat) {
        setForFloat(isForFloat, null);
        return this;
    }

    @Override
    public String toString() {
        return ir.toString();
    }
}

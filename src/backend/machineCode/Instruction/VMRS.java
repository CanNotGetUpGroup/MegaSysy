package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;


/**
 * Transfer the contents of a VFP system register to an ARM register.
 */
public class VMRS extends MachineInstruction {
    private Register op;
    private Register dest;

    public VMRS(MachineBasicBlock parent, Register dest, Register op) {
        super(parent);
        this.op = op;
        this.dest = dest;
        setForFloat(true, null);
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
    public MCOperand getOp2() {
        return op;
    }

    @Override
    public void setOp2(MCOperand op) {
        if (op instanceof Register)
            this.op = (Register) op;
    }

    public MachineInstruction setForFloat(boolean ifForFloat) {
        throw new RuntimeException("Should call this");
    }


    @Override
    public String toString() {
        return "vmrs" + typeInfoString() + "\t" + dest + ", " + op;
    }
}


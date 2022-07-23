package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;


/**
 * Convert between floating-point numbers and integers.
 * VCVT{cond}.F32.type ,Sd Sm
 * VCVT{R}{cond}..F32 type,Sd Sm
 * R：makes the operation use the rounding mode specified by the FPSCR. Otherwise, the operation rounds towards zero.
 * type：can be either U32 (unsigned 32-bit integer) or S32 (signed 32-bit integer).
 */
public class VCVT extends MachineInstruction {
    private Register op;
    private Register dest;

    public VCVT(MachineBasicBlock parent, Register dest, Register op) {
        super(parent);
        this.op = op;
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
    public MCOperand getOp2() {
        return op;
    }

    @Override
    public void setOp2(MCOperand op) {
        if (op instanceof Register)
            this.op = (Register) op;
    }

    @Override
    public String toString() {
        return "vcvt" + typeInfoString() + "\t" + dest +", " +  op;
    }
}

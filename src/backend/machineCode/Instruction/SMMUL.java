package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;

import java.util.ArrayList;
import java.util.List;

public class SMMUL extends MachineInstruction {
    Register destHi, op1, op2;

    public SMMUL(MachineBasicBlock parent, Register destHi, Register op1, Register op2) {
        super(parent);
        this.destHi = destHi;
        this.op2 = op2;
        this.op1 = op1;
    }

    public SMMUL(MachineBasicBlock parent, SMMUL inst) {
        super(parent);
        this.destHi = inst.destHi;
        this.op2 = inst.op2;
        this.op1 = inst.op1;
    }

    @Override
    public Register getDest() {
        return destHi;
    }

    @Override
    public void setDest(Register dest) {
        this.destHi = dest;
    }


    @Override
    public Register getOp1() {
        return op1;
    }

    @Override
    public void setOp1(MCOperand op1) {
        this.op1 = (Register) op1;
    }

    @Override
    public Register getOp2() {
        return op2;
    }

    @Override
    public void setOp2(MCOperand op2) {
        this.op2 = (Register) op2;
    }

    @Override
    public ArrayList<Register> getDef() {
        return new ArrayList<>(List.of(destHi));
    }

    @Override
    public String toString() {
        return "SMMUL" + (isSetState() ? "s" : "") + condString() + "\t" + destHi + ", " + op1 + ", " + op2;
    }

    @Override
    public MachineInstruction setForFloat(boolean isForFloat) {
        return null;
    }
}

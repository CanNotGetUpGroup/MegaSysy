package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;

public class Shift extends MachineInstruction {
    public enum Type{
        ASR, // Arithmetic Shift Right
        LSL, // Logical Shift Left
        LSR, // Logical Shift Right
        ROR, // Rotate Right
        RRX, // Rotate Right with Extend
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Register getDest() {
        return dest;
    }

    public void setDest(Register dest) {
        this.dest = dest;
    }

    public Register getOp1() {
        return op1;
    }

    public void setOp1(Register op1) {
        this.op1 = op1;
    }

    public MCOperand getSh() {
        return sh;
    }

    public void setSh(MCOperand sh) {
        this.sh = sh;
    }

    private Type type;
    private Register dest;
    private Register op1;
    private MCOperand sh;

    public Shift(MachineBasicBlock parent, Type type, Register dest, Register op1, MCOperand shift){
        super(parent);
        this.type = type;
        this.dest = dest;
        this.op1 = op1;
        this.sh = shift;
    }

    public Shift(MachineBasicBlock parent, Type type, Register op1, MCOperand shift){
        super(parent);
        this.type = type;
        this.dest = op1;
        this.op1 = op1;
        this.sh = shift;
    }


    public String toString(){
        return type.toString() + "\t" + dest.toString() + ", " + op1.toString() + sh.toString();
    }
}

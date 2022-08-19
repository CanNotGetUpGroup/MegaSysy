package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.Address;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;

import java.util.ArrayList;
import java.util.List;

public class LoadOrStore extends MachineInstruction {
    public enum Type {
        LOAD,
        STORE
    }

    private final Type type;
     Register dest;
    private MCOperand addr;     // could be address , or literal number

    public LoadOrStore(MachineBasicBlock parent, Type type, Register dest, MCOperand addr) {
        super(parent);
        this.type = type;
        this.dest = dest;
        this.addr = addr;
        setForFloat(dest.isFloat());
    }
    public LoadOrStore(MachineBasicBlock parent, LoadOrStore inst) {
        super(parent, inst);
        this.type = inst.getType();
        this.dest = inst.dest;
        this.addr = inst.addr;
    }

    @Override
    public MachineInstruction setForFloat(boolean isForFloat) {
       setForFloat(isForFloat, new ArrayList<>(List.of("32")));
       return this;
    }

    @Override
    public Register getDest() {
        return type == Type.STORE? null : dest;
    }

    @Override
    public MCOperand getOp1() {
        if (type == Type.STORE) {
            return dest;
        }
        return null;
    }

    public void setOp1(MCOperand op1) {
        if (op1 instanceof Register && type == Type.STORE) {
            dest = (Register) op1;
        }
    }

    @Override
    public MCOperand getOp2() {
        return addr;
    }

    @Override
    public void setOp2(MCOperand op) {
        assert op instanceof Address;
        this.addr = op;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return (isForFloat()? "v":"") + (type == Type.LOAD ? "LDR" : "STR") + condString() + typeInfoString()  + " " + dest.toString() + ", " + addr.toString().substring(0, addr.toString().length()-2) + (hasShift()? ", " + getShifter() : "") + " ]" ;
    }

    @Override
    public void setDest(Register dest) {
        if(type == Type.LOAD)
        this.dest = dest;
    }

}

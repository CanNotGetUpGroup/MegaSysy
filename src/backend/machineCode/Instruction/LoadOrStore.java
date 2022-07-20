package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.Address;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;

public class LoadOrStore extends MachineInstruction {
    public enum Type {
        LOAD,
        STORE
    }

    private final Type type;
    private Register dest;
    private MCOperand addr;     // could be address , or literal number

    public LoadOrStore(MachineBasicBlock parent, Type type, Register dest, MCOperand addr) {
        super(parent);
        this.type = type;
        this.dest = dest;
        this.addr = addr;
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
        return (type == Type.LOAD ? "LDR " : "STR ") + dest.toString() + ", " + addr.toString();
    }

    @Override
    public void setDest(Register dest) {
        if(type == Type.LOAD)
        this.dest = dest;
    }

}

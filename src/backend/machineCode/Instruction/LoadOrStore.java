package backend.machineCode.Instruction;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineInstruction;
import backend.machineCode.Operand.Adress;
import backend.machineCode.Operand.MCOperand;
import backend.machineCode.Operand.Register;

public class LoadOrStore extends MachineInstruction {
    public enum Type{
        LOAD,
        STORE
    }
    private final Type type;
    private Register dest;
    private MCOperand addr;     // could be address , or literal number

    public LoadOrStore(MachineBasicBlock parent, Type type, Register dest, MCOperand addr){
        super(parent);
        this.type = type;
        this.dest = dest;
        this.addr = addr;
    }

    @Override
    public Register getDest() {
        return dest;
    }

    @Override
    public MCOperand getOp2() {
        return addr;
    }

    @Override
    public void setOp2(MCOperand op) {
        assert op instanceof Adress;
        this.addr = op;
    }

    public Type getType(){
        return type;
    }

    @Override
    public String toString() {
        return (type == Type.LOAD? "LDR " : "STR ") + dest.toString() + ", " + addr.toString();
    }

    @Override
    public void setDest(Register dest) {
        this.dest = dest;
    }

    @Override
    public void setOp1(MCOperand op) {
        throw new RuntimeException("Unfinished");
    }
}

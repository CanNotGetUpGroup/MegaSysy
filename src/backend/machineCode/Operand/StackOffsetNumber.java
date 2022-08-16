package backend.machineCode.Operand;

import backend.machineCode.MachineFunction;

public class StackOffsetNumber extends MCOperand{
    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    int offset;
    MachineFunction mf;
    boolean withPush = false;
    public StackOffsetNumber(int offset, MachineFunction func) {
        super(Type.Imm);
        this.offset = offset;
        mf = func;
    }
    public StackOffsetNumber(int offset, MachineFunction func, boolean withPush) {
        super(Type.Imm);
        this.offset = offset;
        mf = func;
        this.withPush = withPush;
    }

    public int getValue(){
        return mf.getStackSize() + offset + (withPush? mf.getStoredRegisterNum() * 4 : 0);
    }

}

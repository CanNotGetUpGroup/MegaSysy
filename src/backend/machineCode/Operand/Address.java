package backend.machineCode.Operand;

import backend.machineCode.MachineBasicBlock;
import backend.machineCode.MachineFunction;
import backend.machineCode.MachineInstruction;

public class Address extends MCOperand{
    private Register reg;

    public Register getReg() {
        return reg;
    }

    public void setReg(Register reg) {
        this.reg = reg;
    }

    public MCOperand getOffset() {
        return offset;
    }

    public void setOffset(MCOperand offset) {
        this.offset = offset;
    }

    public void setOffset(int offset){
        this.offset = new ImmediateNumber(offset);
    }

    private MCOperand offset;

    public Address(Register reg, MCOperand offset){
        super(Type.Addr);
        this.reg = reg;
        this.offset = offset;
    }

    public Address(Register reg, int offset){
        super(Type.Addr);
        this.reg = reg;
        this.offset = new ImmediateNumber(offset);
    }
    public Address(Register reg){
        super(Type.Addr);
        this.reg = reg;
        this.offset = new ImmediateNumber(0);
    }

    public boolean hasConstOffset(){
        return offset instanceof ImmediateNumber;
    }


    @Override
    public String toString() {
        if(hasConstOffset() &&((ImmediateNumber) offset).getValue() == 0)
            return "[ " + reg.toString() + " ]" ;
        return "[ " + reg.toString() + " , " +  offset.toString() + " ]" ;
    }

    @Override
    public boolean equals(Object obj) {
        // 好像没什么问题
        return this.toString().equals(obj.toString());
    }
}

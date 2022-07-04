package backend.machineCode.Operand;

public class Address extends MCOperand{
    private Register reg;
    private int offset;

    public Address(Register reg, int offset){
        super(Type.Addr);
        this.reg = reg;
        this.offset = offset;
    }
    public Address(Register reg){
        super(Type.Addr);
        this.reg = reg;
    }


    @Override
    public String toString() {
        return "[ " + reg.toString() + (offset == 0 ? "" :  " , " + new ImmediateNumber(offset).toString()) + " ]" ;
    }
}

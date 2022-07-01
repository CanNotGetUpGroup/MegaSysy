package backend.machineCode.Operand;

public class Adress extends MCOperand{
    private Register reg;
    private int offset;

    public Adress(Register reg, int offset){
        super(Type.Addr);
        this.reg = reg;
        this.offset = offset;
    }
    public Adress(Register reg){
        super(Type.Addr);
        this.reg = reg;
    }


    @Override
    public String toString() {
        return "[ " + reg.toString() + (offset == 0 ? "" :  " , " + new ImmediateNumber(offset).toString()) + " ]" ;
    }
}

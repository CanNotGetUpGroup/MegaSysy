package backend.machineCode.Operand;

public class ImmediateNumber extends MCOperand{
    int value;
    public ImmediateNumber(int value){
        super(Type.Imm);
        this.value = value;
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}

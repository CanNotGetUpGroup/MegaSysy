package backend.machineCode.Operand;

public abstract class MCOperand {
    public enum Type{
        Imm,
        Reg,
    }
    private Type type;
    public MCOperand(Type type){
        this.type = type;
    }
}

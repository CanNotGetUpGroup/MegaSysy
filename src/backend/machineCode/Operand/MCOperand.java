package backend.machineCode.Operand;

public abstract class MCOperand {
    public enum Type{
        Imm,
        Reg,
        Addr,
    }
    private Type type;
    public MCOperand(Type type){
        this.type = type;
    }

    @Override
    public boolean equals(Object obj){
        if(!(obj instanceof MCOperand)){
            return false;
        }
        return false; //TODO compare!
    }
}

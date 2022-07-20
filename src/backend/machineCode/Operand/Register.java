package backend.machineCode.Operand;

public class Register extends MCOperand {

    public enum Type{
        VIRTUAL,
        MACHINE
    }

    private Type type;

    Register(Type type){
        super(MCOperand.Type.Reg);
        this.type = type;
    }

    public Type getType(){
        return type;
    }
}

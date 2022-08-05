package backend.machineCode.Operand;

public class Register extends MCOperand {

    public enum Type{
        VIRTUAL,
        MACHINE
    }

    public enum Content {
        Float,
        Int
    }

    private Type type;
    private Content content;

    Register(Type type){
        super(MCOperand.Type.Reg);
        this.type = type;
        this.content = Content.Int;
    }

    Register(Type type, Content content){
        super(MCOperand.Type.Reg);
        this.type = type;
        this.content = content;
    }


    public boolean isFloat(){
        return content == Content.Float;
    }

    public boolean isInt(){
        return content == Content.Int;
    }

    public Type getType(){
        return type;
    }

    public boolean isPrecolored() {
        return this instanceof MCRegister && !((MCRegister) this).isAllocated;
    }
}

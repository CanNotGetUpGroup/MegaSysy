package backend.machineCode.Operand;

import java.util.Objects;

public class Register extends MCOperand implements Comparable {

    public enum Type{
        VIRTUAL,
        MACHINE,
        PlaceHolder
    }

    public enum Content {
        Float,
        Int
    }

    int id;
    private Type type;

    public Content getContent() {
        return content;
    }

    private final Content content;

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

    @Override
    public boolean equals(Object obj) {
        return this.toString().equals(obj.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, content);
    }

    public int getId(){
        return id;
    }

    @Override
    public int compareTo(Object o) {
        if(!(o instanceof Register))
            throw new RuntimeException("Can't compare");
        return ((Register) o).getId() - getId();
    }
}

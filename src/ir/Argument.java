package ir;

public class Argument extends Value {
    private Function parent;
    private int argNo;

    public Argument(Type type, String name, Function parent, int argNo) {
        super(type, name);
        this.parent=parent;
        this.argNo=argNo;
    }

    public Argument(Type type, Function parent, int argNo) {
        super(type);
        this.parent=parent;
        this.argNo=argNo;
    }

    public Function getParent() {
        return parent;
    }

    public void setParent(Function parent) {
        this.parent = parent;
    }

    public int getArgNo() {
        return argNo;
    }

    public void setArgNo(int argNo) {
        this.argNo = argNo;
    }
}

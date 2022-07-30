package ir;

import util.CloneMap;

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

    @Override
    public String toString() {
        return getType() + " " + getName();
    }

    @Override
    public Argument copy(CloneMap cloneMap) {
        if(cloneMap.get(this)!=null){
            return (Argument) cloneMap.get(this);
        }
        Argument ret=new Argument(getType(),null,0);
        cloneMap.put(this,ret);
        return ret;
    }
}

package ir;

import java.util.List;

public class Use {
    private Value Val;
    private User U;

    public Value getVal() {
        return Val;
    }

    public void setVal(Value val) {
        Val = val;
    }

    public User getU() {
        return U;
    }

    public void setU(User u) {
        U = u;
    }

    public void addToList(List<Use> List) {
        List.add(this);
    }

    public void set(Value V) {
        if (Val!=null) Val.removeUse(this);
        Val = V;
        if (V!=null) V.addUse(this);
    }
}

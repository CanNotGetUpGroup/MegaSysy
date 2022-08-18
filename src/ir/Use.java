package ir;

import java.util.List;

public class Use {
    private Value Val;
    private User U;
    private int operandNo;//Val在user的operandList中的位置

    public Use(User u,Value v,int operandNo) {
        U = u;
        Val=v;
        this.operandNo=operandNo;
    }

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

    public void setOperandNo(int operandNo) {
        this.operandNo = operandNo;
    }

    public int getOperandNo() {
//        return U.OperandList.indexOf(Val);
        return operandNo;
    }

    public void addToList(List<Use> List) {
        List.add(this);
    }

    public void set(Value V) {
        if (Val!=null) Val.removeUse(this);
        Val = V;
        if (V!=null) V.addUse(this);
    }

    public void swap(Use RHS){

    }

}

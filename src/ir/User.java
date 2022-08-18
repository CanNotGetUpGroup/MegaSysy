package ir;

import java.util.ArrayList;

public abstract class User extends Value {
    ArrayList<Value> OperandList;
    int numOperands;

    public ArrayList<Value> getOperandList() {
        return OperandList;
    }

    public void setOperandList(ArrayList<Value> operandList) {
        OperandList = operandList;
    }

    public int getNumOperands() {
        return numOperands;
    }

    public void setNumOperands(int numOperands) {
        this.numOperands = numOperands;
    }

    public Value getOperand(int i) {
        assert (i < numOperands);
        return OperandList.get(i);
    }

    public void setOperand(int i, Value Val) {
        assert (i < numOperands);
        OperandList.set(i, Val);
        if (Val != null) {
            Val.addUse(new Use(this, Val, i));
        }
    }

    public void addOperand(Value Val) {
        OperandList.add(Val);
        if (Val != null) {
            Val.addUse(new Use(this, Val, OperandList.size() - 1));
        }
        this.numOperands = OperandList.size();
    }

    public void addAllOperand(ArrayList<Value> Val) {
        if (Val != null) {
            for (Value v : Val) {
                OperandList.add(v);
                v.addUse(new Use(this, v, OperandList.size() - 1));
            }
        }
        this.numOperands = OperandList.size();
    }

    public void removeAllOperand() {
        while(!OperandList.isEmpty()){
            removeOperand(0);
        }
        this.numOperands = 0;
    }

    public void removeOperand(int idx) {
        OperandList.get(idx).removeUse(this, idx);
        for (int i = idx + 1; i < OperandList.size(); i++) {
            for (Use use : OperandList.get(i).getUseList()) {
                if (use.getU() == this) {
                    use.setOperandNo(i - 1);
                    break;
                }
            }
        }
        OperandList.remove(idx);
        numOperands = OperandList.size();
    }

    public Value opBegin() {
        if (OperandList.isEmpty())
            return null;
        return OperandList.get(0);
    }

    public Value opEnd() {
        if (OperandList.isEmpty())
            return null;
        return OperandList.get(numOperands - 1);
    }

    public User(Type ty, String name, int numOperands) {
        super(ty, name);
        this.numOperands = numOperands;
        this.OperandList = new ArrayList<>();
    }

    public User(Type ty, Use Ops, int numOperands) {
        super(ty);
    }

    public User(Type ty, String name) {
        super(ty, name);
        OperandList = new ArrayList<>();
        this.numOperands = 0;
    }

    public User(Type type, int numOperands) {
        super(type);
        OperandList = new ArrayList<>();
        this.numOperands = numOperands;
    }

    public User(Type type, ArrayList<Value> operandList) {
        super(type);
        OperandList = new ArrayList<>();
        addAllOperand(operandList);
    }

    /**
     * 作为User被删除时，将其使用的value对应的use删除
     */
    public void dropUsesAsUser() {
        for (Value v : getOperandList()) {
            if (v == null)
                continue;
            v.removeUseByUser(this);
        }
    }

    /**
     * 设置某index的operand为V
     *
     * @param index:位置，operand视作从0开始
     * @param v:想要设置的operand
     **/
    public void CoSetOperand(int index, Value v) {
        assert this.numOperands > index;
        this.OperandList.set(index, v);
        if (v != null) {
            v.addUse(new Use(this, v, index));
        }
    }

    /**
     * 替换某一index的operand为V，维护 operand 的 usesList
     *
     * @param index: 位置，operand视作从0开始
     * @param v:     想要设置的operand
     */
    public void CoReplaceOperandByIndex(int index, Value v) {
        var op = OperandList.get(index);
        this.CoSetOperand(index, v);
        if (op != null && !this.OperandList.contains(op)) {
            op.removeUseByUser(this);
        }
    }

    /**
     * 将operandlist中所有lhs换为rhs，并更新Use
     */
    public void COReplaceOperand(Value lhs, Value rhs) {
        lhs.removeUseByUser(this);
        for (int i = 0; i < OperandList.size(); i++) {
            if (OperandList.get(i) == lhs) {
                CoSetOperand(i, rhs);
            }
        }
    }
}

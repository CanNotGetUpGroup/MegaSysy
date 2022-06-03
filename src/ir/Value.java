package ir;

import java.util.LinkedList;
import java.util.logging.Logger;

public abstract class Value {
    private LinkedList<Use> UseList; //自己在哪些地方被使用
    private Type type;
    private String name;

    public int getUseSize() {
        return UseList.size();
    }

    public LinkedList<Use> getUseList() {
        return UseList;
    }

    public void setUseList(LinkedList<Use> useList) {
        UseList = useList;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * 自动生成名称
     * @param type
     */
    public Value(Type type) {
        this.type = type;
        setName("x"+String.valueOf(MyContext.valuePtr++));
    }

    public Value(Type type, String name) {
        this.type = type;
        setName(name);
    }

    public User userBack() {
        if (UseList.isEmpty()) return null;
        return UseList.peek().getU();
    }

    public void addUse(Use U) {
        if (UseList == null) UseList = new LinkedList<>();
        U.addToList(UseList);
    }

    public void removeUse(Use U) {
        UseList.remove(U);
    }

    public void removeUseByUser(User U) {
        UseList.removeIf(use -> use.getU() == (U));
    }

    /**
     * 将所有引用this的operand转换为引用V，并清除this的UseList，为V的UseList加上对应的Use
     */
    public void replaceAllUsesWith(Value V) {
        for(Use use:UseList){
            use.getU().setOperand(use.getOperandNo(),V);
        }
        UseList.clear();
    }
}

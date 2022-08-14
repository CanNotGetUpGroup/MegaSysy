package ir;

import util.CloneMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.logging.Logger;

public abstract class Value {
    private LinkedList<Use> UseList=new LinkedList<>();//自己在哪些地方被使用,遍历的途中remove掉value可能导致遍历失败，建议先复制一个ArrayList用于遍历
    private Type type;
    private String name;
    private String comment; //注释，用于debug
    private String varName;
    public static HashMap<String,Integer> varVersion=new HashMap<>();

    public int getUseSize() {
        return UseList.size();
    }

    public LinkedList<Use> getUseList() {
        return UseList;
    }

    public ArrayList<User> getUsers(){
        ArrayList<User> ret=new ArrayList<>();
        for(Use u:UseList){
            ret.add(u.getU());
        }
        return ret;
    }

    public void setVarName(String varName) {
        if(varName.length()>100)
            varName = varName.substring(0,100);
        if(varVersion.containsKey(varName)){
            String oldName=varName;
            varName=varName+varVersion.get(varName);
            varVersion.put(oldName,varVersion.get(oldName)+1);
        }else{
            varVersion.put(varName,0);
        }
        this.varName=varName;
    }

    public String getVarName() {
        return varName;
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = "; "+comment;
    }

    /**
     * 自动生成名称
     * @param type
     */
    public Value(Type type) {
        this.type = type;
        setName("");
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

    /**
     * 移除第一个U的Use，并使用OperandNo进行校验
     * (需要User和OperandIdx才能确定Use)
     */
    public void removeUse(User U,int idx) {
        for(Use use:UseList){
            if(use.getU()==U&&use.getOperandNo()==idx){
                UseList.remove(use);
                return;
            }
        }
    }

    /**
     * 移除所有U的Use
     */
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

    /**
     * 删除UseList中的Use，并将User中对应的value设为Undef
     */
    public void dropUse(Use U){
        removeUse(U);
        U.getU().setOperand(U.getOperandNo(),Constants.UndefValue.get(getType()));
    }

    /**
     * 作为Value被删除时，删除所有use，将所有User中对应的value设为Undef
     */
    public void dropUsesAsValue(){
        for(Use U:new ArrayList<>(getUseList())){
            dropUse(U);
        }
    }

    public void remove(){

    }

    public abstract Value copy(CloneMap cloneMap);
}

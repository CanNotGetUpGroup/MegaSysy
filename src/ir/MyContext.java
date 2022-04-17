package ir;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 前端程序运行过程中的上下文，记录了当前作用域下的符号表
 */
public class MyContext {
    private static final MyContext myContext = new MyContext();
    private ArrayList<HashMap<String,Value>> ValSymTab;
    private HashMap<String,Value> GlobalValSymTab;
    private BasicBlock curBB;
    private Function curFunc;

    public static MyContext getInstance(){
        return myContext;
    }

    private MyContext() {
        ValSymTab = new ArrayList<>();
        GlobalValSymTab=new HashMap<>();
    }

    public HashMap<String, Value> getCurTable(){
        return ValSymTab.get(ValSymTab.size()-1);
    }

    public ArrayList<HashMap<String, Value>> getValSymTab() {
        return ValSymTab;
    }

    public BasicBlock getCurBB() {
        return curBB;
    }

    public void setCurBB(BasicBlock curBB) {
        this.curBB = curBB;
    }

    public Function getCurFunc() {
        return curFunc;
    }

    public void setCurFunc(Function curFunc) {
        this.curFunc = curFunc;
    }

    public HashMap<String, Value> getGlobalValSymTab() {
        return GlobalValSymTab;
    }

    public Value lookup(String name){
        for(int i=ValSymTab.size()-1;i>=0;i--){
            if(ValSymTab.get(i).containsKey(name)){
                return ValSymTab.get(i).get(name);
            }
        }
        return GlobalValSymTab.getOrDefault(name, null);
    }

    public Function getFunction(String name){
        Value f=lookup(name);
        if(f instanceof Function){
            return (Function) f;
        }else{
            return null;
        }
    }

    public boolean inGlobalArea(){
        return ValSymTab.size()==0;
    }
}

package util;

import ir.BasicBlock;
import ir.Function;
import ir.GlobalVariable;
import ir.Value;

import javax.naming.Name;
import java.util.ArrayList;
import java.util.HashMap;

public class SymbolTable {
    private static final SymbolTable symbolTable = new SymbolTable();

    private final ArrayList<HashMap<String, Value>> ValSymTab; //局部变量符号表
    private final HashMap<String,Value> GlobalValSymTab;  //全局变量符号表
    private final HashMap<Value,String> NameValTab;   //查找Value的name
    private BasicBlock curBB;
    private Function curFunc;
    private SymbolTable() {
        ValSymTab = new ArrayList<>();
        GlobalValSymTab=new HashMap<>();
        NameValTab=new HashMap<>();
    }

    public HashMap<String, Value> getCurTable(){
        if(inGlobalArea()) return GlobalValSymTab;
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

    /**
     * 根据变量名查找变量
     * @param name 变量名
     * @return 找到的变量，若没找到返回null
     */
    public Value lookup(String name){
        for(int i=ValSymTab.size()-1;i>=0;i--){
            if(ValSymTab.get(i).containsKey(name)){
                return ValSymTab.get(i).get(name);
            }
        }
        return GlobalValSymTab.getOrDefault(name, null);
    }

    /**
     * 根据变量名称找函数
     * @param name 变量名
     * @return 返回函数
     */
    public Function getFunction(String name){
        Value f=GlobalValSymTab.getOrDefault(name, null);
        if(f instanceof Function){
            return (Function) f;
        }else{
            return null;
        }
    }

    public String getName(Value val){
        return NameValTab.getOrDefault(val, null);
    }

    public boolean inGlobalArea(){
        return ValSymTab.size()==0;
    }

    public void addValue(String name, Value val){
        if (name != null) {
            Value find = lookup(name);
            assert find == null || (!inGlobalArea() && (find instanceof GlobalVariable || find instanceof Function));
        }
        getCurTable().put(name,val);
        NameValTab.put(val,name);
    }

    public void deleteValue(Value val){
        getCurTable().remove(NameValTab.remove(val));
    }

    public void addLevel(){
        ValSymTab.add(new HashMap<>());
    }

    public void removeTop(){
        if(ValSymTab.isEmpty()) return;
        ValSymTab.remove(ValSymTab.size()-1);
    }

    public static SymbolTable getInstance(){
        return symbolTable;
    }
}

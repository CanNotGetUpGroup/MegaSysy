package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import ir.Constants.*;
import ir.DerivedTypes.*;

public class MyContext {
    private static final MyContext myContext = new MyContext();
    private ArrayList<HashMap<String,Value>> ValSymTab; //局部变量符号表
    private HashMap<String,Value> GlobalValSymTab;  //全局变量符号表
    private BasicBlock curBB;
    private Function curFunc;

    /**
     * types instance
     */
    public static final Type FloatTy=new Type(Type.TypeID.FloatTyID);
    public static final IntegerType IntegerTy=new DerivedTypes.IntegerType();
    public static final Type VoidTy=new Type(Type.TypeID.VoidTyID);

    /**
     * 常量储存器
     */
    public HashMap<Integer, ConstantInt> IntConstants=new HashMap<>();
    public HashMap<Float, ConstantFP> FPConstants=new HashMap<>();

    /**
     * value命名序号
     */
    public static int valuePtr=0;

    public static MyContext getInstance(){
        return myContext;
    }

    private MyContext() {
        ValSymTab = new ArrayList<>();
        GlobalValSymTab=new HashMap<>();
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

    public boolean inGlobalArea(){
        return ValSymTab.size()==0;
    }

    public void addValue(String name, Value val){
        if (name != null) {
            MyContext context = MyContext.getInstance();
            Value find = context.lookup(name);
            assert find == null || (!context.inGlobalArea() && (find instanceof GlobalVariable || find instanceof Function));
        }
        getCurTable().put(name,val);
    }

    public void addLevel(){
        ValSymTab.add(new HashMap<>());
    }

    public void removeTop(){
        if(ValSymTab.isEmpty()) return;
        ValSymTab.remove(ValSymTab.size()-1);
    }
}

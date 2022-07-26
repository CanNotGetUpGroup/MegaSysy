package ir;

import util.CloneMap;

public class GlobalVariable extends User {
    private Module parent;
    private boolean isConstantGlobal; // Is this a global constant?

    public Module getParent() {
        return parent;
    }

    public void setParent(Module parent) {
        this.parent = parent;
    }

    public boolean isConstantGlobal() {
        return isConstantGlobal;
    }

    public void setConstantGlobal(boolean constantGlobal) {
        isConstantGlobal = constantGlobal;
    }

    /**
     * 创建一个未初始化的全局变量
     * @param ty
     * @param parent
     * @param isConstantGlobal
     * @return
     */
    public static GlobalVariable create(Type ty, Module parent, boolean isConstantGlobal){
        return new GlobalVariable("",ty, parent,null, isConstantGlobal);
    }

    /**
     * 创建一个初始化的全局变量
     * @param ty
     * @param parent
     * @param InitVal
     * @param isConstantGlobal
     * @return
     */
    public static GlobalVariable create(String name,Type ty, Module parent,Constant InitVal, boolean isConstantGlobal){
        return new GlobalVariable(name,ty, parent,InitVal, isConstantGlobal);
    }

    public GlobalVariable(Type ty, Module parent) {
        super(ty, 0);
        this.parent = parent;
    }

    /**
     * 插入在Moudle最后
     */
    public GlobalVariable(String name,Type ty, Module parent,Constant InitVal, boolean isConstantGlobal) {
        super(DerivedTypes.PointerType.get(ty),name, 0);
        this.parent = parent;
        this.isConstantGlobal = isConstantGlobal;
        if(InitVal!=null){
            assert InitVal.getType().equals(ty);
            addOperand(InitVal);
        }else {
            addOperand(Constant.getNullValue(ty));
        }
        parent.getGlobalVariables().add(this);
    }

    /**
     *
     * @param ty 类型
     * @param numOps 参数数量
     * @param parent 属于的Module
     * @param isConstantGlobal 是否是constant
     * @param insertBefore  插入在该指令前
     */
    public GlobalVariable(Type ty, int numOps, Module parent, boolean isConstantGlobal, GlobalVariable insertBefore) {
        super(DerivedTypes.PointerType.get(ty), numOps);
        this.parent = parent;
        this.isConstantGlobal = isConstantGlobal;
        //插入在该指令前
        int idx=parent.getGlobalVariables().indexOf(insertBefore);
        parent.getGlobalVariables().add(idx,this);
    }

    @Override
    public String toString() {
        if(isConstantGlobal) return getName()+" = dso_local constant "+getOperand(0);
        return getName()+" = dso_local global "+getOperand(0);
    }

    @Override
    public Value copy(CloneMap cloneMap) {
        return this;
    }
}

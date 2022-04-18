package ir;

public class GlobalVariable extends Constant {
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

    public GlobalVariable(Type ty, int numOps, Module parent) {
        super(ty, numOps);
        this.parent = parent;
    }

    /**
     * 插入在Moudle最后
     */
    public GlobalVariable(Type ty, int numOps, Module parent, boolean isConstantGlobal) {
        super(ty, numOps);
        this.parent = parent;
        this.isConstantGlobal = isConstantGlobal;
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
        super(ty, numOps);
        this.parent = parent;
        this.isConstantGlobal = isConstantGlobal;
    }
}

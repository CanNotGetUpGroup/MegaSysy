package ir;

import util.IListNode;

public abstract class Instruction extends User {
    private BasicBlock Parent;
    private IListNode<Instruction,BasicBlock> instNode;

    public BasicBlock getParent() {
        return Parent;
    }

    public void setParent(BasicBlock parent) {
        Parent = parent;
    }

    public IListNode<Instruction, BasicBlock> getInstNode() {
        return instNode;
    }

    public void setInstNode(IListNode<Instruction, BasicBlock> instNode) {
        this.instNode = instNode;
    }

    public Instruction(Type type, String name , int numOperands) {
        super(type, name, numOperands);
        instNode = new IListNode<>(this);
    }

    /**
     * @param InsertBefore 插入在这个指令之前
     */
    public Instruction(Type type, int numOperands, Instruction InsertBefore ) {
        super(type, numOperands);
        //TODO: 插入在这个指令之前
    }

    /**
     *
     * @param type 类型
     * @param numOperands 参数数量
     * @param InsertAtEnd 插入在这个基本块的最后
     */
    public Instruction(Type type, int numOperands, BasicBlock InsertAtEnd ) {
        super(type, numOperands);
        //TODO: 插入在这个基本块的最后
    }
}

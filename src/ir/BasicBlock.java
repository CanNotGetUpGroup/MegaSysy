package ir;

import util.IList;
import util.IListNode;

public class BasicBlock extends Value {
    private Function Parent;
    private IList<Instruction, BasicBlock> instList;
    private IListNode<BasicBlock, Function> bbNode;


    public BasicBlock(Type type, Function parent) {
        super(type);
        Parent = parent;
        //TODO: 插入到parent末尾
    }

    public BasicBlock(Type type, String name, Function parent) {
        super(type, name);
        Parent = parent;
        //TODO: 插入到parent末尾
    }

    public Function getParent() {
        return Parent;
    }

    public void setParent(Function parent) {
        Parent = parent;
    }

    public IList<Instruction, BasicBlock> getInstList() {
        return instList;
    }

    public void setInstList(IList<Instruction, BasicBlock> instList) {
        this.instList = instList;
    }

    public IListNode<BasicBlock, Function> getBbNode() {
        return bbNode;
    }

    public void setBbNode(IListNode<BasicBlock, Function> bbNode) {
        this.bbNode = bbNode;
    }
}

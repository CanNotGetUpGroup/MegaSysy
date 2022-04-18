package ir;

import util.IList;
import util.IListNode;

public class BasicBlock extends Value {
    private Function Parent;
    private IListNode<BasicBlock, Function> bbNode;
    private IList<Instruction, BasicBlock> instList;


    public BasicBlock(Type type, Function parent) {
        super(type);
        Parent = parent;
        bbNode = new IListNode<>(this);
        instList = new IList<>(this);
        //插入到parent末尾
        bbNode.insertIntoListEnd(Parent.getBbList());
    }

    public BasicBlock(Type type, String name, Function parent) {
        super(type, name);
        Parent = parent;
        bbNode = new IListNode<>(this);
        instList = new IList<>(this);
        //插入到parent末尾
        bbNode.insertIntoListEnd(Parent.getBbList());
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

package ir;

import util.IList;
import util.IListNode;

public class BasicBlock extends Value {
    private Function Parent;
    private IListNode<BasicBlock, Function> bbNode;
    private IList<Instruction, BasicBlock> instList;

    /**
     * 生成基本块对象
     * @param parent
     * @return
     */
    public static BasicBlock create(Function parent){
        return new BasicBlock(parent);
    }

    public static BasicBlock create(String name,Function parent){
        return new BasicBlock(name,parent);
    }

    public BasicBlock(Function parent) {
        super(Type.getLabelTy());
        Parent = parent;
        bbNode = new IListNode<>(this,parent.getBbList());
        instList = new IList<>(this);
        //插入到parent末尾
        bbNode.insertIntoListEnd(Parent.getBbList());
    }

    public BasicBlock(String name, Function parent) {
        super(Type.getLabelTy(), name);
        Parent = parent;
        bbNode = new IListNode<>(this,parent.getBbList());
        instList = new IList<>(this);
        //插入到parent末尾
        bbNode.insertIntoListEnd(Parent.getBbList());
    }

    @Override
    public String toString() {
        return getName()+":";
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

    //从函数中删除
    public void remove(){
        bbNode.remove();
    }
}

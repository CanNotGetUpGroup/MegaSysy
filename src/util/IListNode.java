package util;

import ir.*;
import ir.Module;

public class IListNode<T, P> {
    private IListNode<T, P> Prev;
    private IListNode<T, P> Next;
    private T Val;

    public IListNode(T val) {
        Val = val;
        Prev=Next=null;
    }

    public IListNode() {
        Prev=Next=null;
    }

    public IListNode<T, P> getPrev() {
        return Prev;
    }

    public void setPrev(IListNode<T, P> prev) {
        Prev = prev;
    }

    public IListNode<T, P> getNext() {
        return Next;
    }

    public void setNext(IListNode<T, P> next) {
        Next = next;
    }

    public T getVal() {
        return Val;
    }

    public void setVal(T val) {
        Val = val;
    }

    public IList<T, P> getParent() {
        if(Val instanceof Instruction){
            BasicBlock p=((Instruction) Val).getParent();
            return (IList<T, P>) p.getInstList();
        }else if(Val instanceof BasicBlock){
            Function p=((BasicBlock) Val).getParent();
            return (IList<T, P>) p.getBbList();
        }else if(Val instanceof Function){
            Module p=((Function) Val).getParent();
            return (IList<T, P>) p.getFuncList();
        }
        return null;
    }

    public void remove(){
        assert !isBorder();
        IList<T,P> p=getParent();
        p.setList_size(p.getList_size()-1);
        //TODO
        IListNode<T,P> tmp=this.Prev;
        this.Prev=null;
        tmp.setNext(this.Next);
        this.Next.setPrev(tmp);
        this.Next=null;
    }

    public void insertBefore(IListNode<T,P> node){
        if(node==null) return;
        IListNode<T,P> tmp=this.Prev;
        this.Prev=node;
        node.setNext(this);
        tmp.setNext(node);
        node.setPrev(tmp);
    }

    public void insertIntoListEnd(IList<T,P> list){
        if(list==null) return;
        insertBefore(list.getTail());
    }

    public boolean isBorder(){
        return Prev==null||Next==null;
    }

}

package util;

import ir.*;

public class IListNode<T, P> {
    private IListNode<T, P> Prev;
    private IListNode<T, P> Next;
    private T Val;

    public IListNode(T val) {
        Val = val;
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
        IList<T,P> p=getParent();
        p.setList_size(p.getList_size()-1);
        //TODO
    }

}

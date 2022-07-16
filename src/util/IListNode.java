package util;

import ir.*;
import ir.Module;

import java.util.Iterator;

public class IListNode<T, P> {
    private IListNode<T, P> Prev;
    private IListNode<T, P> Next;
    private T Val;
    private IList<T,P> Parent;

    public IListNode(T val,IList<T,P> parent) {
        Val = val;
        Parent=parent;
        Prev=Next=null;
    }

    public IListNode() {
        Prev=Next=null;
    }

    public IListNode<T, P> getPrev() {
        return Prev.equals(getParent().getHead())?null:Prev;
    }

    public void setPrev(IListNode<T, P> prev) {
        Prev = prev;
    }

    public IListNode<T, P> getNext() {
        return Next.equals(getParent().getTail())?null:Next;
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
        return Parent;
    }

    public void setParent(IList<T, P> parent) {
        Parent = parent;
    }

    public int index(){
        int ret=0;
        for(T e:getParent()){
            if(e.equals(getVal())){
                return ret;
            }
            ret++;
        }
        return -1;
    }

    public void remove(){
        assert !isBorder();
        IList<T,P> p=getParent();
        p.setList_size(p.getList_size()-1);
        IListNode<T,P> tmp=this.Prev;
        this.Prev=null;
        tmp.setNext(this.Next);
        this.Next.setPrev(tmp);
        this.Next=null;
    }

    /**
     * 把当前节点插入到node之前
     * @param node
     */
    public void insertBefore(IListNode<T,P> node){
        if(node==null||this.equals(node.getPrev())) return;
        IListNode<T,P> tmp=node.Prev;
        node.Prev=this;
        this.setNext(node);
        tmp.setNext(this);
        this.setPrev(tmp);
        IList<T,P> p=getParent();
        p.setList_size(p.getList_size()+1);
    }

    /**
     * 把当前节点插入到node之后
     * @param node
     */
    public void insertAfter(IListNode<T,P> node){
        if(node==null||this.equals(node.getNext())) return;
        IListNode<T,P> tmp=node.Next;
        node.Next=this;
        this.setPrev(node);
        tmp.setPrev(this);
        this.setNext(tmp);
        IList<T,P> p=getParent();
        p.setList_size(p.getList_size()+1);
    }

    /**
     * 把当前节点插入到list最后
     * @param list
     */
    public void insertIntoListEnd(IList<T,P> list){
        if(list==null) return;
        insertBefore(list.getTail());
    }

    /**
     * 删除后继结点
     */
    public void cutFollow(Iterator<T> itr){
        while(itr.hasNext()){
            itr.next();
            itr.remove();
        }
    }

    public boolean isBorder(){
        return Prev==null||Next==null;
    }

}

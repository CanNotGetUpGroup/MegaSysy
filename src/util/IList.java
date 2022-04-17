package util;

import ir.Value;

public class IList<T, P> {
    private IListNode<T, P> head;
    private IListNode<T, P> tail;
    private P Val;
    private int list_size;

    public IList(P val) {
        Val = val;
        list_size=0;
    }

    public IListNode<T, P> getHead() {
        return head;
    }

    public void setHead(IListNode<T, P> head) {
        this.head = head;
    }

    public IListNode<T, P> getTail() {
        return tail;
    }

    public void setTail(IListNode<T, P> tail) {
        this.tail = tail;
    }

    public P getVal() {
        return Val;
    }

    public void setVal(P val) {
        Val = val;
    }

    public int getList_size() {
        return list_size;
    }

    public void setList_size(int list_size) {
        this.list_size = list_size;
    }
}

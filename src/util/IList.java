package util;

import ir.BasicBlock;
import ir.Instruction;

import java.util.*;

/**
 * 遍历方法 for(T t:IList){}
 *
 * @param <T>
 * @param <P>
 */
public class IList<T, P> implements Iterable<T> {
    private IListNode<T, P> head = new IListNode<>();
    private IListNode<T, P> tail = new IListNode<>();
    private P Val;
    private int list_size;
    private IListIterator<T,P> end = new ListItr(null);

    public IList(P val) {
        Val = val;
        list_size = 0;
        head.setNext(tail);
        tail.setPrev(head);
        head.setParent(this);
        tail.setParent(this);
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

    public IListNode<T, P> getFirst() {
        return head.getNext();
    }

    public IListNode<T, P> getLast() {
        return tail.getPrev();
    }

    public void insertAtHead(IListNode<T, P> e) {
        e.insertAfter(head);
    }

    public void insertBefore(IListNode<T, P> e, IListNode<T, P> succ) {
        e.insertBefore(succ);
    }

    public void pushBack(IListNode<T, P> e) {
        e.insertBefore(tail);
    }

    public void mergeList(IList<T, P> follow) {
        IListNode<T, P> last=getLast();
        IListNode<T, P> followFirst=follow.getFirst();
        if(followFirst==null){
            return;
        }
        tail = follow.getTail();
        if(last==null){
            head=follow.getHead();
            return;
        }

        last.setNext(followFirst);
        followFirst.setPrev(last);
        followFirst.setParent(this);
        while(followFirst.getNext()!=null){
            followFirst=followFirst.getNext();
            followFirst.setParent(this);
        }
    }

    /**
     * 在insertHead前插入insertList
     */
    public void splice(IListIterator<T,P> insertHead,IList<T,P> insertList){
        IListNode<T, P> First=insertList.getFirst();
        while(First!=null){
            insertHead.add(First);
            First=First.getNext();
        }
    }

    public boolean isEmpty(){
        return getFirst()==null;
    }

    public int indexOf(T e) {
        int ret = 0;
        for (T t : this) {
            if (t.equals(e)) {
                return ret;
            }
            ret++;
        }
        return -1;
    }

    IListNode<T, P> node(int index) {
        // assert isElementIndex(index);

        if (index < (list_size >> 1)) {
            IListNode<T, P> x = getFirst();
            for (int i = 0; i < index; i++)
                x = x.getNext();
            return x;
        } else {
            IListNode<T, P> x = getLast();
            for (int i = list_size - 1; i > index; i--)
                x = x.getPrev();
            return x;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new ListItr(head.getNext());
    }

    public IListIterator<T,P> iterator(T t) {
        IListNode<T, P> H = head.getNext(), T = tail.getPrev();
        while ((H != T)) {
            if (H.getVal() == t) {
                return new ListItr(H);
            }
            if (T.getVal() == t) {
                return new ListItr(T);
            }
            H = H.getNext();
            if (H == T) break;
            T = T.getPrev();
        }
        return H.getVal() == t ? new ListItr(H) : end;
    }

    public Iterator<T> end(){
        return end;
    }

    private class ListItr implements IListIterator<T,P> {
        private IListNode<T, P> lastReturned;
        private IListNode<T, P> nodePtr;

        ListItr(IListNode<T, P> N) {
            // assert isPositionIndex(index);
            nodePtr = N;
        }

        public boolean hasNext() {
            return nodePtr != null;
        }

        public T next() {
            if (!hasNext())
                return null;

            lastReturned = nodePtr;
            nodePtr = nodePtr.getNext();
            return lastReturned.getVal();
        }

        public boolean hasPrevious() {
            return (nodePtr == null) ? tail.getPrev() != null : nodePtr.getPrev() != null;
        }

        public T previous() {
            if (!hasPrevious())
                return null;

            lastReturned = nodePtr = (nodePtr == null) ? tail.getPrev() : nodePtr.getPrev();
            return lastReturned.getVal();
        }

        public void remove() {
            if (lastReturned == null)
                throw new IllegalStateException();

            IListNode<T, P> lastNext = lastReturned.getNext();
            if(lastReturned.getVal() instanceof BasicBlock){
                ((BasicBlock)lastReturned.getVal()).remove();
            }else if(lastReturned.getVal() instanceof Instruction){
                ((Instruction) lastReturned.getVal()).remove();
            }

            lastReturned.remove();
            if (nodePtr == lastReturned)
                nodePtr = lastNext;
            lastReturned = null;
        }

        public void set(T e) {
            if (lastReturned == null)
                throw new IllegalStateException();
            lastReturned.setVal(e);
        }

        public void add(IListNode<T, P> e) {
            lastReturned = null;
            if (nodePtr == null)
                pushBack(e);
            else
                insertBefore(e, nodePtr);
        }
    }
}

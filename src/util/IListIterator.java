package util;

import java.util.Iterator;
import java.util.ListIterator;

public interface IListIterator<T> extends Iterator<T> {
    /**
     * 有后继节点
     */
    public boolean hasNext();

    /**
     * 返回当前节点，指向后继节点
     */
    public T next();

    /**
     * 有前驱节点
     */
    public boolean hasPrevious();

    /**
     * 指向前驱节点，返回该节点
     */
    public T previous();

    /**
     * 移除当前节点，指向后继节点
     */
    public void remove();

    public void set(T e);
    public void add(T e);
}

package util;

import java.util.Iterator;
import java.util.ListIterator;

public interface IListIterator<T> extends Iterator<T> {
    /**
     * 有后继节点
     */
    boolean hasNext();

    /**
     * 返回当前节点，指向后继节点
     */
    T next();

    /**
     * 有前驱节点
     */
    boolean hasPrevious();

    /**
     * 指向前驱节点，返回该节点
     */
    T previous();

    /**
     * 移除当前节点，指向后继节点
     */
    void remove();

    void set(T e);
    void add(T e);
}

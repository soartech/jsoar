/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2008
 */
package org.jsoar.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author ray
 */
public class ListHead <T> implements Iterable<T>
{
    public AsListItem<T> first;

    public ListHead()
    {
    }
    
    public ListHead(ListHead<T> other)
    {
        this.first = other.first;
    }
    
    public boolean isEmpty()
    {
        return first == null;
    }
    
    public T getFirstItem()
    {
        return isEmpty() ? first.get() : null;
    }
    
    public int size()
    {
        return first != null ? first.count() : 0;
    }
    
    public List<T> toList()
    {
        List<T> r = new ArrayList<T>();
        for(T member : this)
        {
            r.add(member);
        }
        return r;
    }
    
    public static <T> ListHead<T> fromCollection(Collection<T> collection)
    {
        ListHead<T> head = new ListHead<T>();
        
        AsListItem<T> previous = null;
        for(T item : collection)
        {
            AsListItem<T> member = new AsListItem<T>(item);
            member.insertAfter(head, previous);
            previous = member;
        }
        return head;
    }

    /* (non-Javadoc)
     * @see java.lang.Iterable#iterator()
     */
    @Override
    public Iterator<T> iterator()
    {
        return first != null ? first.iterator() : new Iterator<T>() {

            @Override
            public boolean hasNext()
            {
                return false;
            }

            @Override
            public T next()
            {
                return null;
            }

            @Override
            public void remove()
            {
                throw new UnsupportedOperationException();
            }};
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        // Display like a Java collection
        return toList().toString();
    }
    
    
    
}

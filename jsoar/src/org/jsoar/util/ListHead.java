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
 * Implements the "head" of a doubly-linked list of items.
 * 
 * @author ray
 */
public class ListHead <T> implements Iterable<T>
{
    /**
     * The first item in the list
     */
    public AsListItem<T> first;

    /**
     * Construct a new, empty instance. This function is provided rather than
     * a constructor to simplify initialization with generic parameters. 
     * 
     * @param <T> The type of object store in the list
     * @return New list head
     */
    public static <T> ListHead<T> newInstance()
    {
        return new ListHead<T>();
    }
    
    /**
     * Construct a new list head pointing at the same list as other. This is
     * a shallow copy of the list.
     * 
     * @param <T> The type of object stored in the list
     * @param other The list head to copy
     * @return New list head
     */
    public static <T> ListHead<T> newInstance(ListHead<T> other)
    {
        return new ListHead<T>(other);
    }
    
    private ListHead()
    {
    }
    
    private ListHead(ListHead<T> other)
    {
        this.first = other.first;
    }
    
    /**
     * @return True if this list is empty
     */
    public boolean isEmpty()
    {
        return first == null;
    }
    
    /**
     * Make this list empty. Note that if other list heads are pointing at the
     * same list, they will not be cleared. This effectively sets the 
     * {@link #first} attribute to <code>null</code> 
     */
    public void clear()
    {
        first = null;
    }
    
    /**
     * @return The first item in the list, or null if the list is empty
     */
    public T getFirstItem()
    {
        return !isEmpty() ? first.get() : null;
    }
    
    /**
     * @return The size of this list. Note that this function is <b>linear</b>
     *      in the size of the list.
     */
    public int size()
    {
        return first != null ? first.count() : 0;
    }
    
    /**
     * Find the list item that contains the given value
     * 
     * @param value The value to search for
     * @return The containing list item, or null if not found
     */
    public AsListItem<T> find(T value)
    {
        return first != null ? first.find(value) : null;
    }
    
    /**
     * Check whether this list contains the given value.
     * 
     * @param value The value to search for
     * @return true if the list contains an item with the given value
     */
    public boolean contains(T value)
    {
        return find(value) != null;
    }
    
    /**
     * Create a Java List from this list. The returned list is a copy.
     * 
     * @return A list containing the items in this list.
     */
    public List<T> toList()
    {
        List<T> r = new ArrayList<T>();
        for(T member : this)
        {
            r.add(member);
        }
        return r;
    }
    
    /**
     * Construct a new list from the elements in the given collection
     * 
     * @param <T> The type of elements in the list
     * @param collection The collection
     * @return New list head pointing at a list of elements from the given
     *      collection. Elements are not copied.
     */
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

    /*package*/ boolean containsAsListItem(AsListItem<T> item)
    {
        return first != null ? first.containsAsListItem(item) : false;
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

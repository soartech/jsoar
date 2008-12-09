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
public final class ListHead <T> implements Iterable<T>
{
    /**
     * The first item in the list
     */
    public ListItem<T> first;

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
        return !isEmpty() ? first.item : null;
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
    public ListItem<T> find(Object value)
    {
        return first != null ? first.find(value) : null;
    }
    
    /**
     * Check whether this list contains the given value.
     * 
     * @param value The value to search for
     * @return true if the list contains an item with the given value
     */
    public boolean contains(Object value)
    {
        return find(value) != null;
    }
    
    /**
     * Push the given value onto the front of this list header.
     * 
     * @param value The value to add
     * @return The resulting list item
     */
    public ListItem<T> push(T value)
    {
        ListItem<T> item = new ListItem<T>(value);
        item.insertAtHead(this);
        return item;
    }
    
    /**
     * Remove the first item in this list and return it
     * 
     * @return The removed item, or <code>null</code> if the list is empty
     */
    public T pop()
    {
        T result = null;
        if(first != null)
        {
            result = first.item;
            first.remove(this);
        }
        return result;
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
        
        ListItem<T> previous = null;
        for(T item : collection)
        {
            ListItem<T> member = new ListItem<T>(item);
            member.insertAfter(head, previous);
            previous = member;
        }
        return head;
    }

    /*package*/ boolean containsAsListItem(ListItem<T> item)
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
    /*
    public static void main(String args[])
    {
        long start = System.currentTimeMillis();
        int total = 0;
        for(int i = 0; i < 10000000; ++i)
        {
            //ListHead<Integer> list = ListHead.newInstance();
            ArrayList<Integer> list = new ArrayList<Integer>();
            //LinkedList<Integer> list = new LinkedList<Integer>();
            for(int j = 0; j < 20; ++j)
            {
                //list.push(Integer.valueOf(j));
                list.add(Integer.valueOf(j));
            }
//            for(Integer j : list)
//            {
//                total += j.intValue();
//            }
//            for(int j = 0; j < list.size(); ++j)
//            {
//                total += list.get(j).intValue();
//            }
//            for(AsListItem<Integer> j = list.first; j != null; j = j.next)
//            {
//                total += j.item.intValue();
//            }
            
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);
        System.out.println(total);
        
        // LinkedList - 9.922s
        // LinkedList/Indexes - 10.203s
        // ArrayList - 15.375s
        // ArrayList/Indexes - 8.313
        // ListHead - 5.093s
        // ListHead/Iterator - 8.656s
    }
    */
}

/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 15, 2008
 */
package org.jsoar.kernel.memory;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 * 
 * An iterator that knows how to iterate over all the weirdness of working
 * memory, i.e. slots, impasse_wmes, input_wmes, etc, etc.
 * 
 * @author ray
 */
public class WmeIterator implements Iterator<Wme>
{
    private WmeImpl next;
    
    /**
     * @param next
     */
    public WmeIterator(WmeImpl next)
    {
        this.next = next;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext()
    {
        return next != null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#next()
     */
    @Override
    public Wme next()
    {
        WmeImpl temp = next;
        if(next != null)
        {
            next = next.next;
        }
        else
        {
            throw new NoSuchElementException();
        }
        return temp;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.util.Iterator#remove()
     */
    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
    
}

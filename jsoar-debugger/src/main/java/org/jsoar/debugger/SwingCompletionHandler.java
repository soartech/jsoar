/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 21, 2009
 */
package org.jsoar.debugger;

import javax.swing.SwingUtilities;

import org.jsoar.runtime.CompletionHandler;

/**
 * Wrap a completion handler in logic to ensure that it executes on the 
 * Swing event thread. Do not interact with the Soar agent in this unless
 * rewrapping in a {@code ThreadedAgent.execute}
 * 
 * @author ray
 */
public class SwingCompletionHandler<T> implements CompletionHandler<T>
{
    private final CompletionHandler<T> inner;
    
    public static <V> CompletionHandler<V> newInstance(CompletionHandler<V> inner) 
    {
        return new SwingCompletionHandler<V>(inner);
    }
    
    private SwingCompletionHandler(CompletionHandler<T> inner)
    {
        this.inner = inner;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.runtime.Result#finish(java.lang.Object)
     */
    @Override
    public void finish(final T result)
    {
        if(SwingUtilities.isEventDispatchThread())
        {
            inner.finish(result);
        }
        else
        {
            SwingUtilities.invokeLater(() -> inner.finish(result));
        }
    }

}

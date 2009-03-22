/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 21, 2009
 */
package org.jsoar.runtime;

import javax.swing.SwingUtilities;

/**
 * @author ray
 * @param <T>
 */
public class SwingCompletion<T> implements Completer<T>
{
    private final Completer<T> inner;
    
    public static <V> Completer<V> newInstance(Completer<V> inner) 
    {
        return new SwingCompletion<V>(inner);
    }
    
    private SwingCompletion(Completer<T> inner)
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
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run()
                {
                    inner.finish(result);
                }});
        }
    }

}

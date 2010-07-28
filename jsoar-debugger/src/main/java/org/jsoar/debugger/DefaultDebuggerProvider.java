/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 25, 2009
 */
package org.jsoar.debugger;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.DebuggerProvider;
import org.jsoar.kernel.SoarException;
import org.jsoar.runtime.ThreadedAgent;

/**
 * Implementation of {@link DebuggerProvider} interface that opens an instance
 * of {@link JSoarDebugger}.
 * 
 * @see JSoarDebugger#newDebuggerProvider()
 * 
 * @author ray
 */
public class DefaultDebuggerProvider implements DebuggerProvider
{
    private static final Log logger = LogFactory.getLog(DefaultDebuggerProvider.class);
    
    private final Map<String, Object> properties = new HashMap<String, Object>();

    public DefaultDebuggerProvider()
    {
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.DebuggerProvider#getProperties()
     */
    @Override
    public synchronized Map<String, Object> getProperties()
    {
        return new HashMap<String, Object>(properties);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.DebuggerProvider#setProperties(java.util.Map)
     */
    @Override
    public synchronized void setProperties(Map<String, Object> props)
    {
        properties.putAll(props);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.DebuggerProvider#openDebugger(org.jsoar.kernel.Agent)
     */
    @Override
    public void openDebugger(final Agent agent) throws SoarException
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(getRunnable(agent)); 
        }
        else
        {        
            doIt(agent);
        }
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.DebuggerProvider#openDebuggerAndWait(org.jsoar.kernel.Agent)
     */
    @Override
    public void openDebuggerAndWait(Agent agent) throws SoarException, InterruptedException
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            try
            {
                SwingUtilities.invokeAndWait(getRunnable(agent));
            }
            catch (InvocationTargetException e)
            {
                final Throwable cause = e.getCause();
                if(cause instanceof SoarException)
                {
                    throw (SoarException) cause;
                }
                else
                {
                    throw new SoarException(e);
                }
            } 
        }
        else
        {        
            doIt(agent);
        }
    }
    
    private Runnable getRunnable(final Agent agent)
    {
        return new Runnable() { 
            public void run() 
            {
                try
                {
                    doIt(agent);
                }
                catch (SoarException e)
                {
                    logger.error("Failed to open new debugger: " + e.getMessage(), e);
                }
            } 
        };
    }

    private void doIt(Agent agent) throws SoarException
    {
        final ThreadedAgent ta = ThreadedAgent.find(agent);
        if(ta == null)
        {
            throw new SoarException("Can't attach debugger to agent with no ThreadedAgent proxy");
        }
        JSoarDebugger.attach(ta, getProperties());
    }
}

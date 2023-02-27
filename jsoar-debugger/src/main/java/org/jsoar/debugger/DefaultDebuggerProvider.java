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

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.DebuggerProvider;
import org.jsoar.kernel.SoarException;
import org.jsoar.runtime.ThreadedAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(DefaultDebuggerProvider.class);
    
    private final Map<String, Object> properties = new HashMap<>();
    
    public DefaultDebuggerProvider()
    {
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.DebuggerProvider#getProperties()
     */
    @Override
    public synchronized Map<String, Object> getProperties()
    {
        return new HashMap<>(properties);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.DebuggerProvider#setProperties(java.util.Map)
     */
    @Override
    public synchronized void setProperties(Map<String, Object> props)
    {
        properties.putAll(props);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.DebuggerProvider#openDebugger(org.jsoar.kernel.Agent)
     */
    @Override
    public void openDebugger(final Agent agent) throws SoarException
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(getOpenDebuggerRunnable(agent));
        }
        else
        {
            doOpenDebugger(agent);
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.DebuggerProvider#openDebuggerAndWait(org.jsoar.kernel.Agent)
     */
    @Override
    public void openDebuggerAndWait(Agent agent) throws SoarException, InterruptedException
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            try
            {
                SwingUtilities.invokeAndWait(getOpenDebuggerRunnable(agent));
            }
            catch(InvocationTargetException e)
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
            doOpenDebugger(agent);
        }
    }
    
    private Runnable getOpenDebuggerRunnable(final Agent agent)
    {
        return () ->
        {
            try
            {
                doOpenDebugger(agent);
            }
            catch(SoarException e)
            {
                LOG.error("Failed to open new debugger: " + e.getMessage(), e);
            }
        };
    }
    
    private void doOpenDebugger(Agent agent) throws SoarException
    {
        final ThreadedAgent ta = ThreadedAgent.find(agent);
        if(ta == null)
        {
            throw new SoarException("Can't attach debugger to agent with no ThreadedAgent proxy");
        }
        JSoarDebugger.attach(ta, getProperties());
    }
    
    @Override
    public void closeDebugger(Agent agent)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(() -> doCloseDebugger(agent));
        }
        else
        {
            doCloseDebugger(agent);
        }
    }
    
    private void doCloseDebugger(Agent agent)
    {
        final ThreadedAgent ta = ThreadedAgent.find(agent);
        if(ta == null)
        {
            LOG.warn("Tried to close debugger for agent {} that does not have a debugger.", agent.getName());
            return;
        }
        
        JSoarDebugger.exit(ta);
    }
    
    @Override
    public JSoarDebugger getDebugger(Agent agent)
    {
        final ThreadedAgent ta = ThreadedAgent.find(agent);
        if(ta == null)
        {
            return null;
        }
        return JSoarDebugger.getDebugger(ta);
    }
}

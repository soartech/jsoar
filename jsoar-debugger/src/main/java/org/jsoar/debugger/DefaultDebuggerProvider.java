/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 25, 2009
 */
package org.jsoar.debugger;

import java.lang.reflect.InvocationTargetException;

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
    
    private final JSoarDebuggerConfiguration config;

    public DefaultDebuggerProvider()
    {
        this(null);
    }

    /**
     * @param config
     */
    public DefaultDebuggerProvider(JSoarDebuggerConfiguration config)
    {
        this.config = config;
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
                    logger.error("Failed to open new debugger", e);
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
        final JSoarDebugger debugger = JSoarDebugger.attach(ta);
        if(debugger != null && config != null)
        {
            debugger.setConfiguration(config);
        }
        
    }
}

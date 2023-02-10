/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Mar 22, 2009
 */
package org.jsoar.kernel;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the default implementation of the {@link DebuggerProvider}
 * interface.
 * 
 * <p>Since a debugger is not provided in the core JSoar package
 * (jsoar-core.jar) this class attempts to load a debugger reflectively
 * based on a system property. When {@link #openDebugger(Agent)} is called
 * the value of the {@code jsoar.debugger.provider} system property is
 * retrieved. It is assumed to be the name of a public class with public
 * no-args constructor that implements {@link DebuggerProvider}. If the
 * class is loaded and instantiated successfully, {@link #openDebugger(Agent)}
 * is called.
 * 
 * <p>If the {@code jsoar.debugger.provider} property is not set, then
 * {@code org.jsoar.debugger.DefaultDebuggerProvider} is used as a default.
 * That is, if it's on the class path, the graphical JSoar debugger will be
 * used.
 * 
 * @author ray
 */
public class DefaultDebuggerProvider extends AbstractDebuggerProvider
{
    private static final Logger logger = LoggerFactory.getLogger(DefaultDebuggerProvider.class);
    
    public static final String PROPERTY = "jsoar.debugger.provider";
    public static final String DEFAULT_CLASS = "org.jsoar.debugger.DefaultDebuggerProvider";
    
    private DebuggerProvider debuggerProvider;
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.DebuggerProvider#openDebugger()
     */
    @Override
    public void openDebugger(Agent agent) throws SoarException
    {
        this.debuggerProvider = loadProvider();
        this.debuggerProvider.openDebugger(agent);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.DebuggerProvider#openDebuggerAndWait(org.jsoar.kernel.Agent)
     */
    @Override
    public void openDebuggerAndWait(Agent agent) throws SoarException,
            InterruptedException
    {
        this.debuggerProvider = loadProvider();
        this.debuggerProvider.openDebuggerAndWait(agent);
    }
    
    private synchronized DebuggerProvider loadProvider() throws SoarException
    {
        final String className = System.getProperty(PROPERTY, DEFAULT_CLASS);
        try
        {
            final Class<?> klass = Class.forName(className);
            final Object o = klass.getConstructor().newInstance();
            if(o instanceof DebuggerProvider)
            {
                final DebuggerProvider p = (DebuggerProvider) o;
                p.setProperties(getProperties());
                return p;
            }
            else
            {
                logger.error("Expected instance of " + DebuggerProvider.class + ", got " + klass);
                throw new SoarException("Expected instance of " + DebuggerProvider.class + ", got " + klass);
            }
        }
        catch(ClassNotFoundException e)
        {
            logger.error("Could not find default debugger provider class '" + DEFAULT_CLASS + "'");
            throw new SoarException("Could not find default debugger provider class '" + DEFAULT_CLASS + "'");
        }
        catch(InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e)
        {
            logger.error("Error instantiating debugger provider class '" + DEFAULT_CLASS + "': " + e.getMessage(), e);
            throw new SoarException("Error instantiating debugger provider class '" + DEFAULT_CLASS + "': " + e.getMessage(), e);
        }
        
    }
    
    @Override
    public void closeDebugger(Agent agent)
    {
        this.debuggerProvider.closeDebugger(agent);
    }
    
    @Override
    public Object getDebugger(Agent agent)
    {
        return this.debuggerProvider.getDebugger(agent);
    }
}

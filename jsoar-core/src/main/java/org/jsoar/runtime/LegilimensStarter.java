/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2009
 */
package org.jsoar.runtime;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author ray
 */
public class LegilimensStarter
{
    private static final Logger LOG = LoggerFactory.getLogger(LegilimensStarter.class);
    
    public static final String AUTO_START_PROPERTY = "jsoar.legilimens.autoStart";
    
    /**
     * If the auto-start property is set ({@link #AUTO_START_PROPERTY}), then this
     * method will try to start Legilimens using {@link #start()}. Otherwise, it does
     * nothing.
     * 
     * <p>This method is called automatically when a ThreadedAgent is created.
     * 
     * @see ThreadedAgentManager#attach(org.jsoar.kernel.Agent)
     */
    public static void startIfAutoStartEnabled()
    {
        final String property = System.getProperty(AUTO_START_PROPERTY, "false");
        if(Boolean.parseBoolean(property))
        {
            start();
        }
    }
    
    /**
     * If Legilimens is available (it's on the classpath), start it. Otherwise,
     * print a warning and do nothing. If any errors occur, they are logged and
     * ignored.
     */
    public static void start()
    {
        LOG.info("Attempting to start Legilimens server, if available");
        
        final Class<?> serverClass = getServerClass();
        if(serverClass == null)
        {
            return;
        }
        
        final Method startMethod = getStartMethod(serverClass);
        if(startMethod == null)
        {
            return;
        }
        
        try
        {
            startMethod.invoke(null);
        }
        catch(IllegalArgumentException e)
        {
            LOG.warn("Failed to start Legilimens: " + e.getMessage(), e);
        }
        catch(IllegalAccessException e)
        {
            LOG.warn("Failed to start Legilimens: " + e.getMessage(), e);
        }
        catch(InvocationTargetException e)
        {
            LOG.warn("Failed to start Legilimens: " + e.getMessage(), e);
        }
    }
    
    public static Class<?> getServerClass()
    {
        try
        {
            return Class.forName("org.jsoar.legilimens.LegilimensServer");
        }
        catch(ClassNotFoundException e)
        {
            LOG.warn("Could not locate LegilimensServer class.");
            return null;
        }
    }
    
    public static Method getStartMethod(Class<?> serverClass)
    {
        try
        {
            return serverClass.getMethod("start");
        }
        catch(SecurityException e)
        {
            LOG.warn("Could not find start() method on LegilimensServer class", e);
            return null;
        }
        catch(NoSuchMethodException e)
        {
            LOG.warn("Could not find start() method on LegilimensServer class", e);
            return null;
        }
    }
}

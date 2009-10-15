/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.legilimens.trace.AgentTraceState;
import org.jsoar.runtime.ThreadedAgent;
import org.restlet.Component;
import org.restlet.data.Protocol;

/**
 * A simple, embedded web application that provides remote debugging for all
 * JSoar agents (those wrapped with {@link ThreadedAgent} in a single JVM.
 * 
 * <p>The requirement for {@link ThreadedAgent} is to ensure that the agent can
 * be accessed in a thread-safe way. {@link Agent} provides no such guarantees.
 * 
 * @author ray
 */
public class LegilimensServer
{
    private static final String ROOT_PROPERTY = "jsoar.legilimens.root";
    private static final String DEFAULT_ROOT = "/jsoar";
    private static final String PORT_PROPERTY = "jsoar.legilimens.port";
    private static final int DEFAULT_PORT = 12122;

    // See Effective Java, page 283
    private static class InstanceHolder
    {
        static final LegilimensServer instance = new LegilimensServer();
    }
    
    private final Component component;
    
    /**
     * Starts the server, if it hasn't already been started, and returns it.
     * 
     * @return the singleton server instance
     */
    public static LegilimensServer start()
    {
        return InstanceHolder.instance;
    }
    
    private LegilimensServer()
    {
        component = new Component();
        
        component.getServers().add(Protocol.HTTP, getPort());
        component.getClients().add(Protocol.CLAP);
        
        component.getDefaultHost().attach(getRoot(), new LegilimensApplication());
        try
        {
            component.start();
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to start server: " + e.getMessage(), e);
        }
    }
    
    private String getRoot()
    {
        return System.getProperty(ROOT_PROPERTY, DEFAULT_ROOT);
    }
    
    private int getPort()
    {
        return Integer.parseInt(System.getProperty(PORT_PROPERTY, Integer.toString(DEFAULT_PORT)));
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) throws Exception
    {
        createAgent("legilimens", "http://darevay.com/jsoar/waterjugs.soar");
        createAgent("eyeball", null);
        
        start();
    }

    private static ThreadedAgent createAgent(String name, final String rules) throws InterruptedException, ExecutionException, TimeoutException
    {
        final ThreadedAgent agent = ThreadedAgent.create();
        agent.setName(name);
        AgentTraceState.attach(agent);
        if(rules != null)
        {
            agent.executeAndWait(new Callable<Void>()
            {
                @Override
                public Void call() throws Exception
                {
                    agent.getInterpreter().eval("source " + rules);
                    agent.runFor(5, RunType.DECISIONS);
                    return null;
                }
            }, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        }
        return agent;
    }
}

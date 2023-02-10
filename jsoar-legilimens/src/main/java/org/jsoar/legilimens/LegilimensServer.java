/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.legilimens.trace.AgentTraceBuffer;
import org.jsoar.runtime.LegilimensStarter;
import org.jsoar.runtime.ThreadedAgent;
import org.restlet.Component;
import org.restlet.data.Protocol;
import org.restlet.routing.VirtualHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(LegilimensServer.class);
    
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
        
        final int port = getPort();
        component.getServers().add(Protocol.HTTP, port);
        component.getClients().add(Protocol.CLAP); // used for static resources on classpath
        
        final VirtualHost host = component.getDefaultHost();
        final String root = getRoot();
        host.attach(root, new LegilimensApplication());
        try
        {
            component.start();
        }
        catch(Exception e)
        {
            throw new RuntimeException("Failed to start server: " + e.getMessage(), e);
        }
        
        logger.info("Legilimens web app running at http://localhost:" + port + root + "/");
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
     * @throws IOException
     * @throws TimeoutException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException, IOException
    {
        createAgent("eye ? ball", null);
        
        // This should cause start() to be called when agents are created below.
        System.setProperty(LegilimensStarter.AUTO_START_PROPERTY, "true");
        
        createAgent("waterjugs", "http://darevay.com/jsoar/waterjugs.soar");
        createAgent("Towers of Hanoi", "http://darevay.com/jsoar/towers.soar");
    }
    
    private static ThreadedAgent createAgent(String name, final String rules) throws InterruptedException, ExecutionException, TimeoutException, IOException
    {
        final ThreadedAgent agent = ThreadedAgent.create();
        agent.setName(name);
        AgentTraceBuffer.attach(agent.getAgent());
        if(rules != null)
        {
            agent.executeAndWait(() -> {
                agent.getInterpreter().eval("source " + rules);
                agent.runFor(5, RunType.DECISIONS);
                return null;
            }, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        }
        return agent;
    }
}

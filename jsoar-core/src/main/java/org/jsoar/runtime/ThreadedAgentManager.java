/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2009
 */
package org.jsoar.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.Agent;
import org.jsoar.util.events.SoarEventManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapMaker;

/**
 * Helper class that deals with managing which threaded agents are attached to which
 * {@link Agent} instances.
 * 
 * @author ray
 */
enum ThreadedAgentManager
{
    INSTANCE;
    
    private static final Logger LOG = LoggerFactory.getLogger(ThreadedAgentManager.class);
    
    private final Map<Agent, ThreadedAgent> agents = new MapMaker().weakKeys().makeMap();
    private final SoarEventManager events = new SoarEventManager();
    
    /**
     * Creates a ThreadedAgent. Automatically initializes it. Returns when complete.
     * 
     * @param name
     */
    public ThreadedAgent create(String name)
    {
        synchronized (agents)
        {
            final ThreadedAgent agent = attach(new Agent(name, false)).initialize(new CompletionHandler<Void>()
            {
                
                @Override
                public void finish(Void result)
                {
                    synchronized (agents)
                    {
                        agents.notifyAll();
                    }
                }
            });
            try
            {
                agents.wait();
            }
            catch(InterruptedException e)
            {
                LOG.error("Interrupted waiting for new ThreadedAgent to initialize.", e);
                Thread.currentThread().interrupt(); // reset interrupt
            }
            
            return agent;
        }
    }
    
    public ThreadedAgent find(Agent agent)
    {
        synchronized (agents)
        {
            return agents.get(agent);
        }
    }
    
    public List<ThreadedAgent> getAll()
    {
        synchronized (agents)
        {
            return new ArrayList<ThreadedAgent>(agents.values());
        }
    }
    
    public ThreadedAgent attach(Agent agent)
    {
        synchronized (agents)
        {
            ThreadedAgent ta = agents.get(agent);
            if(ta == null)
            {
                ta = new ThreadedAgent(agent);
                agents.put(agent, ta);
                events.fireEvent(new ThreadedAgentAttachedEvent(ta));
            }
            
            LegilimensStarter.startIfAutoStartEnabled();
            
            return ta;
        }
        
    }
    
    public void detach(ThreadedAgent agent)
    {
        synchronized (agents)
        {
            agents.remove(agent.getAgent());
            events.fireEvent(new ThreadedAgentDetachedEvent(agent));
        }
    }
    
    /**
     * @return the events
     */
    public SoarEventManager getEventManager()
    {
        return events;
    }
}

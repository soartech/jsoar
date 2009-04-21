/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 22, 2008
 */
package org.jsoar.runtime;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.events.RunLoopEvent;
import org.jsoar.kernel.events.StopEvent;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;

import com.google.common.collect.MapMaker;

/**
 * A wrapper around a raw {@link Agent} which gives the agent its own thread
 * and provides methods for safely interacting with the agent from other 
 * threads.
 * 
 * <p>Generally, <b>all</b> access to agent data structures, i.e. the 
 * {@link Agent} instance should be marshaled through the {@link #execute(Runnable)}
 * methods. Note however, that many public Soar interfaces, or at least parts of them,
 * are immutable, so it is safe to access them from other threads.
 * 
 * @author ray
 */
public class ThreadedAgent
{
    private final static Map<Agent, ThreadedAgent> proxies = new MapMaker().weakKeys().makeMap();
    
    private final BlockingQueue<Runnable> commands = new LinkedBlockingQueue<Runnable>();
    private final Agent agent;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean agentRunning = new AtomicBoolean(false);
    private final AgentThread agentThread = new AgentThread();
    private final WaitRhsFunction waitFunction = new WaitRhsFunction();
    
    /**
     * If there is a ThreadedAgent already attached to the given agent, return
     * it.
     * 
     * @param agent the agent
     * @return the ThreadedAgent proxy, or <code>null</code> if none is 
     *  currently attached.
     */
    public static ThreadedAgent find(Agent agent)
    {
        synchronized (proxies)
        {
            return proxies.get(agent);
        }
    }
    
    /**
     * Attach a threaded wrapper to the given agent. The returned object must
     * be initialized.
     * 
     * @param agent the agent to wrap
     * @return 
     */
    public static ThreadedAgent attach(Agent agent) 
    {
        synchronized(proxies)
        {
            ThreadedAgent ta = proxies.get(agent);
            if(ta == null)
            {
                ta = new ThreadedAgent(agent);
                proxies.put(agent, ta);
            }
            return ta;
        }
    }
    
    /**
     * @param agent the agent to wrap.
     */
    private ThreadedAgent(Agent agent)
    {
        this.agent = agent;
        agentThread.setName("Agent '" + this.agent + "' thread");
        
        this.agent.getEventManager().addListener(RunLoopEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                // If the thread has been interrupted (due to shutdown), throw
                // an exception to break us out of the agent run loop.
                // TODO: It may be nice to have a more official way of doing this
                // from the RunLoopEvent.
                if(Thread.currentThread().isInterrupted()) 
                {
                    throw new InterruptAgentException();
                }
                Runnable runnable = commands.poll();
                while(runnable != null)
                {
                    runnable.run();
                    runnable = commands.poll();
                }
            }});
        
        waitFunction.attach(this);
    }
    
    /**
     * Initialize this object and the underlying agent.
     * 
     * @return this
     */
    public ThreadedAgent initialize()
    {
        return initialize(null);
    }
    
    /**
     * Initialize this object and the underlying agent.
     * 
     * @param done if not <code>null</code> this handler is called after the 
     * agent is initialized.
     * @return this
     */
    public ThreadedAgent initialize(final CompletionHandler<Void> done)
    {
        // Only start the agent thread once
        if(!initialized.getAndSet(true))
        {
            this.agentThread.start();
        }
        
        execute(new Callable<Void>()
        {
            public Void call() throws Exception
            {
                agent.initialize();
                return null;
            }
        }, done);
        return this;
    }
    
    /**
     * Detach this object from the agent. This method will stop the agent thread,
     * and wait for it to exit before proceeding. After being detached, this
     * object may not be used again.
     */
    public void detach()
    {
        try
        {
            agentThread.interrupt();
            try
            {
                agentThread.join();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
            waitFunction.detach();
        }
        finally
        {
            synchronized(proxies)
            {
                proxies.remove(agent);
            }
        }
    }
    
    /**
     * Test whether the current thread is the agent thread
     * 
     * @return true if the current thread is the agent thread
     */
    public boolean isAgentThread()
    {
        return Thread.currentThread().equals(agentThread);
    }
    
    /**
     * @return the agent owned by this proxy
     */
    public Agent getAgent()
    {
        return agent;
    }

    /**
     * @return <code>true</code> if the agent is current running
     */
    public boolean isRunning()
    {
        return agentRunning.get();
    }

    /**
     * Run the agent in a separate thread. This method returns immediately.
     * If the agent is already running, the command is ignored. When the agent
     * stops a {@link StopEvent} event will be fired.
     * 
     * @param n number of steps
     * @param runType type of steps
     * @see Agent#runFor(int, RunType)
     */
    public void runFor(final int n, final RunType runType)
    {
        if(agentRunning.getAndSet(true))
        {
            return;
        }
        execute(new Callable<Void>() {

            @Override
            public Void call()
            {
                try
                {
                    agent.runFor(n, runType);
                }
                finally
                {
                    agentRunning.set(false);
                    agent.getEventManager().fireEvent(new StopEvent(agent));
                }
                return null;
                
            }}, null);
    }
    
    /**
     * Start the agent running. The agent will run until {@link #stop()} is
     * called or it halts for some reason. When the agent stops a
     * {@link StopEvent} event will be fired.
     */
    public void runForever()
    {
        runFor(0, RunType.FOREVER);
    }
    
    /**
     * Stop the agent running at some point in the future. When the agent
     * finally stops, a {@link StopEvent} event will be fired.
     */
    public void stop()
    {
        execute(new Runnable() { public void run() { agent.stop(); } });
    }
    
    
    
    /**
     * Execute the given runnable in the agent thread.
     * 
     * @param runnable the runnable to run
     */
    private void execute(Runnable runnable)
    {
        if(!isAgentThread())
        {
            commands.add(runnable);
        }
        else
        {
            runnable.run();
        }
    }
    
    /**
     * Execute the given callable in the agent thread, wait for its result and
     * return it.
     * 
     * @param <V> return type
     * @param callable the callable to run
     * @param finish called after the callable is executed. Ignored if <code>null</code>.
     * @throws RuntimeException if an exception is thrown while waiting for the
     *  result.
     */
    public <V> void execute(final Callable<V> callable, final CompletionHandler<V> finish)
    {
        execute(new Runnable() {

            @Override
            public void run()
            {
                try
                {
                    final V result = callable.call();
                    if(finish != null)
                    {
                        finish.finish(result);
                    }
                }
                catch(InterruptAgentException e)
                {
                    throw e;
                }
                catch (Exception e)
                {
                    // TODO
                    e.printStackTrace();
                    agent.getPrinter().error("ERROR: " + e.getCause().getMessage() + "\n");
                }
            }});
    }
    
    /**
     * Returns the command queue. For use only by the {@link WaitRhsFunction}
     * 
     * @return the command queue
     */
    BlockingQueue<Runnable> getCommandQueue()
    {
        return commands;
    }
    
    private class AgentThread extends Thread
    {
        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run()
        {
            while(!isInterrupted())
            {
                try
                {
                    commands.take().run();
                }
                catch (InterruptAgentException e)
                {
                    this.interrupt();
                }
                catch (InterruptedException e)
                {
                    this.interrupt();
                }
            }
        }
    }
        
    private static class InterruptAgentException extends RuntimeException
    {
        private static final long serialVersionUID = 3075897216751716278L;
    }
}

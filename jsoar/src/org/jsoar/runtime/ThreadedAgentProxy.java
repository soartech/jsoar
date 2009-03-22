/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 22, 2008
 */
package org.jsoar.runtime;

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

/**
 * @author ray
 */
public class ThreadedAgentProxy
{
    private final BlockingQueue<Runnable> commands = new LinkedBlockingQueue<Runnable>();
    private final Agent agent;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean agentRunning = new AtomicBoolean(false);
    private final AgentThread agentThread = new AgentThread();
    
    /**
     * @param agent an unitialized agent
     */
    public ThreadedAgentProxy(Agent agent)
    {
        this.agent = agent;
        agentThread.setName("ThreadedAgentProxy thread");
        
        this.agent.getEventManager().addListener(RunLoopEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                // If the thread has been interrupted (due to shutdown), throw
                // an exception to break us out of the agent run loop.
                // TODO: It may be nice to have a more official way of doing this
                // from the RunLoopEvent.
                if(Thread.interrupted()) 
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
    }
    
    public ThreadedAgentProxy initialize()
    {
        return initialize(null);
    }
    
    /**
     * Initialize this proxy and the agent
     */
    public ThreadedAgentProxy initialize(final Completer<Void> done)
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
     * Shutdown this proxy
     */
    public void shutdown()
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
     * If the agent is already running, the command is ignored.
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
    
    public void runForever()
    {
        runFor(0, RunType.FOREVER);
    }
    
    /**
     * Stop the agent running at some point in the future
     */
    public void stop()
    {
        execute(new Runnable() { public void run() { agent.stop(); } });
    }
    
    /**
     * Execute the given runnable in the agent thread. The agent lock will be held
     * while the command is run.
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
     * return it. The agent lock will be help while the command is run.
     * 
     * @param <V> return type
     * @param callable the callable to run
     * @return the result of the callable
     * @throws RuntimeException if an exception is thrown while waiting for the
     *  result.
     */
    public <V> void execute(final Callable<V> callable, final Completer<V> completer)
    {
        execute(new Runnable() {

            @Override
            public void run()
            {
                try
                {
                    final V result = callable.call();
                    if(completer != null)
                    {
                        completer.finish(result);
                    }
                }
                catch (Exception e)
                {
                    // TODO
                    e.printStackTrace();
                    agent.getPrinter().error("ERROR: " + e.getCause().getMessage() + "\n");
                }
            }});
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

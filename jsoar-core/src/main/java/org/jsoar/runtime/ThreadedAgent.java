/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 22, 2008
 */
package org.jsoar.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.DebuggerProvider;
import org.jsoar.kernel.ProductionManager;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.commands.RunCommand;
import org.jsoar.kernel.commands.StopCommand;
import org.jsoar.kernel.events.RunLoopEvent;
import org.jsoar.kernel.events.StopEvent;
import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.rhs.functions.RhsFunctionManager;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.util.adaptables.AbstractAdaptable;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.jsoar.util.events.SoarEventManager;
import org.jsoar.util.properties.PropertyManager;
import org.jsoar.util.properties.PropertyProvider;

import com.google.common.collect.MapMaker;

/**
 * A wrapper around a raw {@link Agent} which gives the agent its own thread
 * and provides methods for safely interacting with the agent from other 
 * threads. The wrapper includes a number of convenience wrapper methods that
 * forward to the methods with the same name in the underlying agent.
 * 
 * <p>See also: <a href="http://code.google.com/p/jsoar/wiki/JSoarUsersGuide">JSoar User Guide</a>
 * 
 * <p>Generally, <b>all</b> access to agent data structures, i.e. the 
 * {@link Agent} instance should be marshaled through the {@link #execute(Runnable)}
 * methods. Note however, that many public Soar interfaces, or at least parts of them,
 * are immutable, so it is safe to access them from other threads.
 * 
 * <p>{@code ThreadedAgent} is an {@link Adaptable} where {@link #getAdapter(Class)}
 * has the same behavior as in {@link Agent}.
 * 
 * <p>This object sets the {@link SoarProperties#IS_RUNNING} property appropriately
 * and fires events when its state changes.
 * 
 * <p>This object installs {@code run} and {@code stop-soar} commands as well as
 * the {@code wait} RHS function.
 * 
 * @author ray
 */
public class ThreadedAgent extends AbstractAdaptable
{
    private static final Log logger = LogFactory.getLog(ThreadedAgent.class);
    
    private final static Map<Agent, ThreadedAgent> proxies = new MapMaker().weakKeys().makeMap();
    
    private final Agent agent;
    private final BlockingQueue<Runnable> commands = new LinkedBlockingQueue<Runnable>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean agentRunning = new AtomicBoolean(false);
    private final PropertyProvider<Boolean> agentRunningProvider = new PropertyProvider<Boolean>() {

        @Override
        public Boolean get()
        {
            return agentRunning.get();
        }

        @Override
        public Boolean set(Boolean value)
        {
            throw new UnsupportedOperationException(SoarProperties.IS_RUNNING.getName() + " property is read-only");
        }
    };
    
    private final AgentThread agentThread = new AgentThread();
    private final WaitRhsFunction waitFunction = new WaitRhsFunction();
    
    private final RunCommand runCommand = new RunCommand(this);
    private final StopCommand stopCommand = new StopCommand(this);

    /**
     * Create a new {@link Agent} and automatically wrap it with a ThreadedAgent.
     * This method also initializes the agent and starts its thread. 
     * 
     * <p>This is convenience method equivalent to
     * <pre>{@code
     * ThreadedAgent agent = ThreadedAgent.create(new Agent()).initialize();
     * }</pre>
     * 
     * <p>Note that, unlike a normal call to {@link #initialize()} this method
     * will not return until initialization has completed. Otherwise, strange
     * situations can arise if the agent is returned before initialization is
     * complete.
     * 
     * @return a new, initialized threaded agent
     */
    public static ThreadedAgent create()
    {
        final Object wait = new String("agent init wait lock");
        synchronized(wait)
        {
            final ThreadedAgent agent = attach(new Agent()).initialize(new CompletionHandler<Void>() {

                @Override
                public void finish(Void result)
                {
                    synchronized(wait)
                    {
                        wait.notify();
                    }
                }});
            try
            {
                wait.wait();
            }
            catch (InterruptedException e)
            {
                logger.error("Interrupted waiting for new ThreadedAgent to initialize.", e);
                Thread.currentThread().interrupt(); // reset interrupt
            }
            
            return agent;
        }
    }
    
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
     * Returns a list of all current threaded agents.
     * 
     * @return a list of all current threaded agents
     */
    public static List<ThreadedAgent> getAll()
    {
        synchronized (proxies)
        {
            return new ArrayList<ThreadedAgent>(proxies.values());
        }
    }
    
    /**
     * Attach a threaded wrapper to the given agent. The returned object must
     * be initialized.
     * 
     * @param agent the agent to wrap
     * @return a threaded agent wrapper
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
        
        this.agent.getProperties().setProvider(SoarProperties.IS_RUNNING, agentRunningProvider);
        
        getEvents().addListener(RunLoopEvent.class, new SoarEventListener() {

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

        final SoarCommandInterpreter interp = agent.getInterpreter();
        interp.addCommand("run", runCommand);
        interp.addCommand("stop-soar", stopCommand);
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
     * object may not be used again. The underyling agent remains available for
     * use.
     * 
     * @see #dispose()
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
            
            // TODO: Unregister run and stop commands.
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
     * Dispose of this object and the underlying agent.
     * 
     * @see #detach()
     */
    public void dispose()
    {
        detach();
        agent.dispose();
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
     * @see Agent#runFor(long, RunType)
     */
    public void runFor(final long n, final RunType runType)
    {
        if(agentRunning.getAndSet(true))
        {
            return;
        }
        agent.getProperties().firePropertyChanged(SoarProperties.IS_RUNNING, true, false);
        
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
                    agent.getProperties().firePropertyChanged(SoarProperties.IS_RUNNING, false, true);
                    getEvents().fireEvent(new StopEvent(agent));
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
    
    // Convenience methods forwarded to equivalent {@link Agent} methods.
    public String getName() { return agent.getName(); }
    public void setName(String name) { agent.setName(name); }
    public SoarCommandInterpreter getInterpreter() { return agent.getInterpreter(); }
    public Printer getPrinter() { return agent.getPrinter(); }
    public Trace getTrace() { return agent.getTrace(); }
    public SoarEventManager getEvents() { return agent.getEvents(); }
    public PropertyManager getProperties() { return agent.getProperties(); }
    public SymbolFactory getSymbols() { return agent.getSymbols(); }
    public InputOutput getInputOutput() { return agent.getInputOutput(); }
    public ProductionManager getProductions() { return agent.getProductions(); }
    public RhsFunctionManager getRhsFunctions() { return agent.getRhsFunctions(); }
    public DebuggerProvider getDebuggerProvider() { return agent.getDebuggerProvider(); }
    public void setDebuggerProvider(DebuggerProvider p) { agent.setDebuggerProvider(p); }
    public void openDebugger() throws SoarException { agent.openDebugger(); }
    public void openDebuggerAndWait() throws SoarException, InterruptedException { agent.openDebuggerAndWait(); }
    
    
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
     * Schedule the given callable for execution in the agent thread and return
     * immediately. This is the correct way to access the agent in a thread-safe
     * manner without deadlocks.
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
                    final Throwable cause = e.getCause();
                    agent.getPrinter().error((cause != null ? cause.getMessage() : e.getMessage()) + "\n");
                }
            }});
    }
    
    /**
     * Execute a callable in the agent thread and wait for its result. 
     * 
     * <p>Note that in almost all cases, {@link #execute(Callable, CompletionHandler)} is
     * what you want. This method is very prone to deadlocks if the thread that is calling
     * it (e.g. the Swing UI thread) handles events from the agent.  
     * 
     * @param <V> the return type
     * @param callable the callable to run in the agent thread
     * @param timeout timeout value
     * @param timeUnit timeout units
     * @return the return value
     * @throws InterruptedException if the thread is interrupted while waiting
     * @throws ExecutionException if there's an unhandled exception in the callable
     * @throws TimeoutException on timeout
     */
    public <V> V executeAndWait(final Callable<V> callable, long timeout, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException
    {
        final FutureTask<V> task = new FutureTask<V>(callable);
        
        execute(task);
        
        return task.get(timeout, timeUnit);
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
    
    /* (non-Javadoc)
     * @see org.jsoar.util.adaptables.AbstractAdaptable#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(Class<?> klass)
    {
        Object result = agent.getAdapter(klass);
        if(result != null)
        {
            return result;
        }
        return super.getAdapter(klass);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return agent.toString();
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

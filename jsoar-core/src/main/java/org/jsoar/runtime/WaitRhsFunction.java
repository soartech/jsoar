/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 19, 2009
 */
package org.jsoar.runtime;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.events.AfterDecisionCycleEvent;
import org.jsoar.kernel.events.AsynchronousInputReadyEvent;
import org.jsoar.kernel.events.PhaseEvents.AfterInput;
import org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctions;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.Arguments;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.jsoar.util.properties.PropertyProvider;

/**
 * A "wait" RHS function used in conjunction with a {@link ThreadedAgent}.
 * 
 * <p>Takes a single optional argument, the amount of time to wait in 
 * milliseconds. If the argument is omitted, the function will wait forever.
 * The wait RHS function causes the agent to stop executing (i.e. its
 * thread goes to sleep) until one of the following conditions occurs:
 * 
 * <ul>
 * <li>The agent halts
 * <li>The agent is stopped with {@link ThreadedAgent#stop()}
 * <li>The agent's thread is interrupted
 * <li>A {@link AsynchronousInputReadyEvent} is fired through the agent indicating that new asynchronous
 *     I/O is available.
 * <li>The optional timeout provided as an argument to the wait function 
 *     expires.
 * </ul>
 * 
 * <p>The wait function may only be used as a standalone function.
 * 
 * <p>The wait function only schedules a wait at the end of the current decision 
 * cycle. That is, the agent is not suspended while the production is firing.
 * This means that if multiple productions call the wait function in the same
 * decision, the wait time of the last call "wins". This approach avoids 
 * reentrancy issues as well as ensuring that when a wait is stopped because of
 * new input, another wait before the input cycle doesn't cause a deadlock. 
 * 
 * <p>The current waiting status of the agent is stored in the {@link SoarProperties#WAIT_INFO}
 * property.
 * 
 * <p>Note: This RHS function is automatically registered by {@link ThreadedAgent}
 * 
 * @author ray
 */
public class WaitRhsFunction extends AbstractRhsFunctionHandler
{
    private ThreadedAgent agent;
    private RhsFunctionHandler oldHandler;
    private boolean inputReady = false;
    private SoarEventListener inputReadyListener;
    private final AsynchronousInputReadyCommand inputReadyCommand = new AsynchronousInputReadyCommand();
    private SoarEventListener afterInputListener;
    private SoarEventListener afterDecisionCycleListener;
    private WaitInfo requestedWaitInfo = WaitInfo.NOT_WAITING;
    private final AtomicReference<WaitInfo> waitInfo = new AtomicReference<WaitInfo>(WaitInfo.NOT_WAITING);
    private final PropertyProvider<WaitInfo> waitInfoProp = new PropertyProvider<WaitInfo>() {

        @Override
        public WaitInfo get()
        {
            return waitInfo.get();
        }

        @Override
        public WaitInfo set(WaitInfo value)
        {
            throw new IllegalArgumentException("Can't set wait_info property");
        }};
        
    
    public WaitRhsFunction()
    {
        super("wait", 0, 1);
    }
    
    public void attach(ThreadedAgent agent)
    {
        Arguments.checkNotNull(agent, "agent");
        if(this.agent != null)
        {
            throw new IllegalStateException("Already attached to agent");
        }
        this.agent = agent;
        
        // Listen for input ready events
        this.agent.getEvents().addListener(AsynchronousInputReadyEvent.class, 
                inputReadyListener = new SoarEventListener() {

                    @Override
                    public void onEvent(SoarEvent event)
                    {
                        setNewInputAvailable();
                    }});
        
        // Listen for end of decision cycle event. This is where we actually do the wait
        // if one has been requested. Since the next phase will be the input phase,
        // we don't have to worry about additional waits blocking it and we'll conveniently
        // go straight to input when an asynch input ready event knocks us out of a wait.
        this.agent.getEvents().addListener(AfterDecisionCycleEvent.class, 
                afterDecisionCycleListener = new SoarEventListener() {

                    @Override
                    public void onEvent(SoarEvent event)
                    {
                        doWait();
                    }});
        
        this.agent.getEvents().addListener(AfterInput.class, 
                afterInputListener = new SoarEventListener() {

                    @Override
                    public void onEvent(SoarEvent event)
                    {
                        inputReady = false;
                    }});
        
        // Set up "waiting" property
        this.agent.getProperties().setProvider(SoarProperties.WAIT_INFO, waitInfoProp);
        
        // Register the RHS function
        this.oldHandler = this.agent.getRhsFunctions().registerHandler(this);
    }
    
    public void detach()
    {
        if(agent != null)
        {
            agent.getEvents().removeListener(null, inputReadyListener);
            agent.getEvents().removeListener(null, afterInputListener);
            agent.getEvents().removeListener(null, afterDecisionCycleListener);
            agent.getRhsFunctions().unregisterHandler(getName());
            if(oldHandler != null)
            {
                agent.getRhsFunctions().registerHandler(oldHandler);
                oldHandler = null;
            }
            agent = null;
        }
    }
    
    private void doWait()
    {
        if(!requestedWaitInfo.waiting) // no wait requested
        {
            inputReady = false;
            return;
        }
        
        // Update the wait property
        waitInfo.set(requestedWaitInfo);

        final long start = System.currentTimeMillis();
        final BlockingQueue<Runnable> commands = agent.getCommandQueue();
        boolean done = isDoneWaiting();
        while(!done)
        {
            try
            {
                final long remaining = requestedWaitInfo.timeout - (System.currentTimeMillis() - start);
                if(remaining <= 0)
                {
                    done = true;
                }
                
                final Runnable command = commands.poll(remaining, TimeUnit.MILLISECONDS);
                if(command != null)
                {
                    command.run();
                    
                    done = isDoneWaiting();
                }
                else
                {
                    done = true; // timeout
                }
            }
            catch (InterruptedException e)
            {
                done = true;
                Thread.currentThread().interrupt(); // Reset the interrupt status for higher levels!
                break;
            }
        }
        requestedWaitInfo = WaitInfo.NOT_WAITING; // clear the wait
        
        inputReady = false;
        waitInfo.set(WaitInfo.NOT_WAITING);
    }
    
    private synchronized void setNewInputAvailable()
    {
        // This will break out of the poll below
        if(agent.isAgentThread())
        {
            inputReady = true;
        }
        else
        {
            agent.execute(inputReadyCommand, null);
        }
    }
    
    private synchronized boolean isDoneWaiting()
    {
        return agent.getAgent().getReasonForStop() != null ||
               inputReady || 
               Thread.currentThread().isInterrupted();
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
            throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        RhsFunctions.checkAllArgumentsAreNumeric(getName(), arguments);
        
        // If multiple waits are requested, use the shortest
        final long timeout = arguments.isEmpty() ? Long.MAX_VALUE : arguments.get(0).asInteger().getValue();
        if(!requestedWaitInfo.waiting || timeout < requestedWaitInfo.timeout)
        {
            requestedWaitInfo = new WaitInfo(timeout, context.getProductionBeingFired());
        }
        
        return null;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler#mayBeStandalone()
     */
    @Override
    public boolean mayBeStandalone()
    {
        return true;
    }
    
    private class AsynchronousInputReadyCommand implements Callable<Void>
    {
        @Override
        public Void call() throws Exception
        {
            inputReady = true;
            return null;
        }
        
    }
}

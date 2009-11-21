/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 19, 2009
 */
package org.jsoar.runtime;

import java.util.List;

import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.events.AsynchronousInputReadyEvent;
import org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctions;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.Arguments;

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
    private WaitManager waitManager;
    private ThreadedAgent agent;
    private RhsFunctionHandler oldHandler;
    
    
    public WaitRhsFunction()
    {
        super("wait", 0, 1);
    }
    
    /**
     * Register this RHS function using the given {@link WaitManager} and the
     * agent to which it is already attached.
     * 
     * @param waitManager the wait manager
     * @throws IllegalStateException if attach has already been called before,
     *      or if the {@code waitManager} is not attached to an agent.
     */
    public void attach(WaitManager waitManager)
    {
        Arguments.checkNotNull(waitManager, "waitManager");
        if(this.waitManager != null)
        {
            throw new IllegalStateException("Already attached to wait manager");
        }
        this.waitManager = waitManager;
        this.agent = this.waitManager.getAgent();
        if(this.agent == null)
        {
            throw new IllegalStateException("Wait manager is not attached to an agent");
        }
        
        // Register the RHS function
        this.oldHandler = this.agent.getRhsFunctions().registerHandler(this);
    }
    
    /**
     * Remove this RHS function from the agent.
     */
    public void detach()
    {
        if(agent != null )
        {
            agent.getRhsFunctions().unregisterHandler(getName());
            if(oldHandler != null)
            {
                agent.getRhsFunctions().registerHandler(oldHandler);
                oldHandler = null;
            }
        }
        agent = null;
        waitManager = null;
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
        waitManager.requestWait(new WaitInfo(timeout, context.getProductionBeingFired()));
        
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
}

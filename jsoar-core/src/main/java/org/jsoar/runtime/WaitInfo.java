/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 20, 2009
 */
package org.jsoar.runtime;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.util.Arguments;

/**
 * An immutable structure containing information about the wait state of
 * an agent, typically modified by {@link WaitRhsFunction}. This type is
 * used as the value of the {@link SoarProperties#WAIT_INFO} property.
 * 
 * @author ray
 */
public class WaitInfo
{
    /**
     * Constant indicating that the agent is not waiting.
     */
    public static final WaitInfo NOT_WAITING = new WaitInfo(false, Long.MAX_VALUE, null);
    
    /**
     * If true, the agent is currently waiting
     */
    public final boolean waiting;
    
    /**
     * The timeout of the wait in milliseconds, or <code>Long.MAX_VALUE</code> if no timeout is set.
     */
    public final long timeout;
    
    /**
     * The production that caused the wait, or <code>null</code> for none.
     */
    public final Production cause;
    
    WaitInfo(long timeout, Production cause)
    {
        this(true, timeout, cause);
    }
    
    private WaitInfo(boolean waiting, long timeout, Production cause)
    {
        Arguments.check(timeout > 0, "timeout must be positive");
        
        this.waiting = waiting;
        this.timeout = timeout;
        this.cause = cause;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return waiting ? String.format("Waiting %s [%s]",
                timeout != Long.MAX_VALUE ? Long.toString(timeout) + " ms" : "forever",
                cause) : "No wait";
    }
}

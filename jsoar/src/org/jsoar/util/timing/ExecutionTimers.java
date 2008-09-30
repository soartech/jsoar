/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 29, 2008
 */
package org.jsoar.util.timing;

/**
 * Utility class for execution timers
 * 
 * @author ray
 */
public class ExecutionTimers
{
    private static final boolean ENABLED = true;
    
    /**
     * Start the given timer if timers are enabled for the application
     * 
     * @param timer The timer
     */
    public static void start(ExecutionTimer timer)
    {
        if(ENABLED)
        {
            timer.start();
        }
    }
    
    /**
     * Pause the given timer if timers are enabled for the application
     * 
     * @param timer The timer
     */
    public static void pause(ExecutionTimer timer)
    {
        if(ENABLED)
        {
            timer.pause();
        }
    }
    
    /**
     * Update the given timer if timers are enabled for the application
     * 
     * @param timer The timer
     */
    public static void update(ExecutionTimer timer)
    {
        if(ENABLED)
        {
            timer.update();
        }
    }

}

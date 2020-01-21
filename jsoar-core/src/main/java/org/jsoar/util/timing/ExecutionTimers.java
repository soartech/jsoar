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
    private static boolean enabled = true;
    
    /**
     * Start the given timer if timers are enabled for the application
     * 
     * @param timer The timer
     */
    public static void start(ExecutionTimer timer)
    {
        if(enabled)
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
        if(enabled)
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
        if(enabled)
        {
            timer.update();
        }
    }

    public static void setEnabled(boolean enabled)
    {
        ExecutionTimers.enabled = enabled;
    }


    public static boolean isEnabled()
    {
        return ExecutionTimers.enabled;
    }

}

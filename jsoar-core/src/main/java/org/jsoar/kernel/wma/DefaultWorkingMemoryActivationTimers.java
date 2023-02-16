/*
 * Copyright (c) 2013 Soar Technology, Inc.
 *
 * Created on Feb 21, 2013
 */
package org.jsoar.kernel.wma;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jsoar.kernel.wma.DefaultWorkingMemoryActivationParams.TimerLevels;
import org.jsoar.util.properties.PropertyManager;
import org.jsoar.util.timing.DefaultExecutionTimer;
import org.jsoar.util.timing.ExecutionTimer;

/**
 * Timers for wma
 * TODO: The approach to how this works should probably be generalized similar to how
 * parameters have been generalized, but this gets the functionality in there for now
 * 
 * @author bob.marinier
 */
public class DefaultWorkingMemoryActivationTimers
{
    private final PropertyManager properties;
    
    ExecutionTimer history = DefaultExecutionTimer.newInstance().setName("wma_history");
    ExecutionTimer forgetting = DefaultExecutionTimer.newInstance().setName("wma_forgetting");
    
    Map<TimerLevels, Set<ExecutionTimer>> timerMap = new HashMap<>();
    Set<ExecutionTimer> timerSet = new HashSet<>();
    
    DefaultWorkingMemoryActivationTimers(PropertyManager properties)
    {
        this.properties = properties;
        
        // for the off level, just an empty set
        timerMap.put(TimerLevels.off, new HashSet<ExecutionTimer>());
        
        // put the other timers in the one level
        Set<ExecutionTimer> one = new HashSet<>();
        one.add(history);
        one.add(forgetting);
        timerMap.put(TimerLevels.one, one);
        
        // put all timers in the timer set
        timerSet.add(history);
        timerSet.add(forgetting);
    }
    
    void start(ExecutionTimer timer)
    {
        if(timerMap.get(properties.get(DefaultWorkingMemoryActivationParams.TIMERS)).contains(timer))
        {
            timer.start();
        }
    }
    
    void pause(ExecutionTimer timer)
    {
        if(timerMap.get(properties.get(DefaultWorkingMemoryActivationParams.TIMERS)).contains(timer))
        {
            timer.pause();
        }
    }
    
    void reset(ExecutionTimer timer)
    {
        timer.reset();
    }
    
    void reset()
    {
        for(Set<ExecutionTimer> timerSet : timerMap.values())
        {
            for(ExecutionTimer timer : timerSet)
            {
                reset(timer);
            }
        }
    }
    
    ExecutionTimer get(String timerName)
    {
        for(ExecutionTimer t : timerSet)
        {
            if(t.getName().equals(timerName))
            {
                return t;
            }
        }
        return null;
    }
}

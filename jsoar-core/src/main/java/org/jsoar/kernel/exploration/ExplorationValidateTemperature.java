/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 17, 2008
 */
package org.jsoar.kernel.exploration;

/**
 * exploration.cpp:196:exploration_validate_temperature
 * 
 * @author ray
 */
public class ExplorationValidateTemperature implements ExplorationValueFunction
{
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.ExplorationValueFunction#call(double)
     */
    @Override
    public boolean call(double value)
    {
        return value > 0;
    }
    
}

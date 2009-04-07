/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 17, 2008
 */
package org.jsoar.kernel.exploration;

/**
 * exploration.cpp:188:exploration_validate_epsilon
 * 
 * @author ray
 */
public class ExplorationValidateEpsilon implements ExplorationValueFunction
{

    /* (non-Javadoc)
     * @see org.jsoar.kernel.ExplorationValueFunction#call(double)
     */
    @Override
    public boolean call(double value)
    {
        return value >= 0 && value <= 1;
    }

}

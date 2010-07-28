/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 27, 2010
 */
package org.jsoar.soarunit;

import org.jsoar.kernel.SoarException;

/**
 * @author ray
 */
public interface TestAgent
{
    void initialize(Test test) throws SoarException;
    
    void run();
    
    long getCycleCount();
    
    void dispose();
    
    String getOutput();
    
    boolean isPassCalled();
    
    String getPassMessage();
    
    boolean isFailCalled();
    
    String getFailMessage();
    
    FiringCounts getFiringCounts();
    
    void printMatchesOnFailure();
}

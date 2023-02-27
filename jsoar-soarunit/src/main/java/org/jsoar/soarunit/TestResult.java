/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 23, 2010
 */
package org.jsoar.soarunit;

/**
 * @author ray
 */
public class TestResult
{
    private final Test test;
    private final long initNanos;
    private final long runNanos;
    private final boolean passed;
    private final String message;
    private final String output;
    private final FiringCounts firingCounts;
    
    public TestResult(Test test, long initNanos, long nanos, boolean passed, String message, String output, FiringCounts firingCounts)
    {
        this.test = test;
        this.initNanos = initNanos;
        this.runNanos = nanos;
        this.passed = passed;
        this.message = message;
        this.output = output;
        this.firingCounts = firingCounts;
    }
    
    /**
     * @return the test
     */
    public Test getTest()
    {
        return test;
    }
    
    /**
     * @return the passed
     */
    public boolean isPassed()
    {
        return passed;
    }
    
    /**
     * @return the message
     */
    public String getMessage()
    {
        return message;
    }
    
    /**
     * @return the output
     */
    public String getOutput()
    {
        return output;
    }
    
    /**
     * @return the firingCounts
     */
    public FiringCounts getFiringCounts()
    {
        return firingCounts;
    }
    
    public double getInitTimeInSeconds()
    {
        return initNanos / 1000000000.0;
    }
    
    public double getRunTimeInSeconds()
    {
        return runNanos / 1000000000.0;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return String.format("%s (%.3f s + %.3f s)", test.toString(), getInitTimeInSeconds(), getRunTimeInSeconds());
    }
}

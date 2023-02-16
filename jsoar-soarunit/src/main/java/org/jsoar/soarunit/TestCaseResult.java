/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 23, 2010
 */
package org.jsoar.soarunit;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ray
 */
public class TestCaseResult
{
    private final TestCase testCase;
    private final List<TestResult> results = new ArrayList<>();
    private int passed = 0;
    private int failed = 0;
    private final FiringCounts firingCounts = new FiringCounts();
    
    public TestCaseResult(TestCase testCase)
    {
        this.testCase = testCase;
    }
    
    /**
     * @return the parent test case
     */
    public TestCase getTestCase()
    {
        return testCase;
    }
    
    public void addTestResult(TestResult testResult)
    {
        results.add(testResult);
        if(testResult.isPassed())
        {
            passed++;
        }
        else
        {
            failed++;
        }
        firingCounts.merge(testResult.getFiringCounts());
    }
    
    public List<TestResult> getTestResults()
    {
        return results;
    }
    
    /**
     * @return the passed
     */
    public int getPassed()
    {
        return passed;
    }
    
    /**
     * @return the failed
     */
    public int getFailed()
    {
        return failed;
    }
    
    /**
     * @return the firingCounts
     */
    public FiringCounts getFiringCounts()
    {
        return firingCounts;
    }
}

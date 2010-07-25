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
public class TestSuiteResult
{
    private final TestSuite suite;
    private final List<TestResult> results = new ArrayList<TestResult>();
    private int passed = 0;
    private int failed = 0;
    
    public TestSuiteResult(TestSuite suite)
    {
        this.suite = suite;
    }
    
    

    /**
     * @return the suite
     */
    public TestSuite getSuite()
    {
        return suite;
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
    
    
}

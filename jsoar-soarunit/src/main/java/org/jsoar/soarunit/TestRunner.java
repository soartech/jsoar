/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 27, 2010
 */
package org.jsoar.soarunit;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.SoarException;

/**
 * @author ray
 */
public class TestRunner
{
    private final PrintWriter out;
    private int index;
    private int total;
    private boolean haltOnFailure;
    private FiringCounts firingCounts = new FiringCounts();
    private final TestAgentFactory factory;
    
    public TestRunner(TestAgentFactory factory, PrintWriter out)
    {
        this.factory = factory;
        this.out = out;
    }

    /**
     * @return the total
     */
    public int getTotal()
    {
        return total;
    }

    /**
     * @param total the total to set
     */
    public void setTotal(int total)
    {
        this.total = total;
    }

    /**
     * @return the haltOnFailure
     */
    public boolean isHaltOnFailure()
    {
        return haltOnFailure;
    }

    /**
     * @param haltOnFailure the haltOnFailure to set
     */
    public void setHaltOnFailure(boolean haltOnFailure)
    {
        this.haltOnFailure = haltOnFailure;
    }

    /**
     * @return the firingCounts
     */
    public FiringCounts getFiringCounts()
    {
        return firingCounts;
    }

    public List<TestCaseResult> runAllTestCases(final List<TestCase> all) throws SoarException
    {
        index = 0;
        total = all.size();
        haltOnFailure = true;
        firingCounts = new FiringCounts();
        
        final List<TestCaseResult> results = new ArrayList<TestCaseResult>();
        for(TestCase testCase : all)
        {
            final TestCaseResult result = run(testCase);
            results.add(result);
            if(result.getFailed() > 0)
            {
                break;
            }
        }
        return results;
    }
    
    public TestCaseResult run(TestCase testCase) throws SoarException
    {
        index++;
        out.printf("%d/%d: Running test case '%s' from '%s'%n", index, total, 
                            testCase.getName(), 
                            testCase.getFile());
        final TestCaseResult result = new TestCaseResult(testCase);
        for(Test test : testCase.getTests())
        {
        	final long initStartTime = System.nanoTime();
            final TestAgent agent = factory.createTestAgent();
            out.printf("   Created agent in %f seconds\n", (System.nanoTime() - initStartTime) / 1000000.0);
            try
            {
                final TestResult testResult;
                try
                {
                    testResult = runTest(test, agent);
                }
                catch (SoarException e)
                {
                    throw new SoarException(testCase.getFile() + ":" + testCase.getName() + ": " + e.getMessage(), e);
                }
                result.addTestResult(testResult);
                if(haltOnFailure && !testResult.isPassed())
                {
                    break;
                }
            }
            finally
            {
                agent.dispose();
            }
        }
        
        firingCounts.merge(result.getFiringCounts());
        
        return result;
    }
    
    public void debugTest(Test test, boolean exitOnClose) throws SoarException, InterruptedException
    {
        factory.debugTest(test, exitOnClose);
    }
    
    
    private TestResult runTest(Test test, final TestAgent agent) throws SoarException
    {
        out.printf("Running test: %s%n", test.getName());
        
        final long startInitTimeNanos = System.nanoTime();
        agent.initialize(test);
        final long startRunTimeNanos = System.nanoTime();
        final long elapsedInitTimeNanos = startRunTimeNanos - startInitTimeNanos;
        agent.run();
        final long elapsedNanos = System.nanoTime() - startRunTimeNanos; 
        
        final FiringCounts firingCounts = agent.getFiringCounts();
        
        if(agent.isFailCalled())
        {
            agent.printMatchesOnFailure();
            return new TestResult(test, elapsedInitTimeNanos, elapsedNanos, false, 
                              agent.getFailMessage(),
                              agent.getOutput(),
                              firingCounts);
        }
        else if(!agent.isPassCalled())
        {
            agent.printMatchesOnFailure();
            final long actualCycles = agent.getCycleCount();
            return new TestResult(test, elapsedInitTimeNanos, elapsedNanos, false, 
                    String.format("never called (pass) function. Ran %d decisions.", actualCycles),
                              agent.getOutput(),
                              firingCounts);
        }
        else
        {
            return new TestResult(test, elapsedInitTimeNanos, elapsedNanos, true,
                    agent.getPassMessage(), 
                     "",
                     firingCounts);
        }
    }
}

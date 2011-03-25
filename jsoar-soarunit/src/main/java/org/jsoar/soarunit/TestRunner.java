/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 27, 2010
 */
package org.jsoar.soarunit;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.jsoar.kernel.SoarException;

/**
 * @author ray
 */
public class TestRunner
{
    private final PrintWriter out;
    private int total;
    private boolean haltOnFailure = true;
    private FiringCounts firingCounts = new FiringCounts();
    private final TestAgentFactory factory;
    private final ExecutorService executor;
    
    public TestRunner(TestAgentFactory factory, PrintWriter out, ExecutorService executor)
    {
        this.factory = factory;
        this.out = out;
        
        this.executor = executor != null ? executor : Executors.newSingleThreadExecutor();
    }

    /**
     * @return the total
     */
    public int getTotal()
    {
        return total;
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

    public List<TestCaseResult> runAllTestCases(final List<TestCase> all, TestCaseResultHandler handler) throws SoarException
    {
        total = all.size();
        firingCounts = new FiringCounts();

        final long startTime = System.nanoTime();
        int index = 0;
        final List<Callable<TestCaseResult>> tasks = new ArrayList<Callable<TestCaseResult>>();
        for(TestCase testCase : all)
        {
            tasks.add(createTestCaseRunner(testCase, handler, ++index));
        }
        final List<TestCaseResult> results = new ArrayList<TestCaseResult>();
        try
        {
            final List<Future<TestCaseResult>> futures = executor.invokeAll(tasks);
            for(Future<TestCaseResult> future : futures) 
            {
                final TestCaseResult result = future.get();
                firingCounts.merge(result.getFiringCounts());
                results.add(result);
            }
        }
        catch (InterruptedException e)
        {
            throw new SoarException(e.getMessage(), e);
        }
        catch (ExecutionException e)
        {
            throw new SoarException(e.getMessage(), e);
        }
        final long elapsedTime = System.nanoTime() - startTime;
        out.printf("Ran %d tests in %f s\n", TestCase.getTotalTests(all), elapsedTime / 1000000000.0);
        System.out.printf("Ran %d tests in %f s\n", TestCase.getTotalTests(all), elapsedTime / 1000000000.0);
        return results;
        
        /*
        final long startTime = System.nanoTime();
        int index = 0;
        final List<TestCaseResult> results = new ArrayList<TestCaseResult>();
        for(TestCase testCase : all)
        {
            try
            {
                final TestCaseResult result = createTestCaseRunner(testCase, handler, ++index).call();
                firingCounts.merge(result.getFiringCounts());
                results.add(result);
                
                if(haltOnFailure && result.getFailed() > 0)
                {
                    break;
                }
            }
            catch (Exception e)
            {
                throw new SoarException(e.getMessage(), e);
            }
        }
        final long elapsedTime = System.nanoTime() - startTime;
        out.printf("Ran %d test cases in %f s\n", total, elapsedTime / 1000000000.0);
        return results;
        */
       
    }
    
    private Callable<TestCaseResult> createTestCaseRunner(final TestCase testCase, final TestCaseResultHandler handler, final int index)
    {
        return new Callable<TestCaseResult>()
        {
            @Override
            public TestCaseResult call() throws Exception
            {
                final TestCaseResult result = run(testCase, index);
                if(handler != null) 
                {
                    handler.handleTestCaseResult(result);
                }
                return result;
            }
        };
    }
   
    private TestCaseResult run(TestCase testCase, int index) throws SoarException
    {
        out.printf("%d/%d: Running test case '%s' from '%s'%n", index, total, 
                            testCase.getName(), 
                            testCase.getFile());
        final TestCaseResult result = new TestCaseResult(testCase);
        for(Test test : testCase.getTests())
        {
            final TestAgent agent = factory.createTestAgent();
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
        return result;
    }
    
    public void debugTest(Test test, boolean exitOnClose) throws SoarException, InterruptedException
    {
        factory.debugTest(test, exitOnClose);
    }
    
    
    private TestResult runTest(Test test, final TestAgent agent) throws SoarException
    {
        out.printf("   Running test: '%s/%s' on thread %s%n", test.getTestCase().getName(), test.getName(), Thread.currentThread().getName());
        
        final long startInitTimeNanos = System.nanoTime();
        agent.initialize(test);
        final long startRunTimeNanos = System.nanoTime();
        final long elapsedInitTimeNanos = startRunTimeNanos - startInitTimeNanos;
        agent.run();
        final long elapsedNanos = System.nanoTime() - startRunTimeNanos; 
        out.printf("      finished in %f seconds\n", elapsedNanos / 1000000000.0);
       
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
                     agent.getOutput(),
                     firingCounts);
        }
    }
}

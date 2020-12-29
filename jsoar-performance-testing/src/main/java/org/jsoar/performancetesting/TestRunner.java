/**
 * 
 */
package org.jsoar.performancetesting;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.SoarException;

/**
 * A class for running tests. This can run both CSoar and JSoar tests and
 * doesn't make a difference.
 * 
 * @author ALT
 *
 */
public class TestRunner
{
    private final PrintWriter out;

    private Test test;

    private List<Double> cpuTimes = new ArrayList<>();

    private List<Double> kernelTimes = new ArrayList<>();

    private List<Double> decisionCycles = new ArrayList<>();

    private List<Double> memoryLoads = new ArrayList<>();

    public TestRunner(Test test, PrintWriter out)
    {
        this.test = test;
        this.out = out;
    }

    /**
     * This runs a test a single iterator and records all the statistics.
     * 
     * @param runCount
     * @param seed
     * @return Whether the run was successful
     * @throws SoarException
     */
    public boolean runSingleIteration(int runCount) throws SoarException
    {
        test.reset();

        boolean result = test.run(runCount);

        cpuTimes.add(test.getCPURunTime());
        kernelTimes.add(test.getKernelRunTime());

        decisionCycles.add(Double.valueOf(test.getDecisionCyclesRunFor()));

        memoryLoads.add(Double.valueOf(test.getMemoryForRun()));

        return result;
    }

    /**
     * Runs a test for a passed runCount and for each JSoar test, a passed
     * warmUpCount. Also sets the seed of the test from the passed parameter.
     * 
     * @param runCount
     * @param warmUpCount
     * @param seed
     * @return Whether running all the tests was successful or not.
     * @throws SoarException
     */
    boolean runTestsForAverage(TestSettings settings) throws SoarException
    {
        if (settings.isJsoarEnabled() && settings.getWarmUpCount() > 0)
        {
            out.print("Warming Up: ");
            out.flush();

            for (int i = 0; i < settings.getWarmUpCount(); i++)
            {
                test.reset();

                boolean result = test.run(i);

                if (!result)
                    return false;

                out.print(".");
                out.flush();
            }

            out.print("\n");
        }

        out.print("Running Test: ");
        out.flush();

        for (int i = 0; i < settings.getRunCount(); i++)
        {
            boolean result = runSingleIteration(i);

            if (!result)
                return false;

            out.print(".");
            out.flush();
        }

        out.print("\n");
        out.flush();

        return true;
    }

    /**
     * 
     * @return the total CPU time for all the runs.
     */
    public double getTotalCPUTime()
    {
        return Statistics.calculateTotal(cpuTimes);
    }

    /**
     * 
     * @return the total kernel time for all the runs.
     */
    public double getTotalKernelTime()
    {
        return Statistics.calculateTotal(kernelTimes);
    }

    /**
     * 
     * @return the total decision cycles run for, for all the runs.
     */
    public double getTotalDecisionCycles()
    {
        return Statistics.calculateTotal(decisionCycles);
    }

    /**
     * 
     * @return the total memory load for all the runs.
     */
    public double getTotalMemoryLoad()
    {
        return Statistics.calculateTotal(memoryLoads);
    }

    /**
     * 
     * @return the average cpu time for all the runs.
     */
    public double getAverageCPUTime()
    {
        return Statistics.calculateAverage(cpuTimes);
    }

    /**
     * 
     * @return the median cpu time for all the runs.
     */
    public double getMedianCPUTime()
    {
        return Statistics.calculateMedian(cpuTimes);
    }

    /**
     * 
     * @return the average kernel time for all the runs.
     */
    public double getAverageKernelTime()
    {
        return Statistics.calculateAverage(kernelTimes);
    }

    /**
     * 
     * @return the median kernel time for all the runs.
     */
    public double getMedianKernelTime()
    {
        return Statistics.calculateMedian(kernelTimes);
    }

    /**
     * 
     * @return the average decision cycles over all the runs.
     */
    public double getAverageDecisionCycles()
    {
        return Statistics.calculateAverage(decisionCycles);
    }

    /**
     * 
     * @return the median decision cycles over all the runs.
     */
    public double getMedianDecisionCycles()
    {
        return Statistics.calculateMedian(decisionCycles);
    }

    /**
     * 
     * @return the average memory load for all the runs.
     */
    public double getAverageMemoryLoad()
    {
        return Statistics.calculateAverage(memoryLoads);
    }

    /**
     * 
     * @return the median memory load for all the runs.
     */
    public double getMedianMemoryLoad()
    {
        return Statistics.calculateMedian(memoryLoads);
    }

    /**
     * 
     * @return the total memory load deviation.
     */
    public double getMemoryLoadDeviation()
    {
        return Statistics.calculateDeviation(memoryLoads);
    }

    /**
     * 
     * @return the test this test runner was running.
     */
    public Test getTest()
    {
        return test;
    }

    /**
     * 
     * @return all the cpu times for the runs.
     */
    public List<Double> getAllCPUTimes()
    {
        return cpuTimes;
    }

    /**
     * 
     * @return all the kernel times for the runs.
     */
    public List<Double> getAllKernelTimes()
    {
        return kernelTimes;
    }

    /**
     * 
     * @return all the decision cycle counts for the runs.
     */
    public List<Double> getAllDecisionCycles()
    {
        return decisionCycles;
    }

    /**
     * 
     * @return all the memory loads for the runs.
     */
    public List<Double> getAllMemoryLoads()
    {
        return memoryLoads;
    }
}

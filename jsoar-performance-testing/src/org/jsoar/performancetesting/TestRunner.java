/**
 * 
 */
package org.jsoar.performancetesting;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jsoar.kernel.SoarException;

/**
 * @author ALT
 *
 */
public class TestRunner
{
    private final PrintWriter out;
    private Test test;
    
    private List<Double> cpuTimes;
    private List<Double> kernelTimes;
    private List<Integer> decisionCycles;
    private List<Long> memoryLoads;
    
    private int runs;
    
    public TestRunner(Test test, PrintWriter out)
    {
        this.test = test;
        this.out = out;
        
        cpuTimes = new ArrayList<Double>();
        kernelTimes = new ArrayList<Double>();
        decisionCycles = new ArrayList<Integer>();
        memoryLoads = new ArrayList<Long>();
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
        
        decisionCycles.add(test.getDecisionCyclesRunFor());
        
        memoryLoads.add(test.getMemoryForRun());
        
        return result;
    }

    /**
     * Runs a test for a passed runCount and for each JSoar test, a passed
     * warmUpCount.  Also sets the seed of the test from the passed parameter.
     * 
     * @param runCount
     * @param warmUpCount
     * @param seed
     * @return Whether running all the tests was successful or not.
     * @throws SoarException
     */
    boolean runTestsForAverage(TestSettings settings) throws SoarException
    {
        if (settings.isJSoarEnabled() && settings.getWarmUpCount() > 0)
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
        
        runs = settings.getRunCount();
        
        out.print("Running Test: ");
        out.flush();
        
        for (int i = 0;i < settings.getRunCount();i++)
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
        double averageCPUTime = 0.0;
        
        for (Double d : cpuTimes)
        {
            averageCPUTime += d;
        }
                
        return averageCPUTime;
    }
    
    /**
     * 
     * @return the total kernel time for all the runs.
     */
    public double getTotalKernelTime()
    {
        double averageKernelTime = 0.0;
        
        for (Double d : kernelTimes)
        {
            averageKernelTime += d;
        }
                
        return averageKernelTime;
    }
    
    /**
     * 
     * @return the total decision cycles run for, for all the runs.
     */
    public double getTotalDecisionCycles()
    {
        double averageDecisionCycles = 0.0;
        
        for (Integer i : decisionCycles)
        {
            averageDecisionCycles += i;
        }
                
        return averageDecisionCycles;
    }
    
    /**
     * 
     * @return the total memory load for all the runs.
     */
    public double getTotalMemoryLoad()
    {
        double averageMemoryLoad = 0.0;
        
        for (Long l : memoryLoads)
        {
            averageMemoryLoad += l;
        }
                
        return averageMemoryLoad;
    }
    
    /**
     * 
     * @return the average cpu time for all the runs.
     */
    public double getAverageCPUTime()
    {
        double averageCPUTime = 0.0;
        
        for (Double d : cpuTimes)
        {
            averageCPUTime += d;
        }
        
        averageCPUTime /= runs;
        
        return averageCPUTime;
    }
    
    /**
     * 
     * @return the median cpu time for all the runs.
     */
    public double getMedianCPUTime()
    {
        List<Double> cpuTimesSorted = new ArrayList<Double>(cpuTimes);
        
        Collections.sort(cpuTimesSorted);
        
        int size = cpuTimesSorted.size();
        
        if (size % 2 == 0)
        {
            //Even
            return cpuTimesSorted.get(size/2);
        }
        else
        {
            //Odd
            int index_top = (int) Math.floor(size/2.0);
            int index_bottom = (int) Math.nextUp(size/2.0);
            
            double median = cpuTimesSorted.get(index_bottom) + cpuTimesSorted.get(index_top);
            
            median /= 2.0;
            
            return median;
        }
    }
    
    /**
     * 
     * @return the average kernel time for all the runs.
     */
    public double getAverageKernelTime()
    {
        double averageKernelTime = 0.0;
        
        for (Double d : kernelTimes)
        {
            averageKernelTime += d;
        }
        
        averageKernelTime /= runs;
        
        return averageKernelTime;
    }
    
    /**
     * 
     * @return the median kernel time for all the runs.
     */
    public double getMedianKernelTime()
    {
        List<Double> kernelTimesSorted = new ArrayList<Double>(kernelTimes);
        
        Collections.sort(kernelTimesSorted);
        
        int size = kernelTimesSorted.size();
        
        if (size % 2 == 0)
        {
            //Even
            return kernelTimesSorted.get(size/2);
        }
        else
        {
            //Odd
            int index_top = (int) Math.floor(size/2.0);
            int index_bottom = (int) Math.nextUp(size/2.0);
            
            double median = kernelTimesSorted.get(index_bottom) + kernelTimesSorted.get(index_top);
            
            median /= 2.0;
            
            return median;
        }
    }
    
    /**
     * 
     * @return the average decision cycles over all the runs.
     */
    public double getAverageDecisionCycles()
    {
        double averageDecisionCycles = 0.0;
        
        for (Integer i : decisionCycles)
        {
            averageDecisionCycles += i;
        }
        
        averageDecisionCycles /= runs;
        
        return averageDecisionCycles;
    }
    
    /**
     * 
     * @return the median decision cycles over all the runs.
     */
    public double getMedianDecisionCycles()
    {
        List<Integer> decisionCyclesSorted = new ArrayList<Integer>(decisionCycles);
        
        Collections.sort(decisionCyclesSorted);
        
        int size = decisionCyclesSorted.size();
        
        if (size % 2 == 0)
        {
            //Even
            return decisionCyclesSorted.get(size/2);
        }
        else
        {
            //Odd
            int index_top = (int) Math.floor(size/2.0);
            int index_bottom = (int) Math.nextUp(size/2.0);
            
            double median = decisionCyclesSorted.get(index_bottom) + decisionCyclesSorted.get(index_top);
            
            median /= 2.0;
            
            return median;
        }
    }
    
    /**
     * 
     * @return the average memory load for all the runs.
     */
    public double getAverageMemoryLoad()
    {
        double averageMemoryLoad = 0.0;
        
        for (Long l : memoryLoads)
        {
            averageMemoryLoad += l;
        }
        
        averageMemoryLoad /= runs;
        
        return averageMemoryLoad;
    }
    
    /**
     * 
     * @return the median memory load for all the runs.
     */
    public double getMedianMemoryLoad()
    {
        List<Long> memoryLoadsSorted = new ArrayList<Long>(memoryLoads);
        
        Collections.sort(memoryLoadsSorted);
        
        int size = memoryLoadsSorted.size();
        
        if (size % 2 == 0)
        {
            //Even
            return memoryLoadsSorted.get(size/2);
        }
        else
        {
            //Odd
            int index_top = (int) Math.floor(size/2.0);
            int index_bottom = (int) Math.nextUp(size/2.0);
            
            double median = memoryLoadsSorted.get(index_bottom) + memoryLoadsSorted.get(index_top);
            
            median /= 2.0;
            
            return median;
        }
    }
    
    /**
     * 
     * @return the total memory load deviation.
     */
    public double getMemoryLoadDeviation()
    {        
        return Collections.max(memoryLoads) - getAverageMemoryLoad();
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
    public List<Integer> getAllDecisionCycles()
    {
        return decisionCycles;
    }
    
    /**
     * 
     * @return all the memory loads for the runs.
     */
    public List<Long> getAllMemoryLoads()
    {
        return memoryLoads;
    }
}

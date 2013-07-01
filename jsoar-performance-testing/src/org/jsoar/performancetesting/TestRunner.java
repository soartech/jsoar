/**
 * 
 */
package org.jsoar.performancetesting;

import java.io.PrintWriter;
import java.util.ArrayList;
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
    
    public boolean runSingleIteration() throws SoarException
    {
        test.reset();
        
        boolean result = test.run();
        
        cpuTimes.add(test.getCPURunTime());
        kernelTimes.add(test.getKernelRunTime());
        
        decisionCycles.add(test.getDecisionCyclesRunFor());
        
        memoryLoads.add(test.getMemoryForRun());
        
        return result;
    }
    
    public boolean runTestsForAverage() throws SoarException
    {
        return runTestsForAverage(20, 10);
    }
    
    public boolean runTestsForAverage(int runCount, int warmUpCount) throws SoarException
    {
        if (runCount < (warmUpCount + 1))
            return false;
        
        out.print("Warming Up: ");
        out.flush();
        
        for (int i = 0;i < warmUpCount;i++)
        {
            test.reset();
            
            boolean result = test.run();
            
            if (!result)
                return false;
            
            out.print(".");
            out.flush();
        }
        
        runs = runCount - warmUpCount;
        
        out.print("\n" +
                  "Running Test: ");
        out.flush();
        
        for (int i = warmUpCount;i < runCount;i++)
        {
            boolean result = runSingleIteration();
            
            if (!result)
                return false;
            
            out.print(".");
            out.flush();
        }
        
        out.print("\n");
        out.flush();
        
        return true;
    }
    
    public double getTotalCPUTime()
    {
        double averageCPUTime = 0.0;
        
        for (Double d : cpuTimes)
        {
            averageCPUTime += d;
        }
                
        return averageCPUTime;
    }
    
    public double getTotalKernelTime()
    {
        double averageKernelTime = 0.0;
        
        for (Double d : kernelTimes)
        {
            averageKernelTime += d;
        }
                
        return averageKernelTime;
    }
    
    public double getTotalDecisionCycles()
    {
        double averageDecisionCycles = 0.0;
        
        for (Integer i : decisionCycles)
        {
            averageDecisionCycles += i;
        }
                
        return averageDecisionCycles;
    }
    
    public double getTotalMemoryLoad()
    {
        double averageMemoryLoad = 0.0;
        
        for (Long l : memoryLoads)
        {
            averageMemoryLoad += l;
        }
                
        return averageMemoryLoad;
    }
    
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
    
    public Test getTest()
    {
        return test;
    }
}

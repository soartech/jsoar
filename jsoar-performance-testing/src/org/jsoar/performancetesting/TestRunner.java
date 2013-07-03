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
    
    public boolean runSingleIteration(int runCount, Long seed) throws SoarException
    {
        test.reset();
        
        boolean result = test.run(runCount, seed);
        
        cpuTimes.add(test.getCPURunTime());
        kernelTimes.add(test.getKernelRunTime());
        
        decisionCycles.add(test.getDecisionCyclesRunFor());
        
        memoryLoads.add(test.getMemoryForRun());
        
        return result;
    }
    
    public boolean runTestsForAverage(Long seed) throws SoarException
    {
        return runTestsForAverage(20, 10, seed);
    }
    
    public boolean runTestsForAverage(int runCount, int warmUpCount, Long seed) throws SoarException
    {
        if (warmUpCount > 0)
        {
            out.print("Warming Up: ");
            out.flush();

            for (int i = 0; i < warmUpCount; i++)
            {
                test.reset();

                boolean result = test.run(i, seed);

                if (!result)
                    return false;

                out.print(".");
                out.flush();
            }
            
            out.print("\n");
        }
        
        runs = runCount;
        
        out.print("Running Test: ");
        out.flush();
        
        for (int i = 0;i < runCount;i++)
        {
            boolean result = runSingleIteration(i, seed);
            
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
    
    public double getMeanMemoryLoad()
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
            
            double mean = memoryLoadsSorted.get(index_bottom) + memoryLoadsSorted.get(index_top);
            
            mean /= 2.0;
            
            return mean;
        }
    }
    
    public double getMemoryLoadDeviation()
    {        
        return Collections.max(memoryLoads) - getAverageMemoryLoad();
    }
    
    public Test getTest()
    {
        return test;
    }
    
    public List<Double> getAllCPUTimes()
    {
        return cpuTimes;
    }
    
    public List<Double> getAllKernelTimes()
    {
        return kernelTimes;
    }
    
    public List<Integer> getAllDecisionCycles()
    {
        return decisionCycles;
    }
    
    public List<Long> getAllMemoryLoads()
    {
        return memoryLoads;
    }
}

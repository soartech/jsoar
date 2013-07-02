/**
 * 
 */
package org.jsoar.performancetesting.csoar;

import java.util.Arrays;
import java.util.List;

import org.jsoar.performancetesting.Test;

/**
 * @author ALT
 * 
 */
public class CSoarTest implements Test
{
    private String testName;

    private String testFile;

    private CSoarAgentWrapper agent;

    private CSoarKernelFactory kernelFactory;
    private CSoarKernelWrapper kernel;

    private Double cpuTime;

    private Double kernelTime;

    private int decisionsRunFor;
    
    private Integer decisionCyclesToRun;

    private long memoryForRun;

    public CSoarTest(String label, String csoarDirectory)
    {
        this.agent = null;
        this.kernel = null;

        this.cpuTime = -1.0;
        this.kernelTime = -1.0;
        this.decisionsRunFor = -1;
        this.memoryForRun = -1;
        
        this.kernelFactory = new CSoarKernelFactory(label, csoarDirectory);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.performancetesting.Test#initialize(java.lang.String,
     * java.lang.String)
     */
    @Override
    public void initialize(String testName, String testFile, Integer decisionCycles)
    {
        this.testName = testName;
        this.testFile = testFile;
        this.decisionCyclesToRun = decisionCycles;

        kernel = kernelFactory.CreateKernelInCurrentThread(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.performancetesting.Test#getTestName()
     */
    @Override
    public String getTestName()
    {
        return testName;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.performancetesting.Test#getTestFile()
     */
    @Override
    public String getTestFile()
    {
        return testFile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.performancetesting.Test#run()
     */
    @Override
    public boolean run(int runCount)
    {
        agent = kernel.CreateAgent("CSoar Performance Testing Agent - " + testName + " - " + runCount);

        if (agent.LoadProductions(testFile) == false)
        {
            System.out.println("\n" + "ERROR: Failed to load " + testFile);
            return false;
        }

        if (decisionCyclesToRun == 0)
            agent.RunSelfForever();
        else
            agent.RunSelf(decisionCyclesToRun);
        
        cpuTime = getCPUTime();
        kernelTime = getKernelTime();

        decisionsRunFor = getDecisions();

        // JSoar will always be more memory than CSoar and there is no easy way
        // to
        // measure CSoar memory since there is no built in command to CSoar and
        // getting the runtime memory will also include the JVM therefore just
        // return 0.
        memoryForRun = 0;

        agent = null;
        
        System.gc();
        System.gc();

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.performancetesting.Test#reset()
     */
    @Override
    public boolean reset()
    {
        if (agent != null)
        {
            agent = null;
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.performancetesting.Test#getCPURunTime()
     */
    @Override
    public double getCPURunTime()
    {
        return cpuTime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.performancetesting.Test#getKernelRunTime()
     */
    @Override
    public double getKernelRunTime()
    {
        return kernelTime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.performancetesting.Test#getDecisionCyclesRunFor()
     */
    @Override
    public int getDecisionCyclesRunFor()
    {
        return decisionsRunFor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.performancetesting.Test#getMemoryForRun()
     */
    @Override
    public long getMemoryForRun()
    {
        return memoryForRun;
    }

    private double getKernelTime()
    {
        if (agent == null)
            return -1.0;

        String result = agent.ExecuteCommandLine("stats");

        List<String> splitString = Arrays.asList(result.split("\\s+"));

        for (int i = 0; i < splitString.size(); i++)
        {
            String s = splitString.get(i);

            if (s == null)
                return -1.0;

            if (s.equals("Kernel") && splitString.size() >= (i + 4))
            {
                String next = splitString.get(i + 1);

                if (next.equals("CPU"))
                {
                    // It is our match

                    String time = splitString.get(i + 3);

                    return Double.parseDouble(time);
                }
            }
        }

        return -1.0;
    }

    private double getCPUTime()
    {
        if (agent == null)
            return -1.0;

        String result = agent.ExecuteCommandLine("stats");

        List<String> splitString = Arrays.asList(result.split("\\s+"));

        for (int i = 0; i < splitString.size(); i++)
        {
            String s = splitString.get(i);

            if (s == null)
                return -1.0;

            if (s.equals("Total") && splitString.size() >= (i + 4))
            {
                String next = splitString.get(i + 1);

                if (next.equals("CPU"))
                {
                    // It is our match

                    String time = splitString.get(i + 3);

                    return Double.parseDouble(time);
                }
            }
        }

        return -1.0;
    }

    private int getDecisions()
    {
        if (agent == null)
            return -1;

        String result = agent.ExecuteCommandLine("stats");

        List<String> splitString = Arrays.asList(result.split("\\s+"));

        for (int i = 0; i < splitString.size(); i++)
        {
            String s = splitString.get(i);

            if (s == null)
                return -1;

            if (s.equals("decisions") && i > 0)
            {
                // The number we want is the previous string

                String decisions = splitString.get(i - 1);

                return Integer.parseInt(decisions);
            }
        }

        return -1;
    }
}

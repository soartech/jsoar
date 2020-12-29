/**
 * 
 */
package org.jsoar.performancetesting.csoar;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jsoar.performancetesting.Test;
import org.jsoar.performancetesting.TestSettings;

/**
 * A CSoar Test. This class is used to launch and run all CSoar tests.
 * 
 * @author ALT
 * 
 */
public class CSoarTest implements Test
{
    private String testName;

    private Path testFile;

    private TestSettings settings = null;

    private CSoarAgentWrapper agent;

    private CSoarKernelFactory kernelFactory;

    private CSoarKernelWrapper kernel;

    private Double cpuTime;

    private Double kernelTime;

    private int decisionsRunFor;

    private long memoryForRun;

    public CSoarTest(String label, Path csoarDirectory)
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
    public void initialize(String testName, Path testFile,
            TestSettings settings)
    {
        this.testName = testName;
        this.testFile = testFile;
        this.settings = settings;

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
    public Path getTestFile()
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
        agent = kernel.CreateAgent("CSoar Performance Testing Agent - "
                + testName + " - " + runCount);

        if (agent.LoadProductions(testFile) == false)
        {
            agent.ExecuteCommandLine("excise --all");
            System.err.println(agent.ExecuteCommandLine("source " + testFile));
            System.err.println("\n" + "ERROR: Failed to load " + testFile);
            return false;
        }

        if (settings.isUsingSeed())
        {
            agent.ExecuteCommandLine("srand " + settings.getSeed());
        }

        agent.ExecuteCommandLine("soar stop-phase output");

        if (settings.getDecisionCycles().size() == 0
                || settings.getDecisionCycles().get(0) == 0)
            agent.RunSelfForever();
        else
            agent.RunSelf(settings.getDecisionCycles().get(0));

        cpuTime = getCPUTime();
        kernelTime = getKernelTime();

        decisionsRunFor = getDecisions();

        // JSoar will always be more memory than CSoar and there is no easy way
        // to
        // measure CSoar memory since there is no built in command to CSoar and
        // getting the runtime memory will also include the JVM therefore just
        // return 0.
        memoryForRun = getMemory();

        agent = null;
        kernel.Shutdown();

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

    /**
     * 
     * @return the memory via an allocate command.
     */
    private long getMemory()
    {
        if (agent == null)
            return 0;

        String result = agent.ExecuteCommandLine("allocate");

        /*
         * Example Allocate Command Result
         * 
         * Memory pool statistics:
         * 
         * Pool Name Item Size Itm/Blk Blocks Total Bytes ---------------
         * --------- ------- ------ ----------- dynamic 64 511 1 32704
         * epmem_interval 32 1023 1 32736 epmem_uedges 56 584 1 32704
         * epmem_pedges 64 511 1 32704 epmem_literals 144 227 1 32688
         * smem_id_data 40 818 1 32720 smem_wmes 40 818 1 32720 epmem_id_data 48
         * 682 1 32736 epmem_wmes 40 818 1 32720 wma_slot_ref 40 818 0 0
         * wma_oset 40 818 0 0 wma_decay_set 40 818 0 0 wma_decay 224 146 0 0
         * rl_rules 40 818 1 32720 rl_et 40 818 1 32720 rl_id_data 40 818 1
         * 32720 gds 16 2047 1 32752 chunk conditio 72 454 1 32688 io wme 40 818
         * 1 32720 output link 48 682 1 32736 preference 192 170 27 881280 wme
         * 200 163 28 912800 slot 216 151 1 32616 instantiation 104 314 10
         * 326560 ms change 96 341 1 32736 right mem 64 511 1 32704 token 112
         * 292 1 32704 node varnames 32 1023 1 32736 rete node 88 372 1 32736
         * rete test 24 1364 1 32736 alpha mem 80 409 1 32720 saved test 24 1364
         * 1 32736 not 24 1364 0 0 action 48 682 1 32736 production 168 194 1
         * 32592 condition 96 341 27 883872 complex test 16 2047 1 32752 float
         * constant 80 409 1 32720 int constant 80 409 4 130880 sym constant 88
         * 372 1 32736 identifier 376 87 18 588816 variable 112 292 1 32704
         * dynamic 24 1364 6 196416 dynamic 40 818 1 32720 dynamic 48 682 1
         * 32736 dl cons 24 1364 1 32736 cons cell 16 2047 2 65504
         */

        List<String> splitString = Arrays.asList(result.split("\\s+"));

        List<Long> numericsOnly = new ArrayList<Long>();

        for (int i = 0; i < splitString.size(); i++)
        {
            try
            {
                Long numeric = Long.parseLong(splitString.get(i));

                numericsOnly.add(numeric);
            }
            catch (NumberFormatException e)
            {
            } // Ignored
        }

        long memoryUsed = 0;

        int i = 3;
        // i is now at the number of bytes after the first 'dynamic' pool
        for (; i < numericsOnly.size(); i += 4)
        {
            memoryUsed += numericsOnly.get(i);
        }

        return memoryUsed;
    }

    /**
     * 
     * @return the kernel time via a stats command.
     */
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

    /**
     * 
     * @return the cpu time via a stats command.
     */
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

    /**
     * 
     * @return the number of decisions run so far via a stats command.
     */
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

    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.performancetesting.Test#getDisplayName()
     */
    @Override
    public String getDisplayName()
    {
        return "CSoar";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.performancetesting.Test#getTestSettings()
     */
    @Override
    public TestSettings getTestSettings()
    {
        return settings;
    }
}

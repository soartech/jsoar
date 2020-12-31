/**
 * 
 */
package org.jsoar.performancetesting.jsoar;

import java.net.MalformedURLException;
import java.nio.file.Path;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.performancetesting.Test;
import org.jsoar.performancetesting.yaml.TestSettings;
import org.jsoar.util.NullWriter;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.commands.SoarCommands;

/**
 * A JSoarTest. This class is used to launch and run all JSoar tests.
 * 
 * @author ALT
 *
 */
public class JSoarTest implements Test
{
    private String testName;

    private Path testFile;

    private Agent agent;

    private Double cpuTime;

    private Double kernelTime;

    private int decisionsRunFor;

    private long memoryForRun;

    private TestSettings settings;

    private JSoarAgentFactory agentFactory;
    
    /**
     * Sets all the values used in the test to be impossible values so we know
     * if something failed horribly.
     * @throws ClassNotFoundException 
     * @throws MalformedURLException 
     */
    public JSoarTest(Path jsoarCoreJar) throws MalformedURLException, ClassNotFoundException
    {
        this.agent = null;

        this.cpuTime = -1.0;
        this.kernelTime = -1.0;
        this.decisionsRunFor = -1;
        this.memoryForRun = -1;

        this.agentFactory = new JSoarAgentFactory(jsoarCoreJar);
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
    }

    @Override
    public Path getSoarPath() {
        return this.agentFactory.getSoarPath();
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
    public boolean run(int runCount) throws SoarException
    {
        // This is to make it very likely that the garbage collector has cleaned
        // up all references and freed memory
        // http://stackoverflow.com/questions/1481178/forcing-garbage-collection-in-java
        System.gc();
        System.gc();

        memoryForRun = Runtime.getRuntime().totalMemory()
                - Runtime.getRuntime().freeMemory();

        agent = (Agent) agentFactory
                .newAgent("JSoar Performance Testing Agent - " + testName
                        + " - " + runCount);
        agent.getTrace().setEnabled(false);
        agent.getPrinter().pushWriter(new NullWriter());
        agent.initialize();
        SoarCommandInterpreter ifc = agent.getInterpreter();
        
        SoarCommands.source(ifc, testFile);

        if (settings.isUsingSeed())
        {
            ifc.eval("srand " + settings.getSeed());
        }

        ifc.eval("soar stop-phase output");

        if (settings.getDecisionCycles().size() == 0
                || settings.getDecisionCycles().get(0) == 0)
            agent.runForever();
        else
            agent.runFor(settings.getDecisionCycles().get(0), RunType.DECISIONS);

        cpuTime = agent.getTotalCpuTimer().getTotalSeconds();
        kernelTime = agent.getTotalKernelTimer().getTotalSeconds();

        // This is the same counter as the stats command
        decisionsRunFor = agent.getProperties()
                .get(SoarProperties.DECISION_PHASES_COUNT).intValue();
        memoryForRun = Runtime.getRuntime().totalMemory()
                - Runtime.getRuntime().freeMemory() - memoryForRun;

        agent.dispose();

        agent = null;

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
            agent.dispose();
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

    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.performancetesting.Test#getSoarVariant()
     */
    @Override
    public String getSoarVariant()
    {
        return "JSoar";
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

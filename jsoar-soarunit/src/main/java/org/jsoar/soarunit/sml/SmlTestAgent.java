/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 28, 2010
 */
package org.jsoar.soarunit.sml;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoar.kernel.SoarException;
import org.jsoar.soarunit.FiringCounts;
import org.jsoar.soarunit.Test;
import org.jsoar.soarunit.TestAgent;
import org.jsoar.util.FileTools;

import sml.Agent;
import sml.Kernel;
import sml.smlPrintEventId;
import sml.Agent.PrintEventInterface;

/**
 * @author ray
 */
public class SmlTestAgent implements TestAgent, PrintEventInterface
{
    private Kernel kernel;
    private Agent agent;
    private TestRhsFunction passFunction;
    private TestRhsFunction failFunction;
    private final StringBuilder output = new StringBuilder();

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#dispose()
     */
    @Override
    public void dispose()
    {
        output.setLength(0);
        if(agent != null)
        {
            kernel.DestroyAgent(agent);
            agent = null;
        }
        if(kernel != null)
        {
            kernel.Shutdown();
            kernel = null;
        }
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#getCycleCount()
     */
    @Override
    public long getCycleCount()
    {
        return agent.GetDecisionCycleCounter();
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#getFailMessage()
     */
    @Override
    public String getFailMessage()
    {
        return failFunction.getArguments();
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#getFiringCounts()
     */
    @Override
    public FiringCounts getFiringCounts()
    {
        // TODO SoarUnit SML: use executeCommandLineXml to get list of firing counts
        try
        {
            return extractFiringCountsFromPrintedOutput(executeCommandLine("firing-counts", false));
        }
        catch (SoarException e)
        {
            e.printStackTrace();
            return new FiringCounts();
        }
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#getOutput()
     */
    @Override
    public String getOutput()
    {
        return output.toString();
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#getPassMessage()
     */
    @Override
    public String getPassMessage()
    {
        // TODO Auto-generated method stub
        return passFunction.getArguments();
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#initialize(org.jsoar.soarunit.Test)
     */
    @Override
    public void initialize(Test test) throws SoarException
    {
        output.setLength(0);

        kernel = Kernel.CreateKernelInCurrentThread();
        kernel.StopEventThread();

        initializeRhsFunctions();

        agent = kernel.CreateAgent(test.getName());
        agent.RegisterForPrintEvent(smlPrintEventId.smlEVENT_PRINT, this, null, false);

        loadTestCode(test);
    }

    public void debug(Test test, boolean exitOnClose) throws SoarException
    {
        kernel = Kernel.CreateKernelInNewThread();
        initializeRhsFunctions();

        agent = kernel.CreateAgent(test.getName());

        // TODO SoarUnit SML: If this fails, there's really no way to tell.
        // TODO SoarUnit SML: This requires that soar/bin be on the system path
        String soarHome = System.getProperty("soar.home", null);
        // TODO SoarUnit SML: library path has to end with a slash. See http://code.google.com/p/soar/issues/detail?id=82.
        if(soarHome != null && !soarHome.endsWith("\\") && !soarHome.endsWith("/"))
        {
            soarHome += File.separator;
        }
        soarHome += "bin" + File.separator + "SoarJavaDebugger.jar";

        System.out.println("launching debugger from: " + soarHome);
        boolean success = agent.SpawnDebugger(Kernel.kDefaultSMLPort, soarHome);
        if (success)
        {
            System.out.println("successfully launched debugger");
        }
        else
        {
            System.out.println("failed to launch debugger; check that SOAR_HOME is set properly");
        }

        loadTestCode(test);

        // TODO SoarUnit SML: How do we clean up? Detect debugger detach?
    }

    private void initializeRhsFunctions()
    {
        passFunction = TestRhsFunction.addTestFunction(kernel, "pass");
        failFunction = TestRhsFunction.addTestFunction(kernel, "fail");
    }


    private void loadTestCode(Test test) throws SoarException
    {
        executeCommandLine(String.format("pushd \"%s\"", FileTools.getParent(test.getTestCase().getFile()).replace('\\', '/')), true);
        executeCommandLine(prepSoarCodeForSml(test.getTestCase().getSetup()), true);
        executeCommandLine(prepSoarCodeForSml(test.getContent()), true);
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#isFailCalled()
     */
    @Override
    public boolean isFailCalled()
    {
        return failFunction.isCalled();
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#isPassCalled()
     */
    @Override
    public boolean isPassCalled()
    {
        return passFunction.isCalled();
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#printMatchesOnFailure()
     */
    @Override
    public void printMatchesOnFailure()
    {
        output.append("\n# Matches for pass rules #\n");
        // TODO use executeCommandLineXml to get list of user rules and print matches
        // output.append("\n" + executeCommandLine("matches pass"));
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgent#run()
     */
    @Override
    public void run()
    {
        agent.RunSelf(50000);
    }

    private String executeCommandLine(String code, boolean echo) throws SoarException
    {
        final String result = agent.ExecuteCommandLine(code, echo);
        if(!agent.GetLastCommandLineResult())
        {
            throw new SoarException(result);
        }
        return result;
    }

    private String prepSoarCodeForSml(String code)
    {
        return code.replace("(pass)", "(exec pass)"). // when there are no args
                    replace("(fail)", "(exec fail)").
                    replace("(pass ", "(exec pass "). // when there are args
                    replace("(fail ", "(exec fail ");
    }

    @Override
    public void printEventHandler(int eventID, Object data, Agent agent,
            String message)
    {
        output.append(message);
//        System.out.print(message);
//        System.out.flush();
    }

    static FiringCounts extractFiringCountsFromPrintedOutput(String in)
    {
        final FiringCounts result = new FiringCounts();
        final Pattern pattern = Pattern.compile("^\\s*(\\d+):\\s*(.*)$", Pattern.MULTILINE);
        final Matcher matcher = pattern.matcher(in);
        while(matcher.find())
        {
            result.adjust(matcher.group(2), Long.parseLong(matcher.group(1)));
        }
        return result;
    }
}

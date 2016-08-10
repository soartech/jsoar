/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 22, 2010
 */
package org.jsoar.soarunit;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.jsoar.kernel.SoarException;
import org.jsoar.soarunit.jsoar.JSoarTestAgentFactory;
import org.jsoar.soarunit.sml.SmlTestAgentFactory;
import org.jsoar.soarunit.ui.MainFrame;
import org.jsoar.util.commands.OptionProcessor;

import com.google.common.collect.Lists;

/**
 * @author ray
 */
public class SoarUnit
{
    private static enum Options { help, debug, Recursive, ui, sml, threads };
    
    private static final int DONT_EXIT = 255;
    
    private final PrintWriter out;
    private final boolean fromCommandLine;
    private TestAgentFactory agentFactory;
    
    public SoarUnit(PrintWriter out)
    {
        this(out, false);
    }

    private SoarUnit(PrintWriter out, boolean fromCommandLine)
    {
        this.out = out;
        this.fromCommandLine = fromCommandLine;
    }
    
    public void usage()
    {
        out.println("SoarUnit - Soar unit test framework\n" +
    		"soar-unit [options] [file and directories]\n" +
    		"\n" +
    		"Options:\n" +
    		"   -h, --help              This message.\n" +               
    		"   -R, --recursive         Recursively search directories for tests.\n" +
    		"   -d, --debug [test-name] Open the given test in a debugger.\n" +
    		"   -u, --ui                Show graphical user interface.\n" +
    		"   -s, --sml               Use CSoar/SML 9.3.0 instead of JSoar\n" +
    		"   -t, --threads           [1, n] for fixed thread pool, 'cpus' for number of cpus, 'cached' for cached\n" +
    		"\n" +
    		"Note: When running in SML mode, CSoar's bin directory must be on the system\n" +
    		"      path or in java.library.path.\n" + 
    		"");
    }
    
    public static void main(String[] args) throws Exception
    {
        final PrintWriter writer = new PrintWriter(System.out);
        final int result = new SoarUnit(writer, true).run(args);
        writer.flush();
        if(result != DONT_EXIT) 
        {
            System.exit(result);
        }
    }
    
    private ExecutorService getExecutor(OptionProcessor<Options> options) {
        final String value = options.get(Options.threads);
        if(value == null)
        {
            out.println("Using single-threaded test executor.");
            return Executors.newSingleThreadExecutor();
        }
        else if(value.equals("cpus"))
        {
            int n = Runtime.getRuntime().availableProcessors();
            out.printf("Using fixed thread pool of size %d for %d processors.%n", n, n);
            return Executors.newFixedThreadPool(n);
        }
        else if(value.equals("cached"))
        {
            out.println("Using cached thread pool.");
            return Executors.newCachedThreadPool();
        }
        else
        {
            final int n = Integer.parseInt(value);
            out.printf("Using fixed thread pool of size %d.%n", n);
            return n == 1 ? Executors.newSingleThreadExecutor() : Executors.newFixedThreadPool(n);
        }
    }
    
    /**
     * @param args
     * @throws SoarException 
     * @throws InterruptedException 
     * @throws IOException 
     */
    public int run(String[] args) throws SoarException, InterruptedException, IOException
    {
        final OptionProcessor<Options> options = new OptionProcessor<Options>();
        options.
        newOption(Options.help).
        newOption(Options.Recursive).
        newOption(Options.ui).
        newOption(Options.debug).requiredArg().
        newOption(Options.sml).
        newOption(Options.threads).requiredArg().
        done();
        
        final List<String> rest;
        try
        {
            rest = options.process(Lists.asList("SoarUnit", args));
        }
        catch (SoarException e)
        {
            out.println(e.getMessage());
            usage();
            return 1;
        }
        
        if(options.has(Options.help))
        {
            usage();
            return 0;
        }
        
        if(options.has(Options.sml))
        {
            out.println("Using CSoar/SML, make sure that SOAR_HOME is set to the location of your Soar binaries.");
            agentFactory = new SmlTestAgentFactory();
        }
        else
        {
            out.println("Using JSoar.");
            agentFactory = new JSoarTestAgentFactory();
        }
        
        final boolean recursive = options.has(Options.Recursive);
        final TestCaseCollector collector = new TestCaseCollector(out);
        if(rest.isEmpty())
        {
            collector.addEntry(new File("."), recursive);
        }
        else
        {
            for(String arg : rest)
            {
                final File file = new File(arg);
                if(!file.exists())
                {
                    return 1;
                }
                collector.addEntry(file, recursive);
            }
        }
        
        if(options.has(Options.debug))
        {
            debugTest(collector.collect(), options.get(Options.debug));
            return DONT_EXIT;
        }
        else if(options.has(Options.ui))
        {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run()
                {
                    MainFrame.initializeLookAndFeel();
                    final MainFrame mf = new MainFrame(agentFactory, collector, getExecutor(options));
                    mf.setSize(640, 480);
                    mf.setDefaultCloseOperation(fromCommandLine ? JFrame.EXIT_ON_CLOSE : JFrame.DISPOSE_ON_CLOSE);
                    mf.setVisible(true);
                    mf.runTests();
                }});
            return DONT_EXIT;
        }
        else
        {
            final TestRunner runner = new TestRunner(agentFactory, out, getExecutor(options));
            final List<TestCaseResult> results = runner.runAllTestCases(collector.collect(), null);
            return printAllTestCaseResults(results, runner.getFiringCounts());
        }
        
    }
    private static Test findTest(List<TestCase> all, String name)
    {
        for(TestCase testCase : all)
        {
            final Test test = testCase.getTest(name);
            if(test != null)
            {
                return test;
            }
        }
        return null;
    }
    
    private void debugTest(List<TestCase> all, String name) throws SoarException, InterruptedException
    {
        final Test test = findTest(all, name);
        if(test == null)
        {
            out.println("No test named '" + name + "'.");
            System.exit(1);
        }
        
        out.printf("Debugging test %s/%s%n", test.getTestCase().getName(), test.getName());
        final TestRunner runner = new TestRunner(agentFactory, out, null);
        runner.debugTest(test, fromCommandLine);
    }

    private int printAllTestCaseResults(final List<TestCaseResult> results, FiringCounts coverage)
    {
        int totalPassed = 0;
        int totalFailed = 0;
        int totalTests = 0;
        for(TestCaseResult result : results)
        {
            final TestCase testCase = result.getTestCase();
            totalTests += testCase.getTests().size();
            out.println("-------------------------------------------------------------");
            out.printf("Test Case: %s (%s)%n", testCase.getName(), testCase.getUrl());
            out.printf("%d passed, %d failed%n", result.getPassed(), result.getFailed());
            for(TestResult testResult : result.getTestResults())
            {
                final Test test = testResult.getTest();
                if(testResult.isPassed())
                {
                    out.printf("PASSED: %s, %s (%.3f s)%n", test.getName(), testResult.getMessage(), testResult.getRunTimeInSeconds());
                    totalPassed++;
                }
                else
                {
                    out.printf("FAILED: %s, %s (%.3f s)%n", test.getName(), testResult.getMessage(), testResult.getRunTimeInSeconds());
                    out.println(testResult.getOutput());
                    totalFailed++;
                }
            }
        }
        out.println("-------------------------------------------------------------");
        out.printf("%d/%d tests run. %d passed, %d failed, %d%% coverage%n", 
                    totalPassed + totalFailed, 
                    totalTests, 
                    totalPassed, 
                    totalFailed,
                    (int)(coverage.getCoverage() * 100));
        
        return totalFailed > 0 ? 1 : 0;
    }    
}

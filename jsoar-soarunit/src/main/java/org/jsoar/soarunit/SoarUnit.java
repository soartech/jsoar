/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 22, 2010
 */
package org.jsoar.soarunit;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

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
    private static enum Options { help, debug, Recursive, ui, sml };
    
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
        if(result != 0)
        {
            System.exit(result);
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
            out.println("Using CSoar/SML, make sure that SOAR_HOME is set to your Soar installation.");
            agentFactory = new SmlTestAgentFactory();
        }
        else
        {
            out.println("Using JSoar.");
            agentFactory = new JSoarTestAgentFactory();
        }
        
        final List<File> inputs = new ArrayList<File>();
        if(rest.isEmpty())
        {
            inputs.add(new File("."));
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
                inputs.add(file);
            }
        }
        
        final List<TestCase> all = collectAllTestCases(inputs, options.has(Options.Recursive));
        if(options.has(Options.debug))
        {
            debugTest(all, options.get(Options.debug));
            return 0;
        }
        else if(options.has(Options.ui))
        {
            SwingUtilities.invokeLater(new Runnable() {

                /* (non-Javadoc)
                 * @see java.lang.Runnable#run()
                 */
                @Override
                public void run()
                {
                    MainFrame.initializeLookAndFeel();
                    final MainFrame mf = new MainFrame(agentFactory, all);
                    mf.setSize(640, 480);
                    mf.setDefaultCloseOperation(fromCommandLine ? JFrame.EXIT_ON_CLOSE : JFrame.DISPOSE_ON_CLOSE);
                    mf.setVisible(true);
                    mf.runTests();
                }});
            return 0;
        }
        else
        {
            final TestRunner runner = new TestRunner(agentFactory, out);
            final List<TestCaseResult> results = runner.runAllTestCases(all);
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
        final TestRunner runner = new TestRunner(agentFactory, out);
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
            out.printf("Test Case: %s (%s)%n", testCase.getName(), testCase.getFile());
            out.printf("%d passed, %d failed%n", result.getPassed(), result.getFailed());
            for(TestResult testResult : result.getTestResults())
            {
                final Test test = testResult.getTest();
                if(testResult.isPassed())
                {
                    out.printf("PASSED: %s, %s (%.3f s)%n", test.getName(), testResult.getMessage(), testResult.getElapsedSeconds());
                    totalPassed++;
                }
                else
                {
                    out.printf("FAILED: %s, %s (%.3f s)%n", test.getName(), testResult.getMessage(), testResult.getElapsedSeconds());
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

    private List<TestCase> collectAllTestCases(final List<File> inputs, boolean recursive) throws SoarException, IOException
    {
        final List<TestCase> all = new ArrayList<TestCase>();
        for(File input : inputs)
        {
            if(input.isFile())
            {
                all.add(TestCase.fromFile(input));
            }
            else if(input.isDirectory() && recursive)
            {
                all.addAll(collectTestCasesInDirectory(input));
            }
        }
        out.printf("Found %d test case%s%n", all.size(), all.size() != 1 ? "s" : "");
        return all;
    }
    
    private List<TestCase> collectTestCasesInDirectory(File dir) throws SoarException, IOException
    {
        out.println("Collecting tests in directory '" + dir + "'");
        
        final List<TestCase> result = new ArrayList<TestCase>();
        final File[] children = dir.listFiles();
        if(children != null)
        {
            for(File file : children)
            {
                if(file.isDirectory() && !file.getName().startsWith("."))
                {
                    result.addAll(collectTestCasesInDirectory(file));
                }
                else if(file.isFile() && file.getName().startsWith("test") && file.getName().endsWith(".soar"))
                {
                    out.println("Collecting tests in file '" + file + "'");
                    result.add(TestCase.fromFile(file));
                }
            }
        }
        
        return result;
    }
}

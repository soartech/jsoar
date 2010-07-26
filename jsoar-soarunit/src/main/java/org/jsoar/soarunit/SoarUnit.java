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
import org.jsoar.soarunit.ui.MainFrame;
import org.jsoar.util.commands.OptionProcessor;

import com.google.common.collect.Lists;

/**
 * @author ray
 */
public class SoarUnit
{
    private static enum Options { help, debug, Recursive, ui };
    
    private final PrintWriter out;
    private final boolean fromCommandLine;
    
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
        newOption(Options.debug).requiredArg().done();
        
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
        
        final List<TestSuite> all = collectAllTestSuites(inputs, options.has(Options.Recursive));
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
                    final MainFrame mf = new MainFrame(all);
                    mf.setSize(640, 480);
                    mf.setDefaultCloseOperation(fromCommandLine ? JFrame.EXIT_ON_CLOSE : JFrame.DISPOSE_ON_CLOSE);
                    mf.setVisible(true);
                    mf.runTests();
                }});
            return 0;
        }
        else
        {
            final List<TestSuiteResult> results = runAllTestSuites(all);
            return printAllTestSuiteResults(results);
        }
        
    }
    private static Test findTest(List<TestSuite> all, String name)
    {
        for(TestSuite suite : all)
        {
            final Test test = suite.getTest(name);
            if(test != null)
            {
                return test;
            }
        }
        return null;
    }
    
    private void debugTest(List<TestSuite> all, String name) throws SoarException, InterruptedException
    {
        final Test test = findTest(all, name);
        if(test == null)
        {
            out.println("No test named '" + name + "'.");
            System.exit(1);
        }
        
        out.printf("Debugging test %s/%s%n", test.getSuite().getName(), test.getName());
        test.getSuite().debugTest(test);
    }

    private int printAllTestSuiteResults(final List<TestSuiteResult> results)
    {
        int totalPassed = 0;
        int totalFailed = 0;
        int totalTests = 0;
        for(TestSuiteResult result : results)
        {
            final TestSuite suite = result.getSuite();
            totalTests += suite.getTests().size();
            out.println("-------------------------------------------------------------");
            out.printf("Test Suite: %s (%s)%n", suite.getName(), suite.getFile());
            out.printf("%d passed, %d failed%n", result.getPassed(), result.getFailed());
            for(TestResult testResult : result.getTestResults())
            {
                final Test test = testResult.getTest();
                if(testResult.isPassed())
                {
                    out.printf("PASSED: %s, %s%n", test.getName(), testResult.getMessage());
                    totalPassed++;
                }
                else
                {
                    out.printf("FAILED: %s, %s%n", test.getName(), testResult.getMessage());
                    out.println(testResult.getOutput());
                    totalFailed++;
                }
            }
        }
        out.println("-------------------------------------------------------------");
        out.printf("%d/%d tests run. %d passed, %d failed%n", totalPassed + totalFailed, totalTests, totalPassed, totalFailed);
        
        return totalFailed > 0 ? 1 : 0;
    }

    private List<TestSuiteResult> runAllTestSuites(final List<TestSuite> all) throws SoarException
    {
        int index = 0;
        final List<TestSuiteResult> results = new ArrayList<TestSuiteResult>();
        for(TestSuite suite : all)
        {
            final TestSuiteResult result = suite.run(index++, all.size(), true);
            results.add(result);
            if(result.getFailed() > 0)
            {
                break;
            }
        }
        return results;
    }

    private List<TestSuite> collectAllTestSuites(final List<File> inputs, boolean recursive) throws SoarException, IOException
    {
        final List<TestSuite> all = new ArrayList<TestSuite>();
        for(File input : inputs)
        {
            if(input.isFile())
            {
                all.add(TestSuite.fromFile(input));
            }
            else if(input.isDirectory() && recursive)
            {
                all.addAll(collectTestSuitesInDirectory(input));
            }
        }
        out.printf("Found %d test suite%s%n", all.size(), all.size() != 1 ? "s" : "");
        return all;
    }
    
    private List<TestSuite> collectTestSuitesInDirectory(File dir) throws SoarException, IOException
    {
        out.println("Collecting tests in directory '" + dir + "'");
        
        final List<TestSuite> result = new ArrayList<TestSuite>();
        final File[] children = dir.listFiles();
        if(children != null)
        {
            for(File file : children)
            {
                if(file.isDirectory() && !file.getName().startsWith("."))
                {
                    result.addAll(collectTestSuitesInDirectory(file));
                }
                else if(file.isFile() && file.getName().startsWith("test") && file.getName().endsWith(".soar"))
                {
                    out.println("Collecting tests in file '" + file + "'");
                    result.add(TestSuite.fromFile(file));
                }
            }
        }
        
        return result;
    }
}

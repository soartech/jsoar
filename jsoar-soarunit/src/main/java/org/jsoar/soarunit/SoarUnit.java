/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 22, 2010
 */
package org.jsoar.soarunit;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.jsoar.kernel.SoarException;
import org.jsoar.soarunit.jsoar.JSoarTestAgentFactory;
import org.jsoar.soarunit.sml.SmlTestAgentFactory;
import org.jsoar.soarunit.ui.MainFrame;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * @author ray
 */

@Command(name="soarunit", description="SoarUnit - Soar unit test framework",
    subcommands={HelpCommand.class})
public class SoarUnit implements Callable<Integer>
{
    @Option(names = {"-R", "--recursive"}, description = "Recursively search directories for tests.")
    boolean recursive;
    
    @Option(names = {"-d", "--debug"}, paramLabel = "TEST_NAME", description = "Open the given test in a debugger.")
    String debugTestName = "";
    
    @Option(names = {"-u", "--ui"}, description = "Show graphical user interface.")
    boolean ui;
    
    @Option(names = {"-s", "--sml"}, description = "Use CSoar/SML instead of JSoar. CSoar's bin directory must be on the system path or in java.library.path.")
    boolean sml;
    
    @Option(names = { "-t", "--threads" }, converter = ThreadPoolSizeConverter.class, description = "[1, n] for fixed thread pool, 'cpus' for number of cpus, 'cached' for cached")
    ThreadPoolSize threads;
    
    @Parameters(paramLabel = "FILE_AND_DIRECTORIES")
    List<Path> fileAndDirectories = Collections.emptyList();
    
    private static final int DONT_EXIT = 255;
    
    private final PrintWriterProxy out;
    private final CwdProxy cwd;
    private final boolean fromCommandLine;
    private TestAgentFactory agentFactory;
    
    public SoarUnit(PrintWriterProxy out, CwdProxy cwd)
    {
        this(out, cwd, false);
    }

    private SoarUnit(PrintWriterProxy out, CwdProxy cwd, boolean fromCommandLine)
    {
        this.out = out;
        this.cwd = cwd;
        this.fromCommandLine = fromCommandLine;
    }
    
    public static void main(String[] args) throws Exception
    {
        final PrintWriter writer = new PrintWriter(System.out);
        final PrintWriterProxy writerProxy = () -> writer;
        int result = new CommandLine(new SoarUnit(writerProxy, () -> Paths.get("."), true)).execute(args);
        writer.flush();
        if(result != DONT_EXIT) 
        {
            System.exit(result);
        }
    }
    
    private ExecutorService getExecutor() {
        if(this.threads == null)
        {
            out.println("Using single-threaded test executor.");
            return Executors.newSingleThreadExecutor();
        }
        else if(this.threads.dynamic == ThreadPoolSize.Dynamic.cpus)
        {
            int n = Runtime.getRuntime().availableProcessors();
            out.printf("Using fixed thread pool of size %d for %d processors.%n", n, n);
            return Executors.newFixedThreadPool(n);
        }
        else if(this.threads.dynamic == ThreadPoolSize.Dynamic.cached)
        {
            out.println("Using cached thread pool.");
            return Executors.newCachedThreadPool();
        }
        else
        {
            out.printf("Using fixed thread pool of size %d.%n", this.threads.fixed);
            return this.threads.fixed == 1 ? Executors.newSingleThreadExecutor() : Executors.newFixedThreadPool(this.threads.fixed);
        }
    }
    
    /**
     * @throws SoarException 
     * @throws InterruptedException 
     * @throws IOException 
     */
    public Integer call() throws SoarException, InterruptedException, IOException
    {
        if(this.sml)
        {
            out.println("Using CSoar/SML, make sure that SOAR_HOME is set to the location of your Soar binaries.");
            agentFactory = new SmlTestAgentFactory();
        }
        else
        {
            out.println("Using JSoar.");
            agentFactory = new JSoarTestAgentFactory();
        }
        
        //final boolean recursive = options.has(Options.Recursive);
        final TestCaseCollector collector = new TestCaseCollector(out);
        if(this.fileAndDirectories.isEmpty())
        {
            collector.addEntry(cwd.get().toFile(), this.recursive);
        }
        else
        {
            for(Path path : this.fileAndDirectories)
            {
                File file = cwd.get().resolve(path).toFile();
                if(!file.exists())
                {
                    out.printf("Failed to find file/directory: %s", file.toString());
                    return 1;
                }
                collector.addEntry(file, this.recursive);
            }
        }
        
        if(!this.debugTestName.isEmpty())
        {
            debugTest(collector.collect(), this.debugTestName);
            return DONT_EXIT;
        }
        else if(this.ui)
        {
            SwingUtilities.invokeLater(() ->
            {
                MainFrame.initializeLookAndFeel();
                final MainFrame mf = new MainFrame(agentFactory, collector, getExecutor());
                mf.setSize(640, 480);
                mf.setDefaultCloseOperation(fromCommandLine ? JFrame.EXIT_ON_CLOSE : JFrame.DISPOSE_ON_CLOSE);
                mf.setVisible(true);
                mf.runTests();
            });
            return DONT_EXIT;
        }
        else
        {
            final TestRunner runner = new TestRunner(agentFactory, out, getExecutor());
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
    
    private static class ThreadPoolSize {
        enum Dynamic { cpus, cached }

        int fixed = -1;  // if -1, then use the dynamic value
        Dynamic dynamic = null; // if null, then use the fixed value
    }

    private static class ThreadPoolSizeConverter implements CommandLine.ITypeConverter<ThreadPoolSize> {

        @Override
        public ThreadPoolSize convert(String value) throws Exception {
            ThreadPoolSize result = new ThreadPoolSize();
            try {
                result.fixed = Integer.parseInt(value);
                if (result.fixed < 1) {
                    throw new CommandLine.TypeConversionException("Invalid value " +
                            value + ": must be 1 or more.");
                }
            } catch (NumberFormatException nan) {
                try {
                    result.dynamic = ThreadPoolSize.Dynamic.valueOf(
                            value.toLowerCase());
                } catch (IllegalArgumentException ex) {
                    throw new CommandLine.TypeConversionException("Invalid value " +
                            value + ": must be one of " + 
                            Arrays.toString(ThreadPoolSize.Dynamic.values()));
                }
            }
            return result;
        }
    }
    
    /**
     * Provides a lazy way to get a print writer -- helps with SoarUnitCommand, for which we want whatever printer is on top of the stack
     * @author bob.marinier
     *
     */
    @FunctionalInterface
    public interface PrintWriterProxy {
        public PrintWriter get();
        default void print(String s) { get().print(s); }
        default void printf(String s, Object...args) { get().printf(s, args); }
        default void println(String s) { get().println(s); }
    }
    
    /**
     * Provides a lazy way to get the current working directory -- helps with SoarUnitCommand, for which we want to use the agent's cwd
     * @author bob.marinier
     *
     */
    public interface CwdProxy {
        public Path get();
    }
}

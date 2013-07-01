/**
 * 
 */
package org.jsoar.performancetesting;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.SoarException;
import org.jsoar.performancetesting.Configuration.InvalidTestNameException;
import org.jsoar.performancetesting.Configuration.MalformedTestCategory;
import org.jsoar.performancetesting.Configuration.UnknownPropertyException;
import org.jsoar.performancetesting.jsoar.JSoarTestFactory;
import org.jsoar.util.commands.OptionProcessor;

import com.google.common.collect.Lists;

/**
 * @author ALT
 *
 */
public class PerformanceTesting
{
    private static enum Options { help, directory, recursive, configuration, test, jsoar };
    
    private final PrintWriter out;
    
    private JSoarTestFactory jsoarTestFactory;
    
    private final List<TestCategory> csoarTestCategories;
    private final List<Test> csoarTests;
    
    private final List<TestCategory> jsoarTestCategories;
    private final List<Test> jsoarTests;
    
    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_FAILURE = 255;
    private static final int EXIT_FAILURE_TEST = 254;
    private static final int EXIT_FAILURE_CONFIGURATION = 253;
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        final PrintWriter writer = new PrintWriter(System.out);
        final PerformanceTesting performanceTester = new PerformanceTesting(writer);
        
        final int result = performanceTester.doPerformanceTesting(args);
        
        writer.flush();
        
        if (result != EXIT_SUCCESS)
        {
            System.exit(result);
        }
    }

    public PerformanceTesting(PrintWriter out)
    {
        this.out = out;
        
        this.jsoarTestCategories = new ArrayList<TestCategory>();
        this.jsoarTests = new ArrayList<Test>();
        
        this.csoarTestCategories = new ArrayList<TestCategory>();
        this.csoarTests = new ArrayList<Test>();
        
        this.jsoarTestFactory = new JSoarTestFactory();
        
        
        this.jsoarTestCategories.add(new TestCategory("Uncategorized Tests", new ArrayList<Test>()));
        this.csoarTestCategories.add(new TestCategory("Uncategorized Tests", new ArrayList<Test>()));
    }
    
    public void usage()
    {
        out.println("Performance Testing - Performance testing framework for JSoar and CSoar\n" +
                    "performance-testing [options]\n" +
                    "\n" +
                    "Options:\n" +
                    "   -h, --help              This message.\n" +               
                    "   -d, --directory         Load all tests (.soar files) from this directory recursively.\n" +
                    "   -c, --configuration     Load a configuration file to use for testing.\n" +
                    "   -t, --test              Manually specify a test to load.\n" +
                    "   -j, --jsoar             Enables running tests in JSoar. This is on by default.\n" +
                    "\n" +
                    "Note: When running with CSoar, CSoar's bin directory must be on the system\n" +
                    "      path or in java.library.path or in ./csoar/.\n" + 
                    "");
    }
    
    public int doPerformanceTesting(String[] args)
    {
        final OptionProcessor<Options> options = new OptionProcessor<Options>();
        options.
        newOption(Options.help).
        newOption(Options.directory).requiredArg().
        newOption(Options.recursive).
        newOption(Options.configuration).requiredArg().
        newOption(Options.test).requiredArg().
        newOption(Options.jsoar).requiredArg().
        done();
        
        final List<String> rest;
        try
        {
            rest = options.process(Lists.asList("PerformanceTesting", args));
        }
        catch (SoarException e)
        {
            out.println(e.getMessage());
            usage();
            return EXIT_SUCCESS;
        }
        
        if (options.has(Options.help))
        {
            usage();
            return EXIT_SUCCESS;
        }
                
        if (options.has(Options.directory))
        {
            String directory = options.get(Options.directory);
            
            Path directoryPath = FileSystems.getDefault().getPath(directory);
            DirectoryStream<Path> stream;
            
            try
            {
                stream = Files.newDirectoryStream(directoryPath);
            }
            catch (IOException e)
            {
                out.println("Failed to create new directory stream: " + e.getMessage());
                return EXIT_FAILURE;
            }
            
            for (Path path : stream)
            {
                String testName = path.getFileName().toString();
                
                if (!testName.endsWith(".soar"))
                    continue;
                
                testName = testName.substring(0, testName.length()-5);
                
                //TODO: Add the CSoar version of this
                Test jsoarTest = jsoarTestFactory.createTest(testName, path.toString());
                jsoarTests.add(jsoarTest);
                
                TestCategory.getTestCategory("Uncategorized Tests", jsoarTestCategories).addTest(jsoarTest);
            }
            
            try
            {
                stream.close();
            }
            catch (IOException e)
            {
                out.println("Failed to close directory stream: " + e.getMessage());
                return EXIT_FAILURE;
            }
        }
        
        if (options.has(Options.test))
        {
            String testPath = options.get(Options.test);
            
            if (!testPath.endsWith(".soar"))
            {
                out.println("Tests need to end with .soar");
                return EXIT_FAILURE_TEST;
            }
            
            String testName = testPath.substring(0, testPath.length()-5);
            
            //TODO: Add the CSoar version of this
            Test jsoarTest = jsoarTestFactory.createTest(testName, testPath);
            jsoarTests.add(jsoarTest);
            
            TestCategory.getTestCategory("Uncategorized Tests", jsoarTestCategories).addTest(jsoarTest);
        }
        
        if (options.has(Options.configuration))
        {
            String configurationPath = options.get(Options.configuration);
            
            if (!configurationPath.endsWith(".properties"))
            {
                out.println("Configuration files need to be properties files.");
                return EXIT_FAILURE_CONFIGURATION;
            }
            
            Configuration config = new Configuration(configurationPath);
            int result = Configuration.PARSE_FAILURE;
            
            try
            {
                result = config.parse();
            }
            catch (IOException e)
            {
                out.println("Parsing configuration failed with IOException: " + e.getMessage());
                return EXIT_FAILURE;
            }
            catch (UnknownPropertyException e)
            {
                out.println(e.getMessage());
                return EXIT_FAILURE;
            }
            catch (InvalidTestNameException e)
            {
                out.println(e.getMessage());
                return EXIT_FAILURE;
            }
            catch (MalformedTestCategory e)
            {
                out.println(e.getMessage());
                return EXIT_FAILURE;
            }
            
            if (result != Configuration.PARSE_SUCCESS)
            {
                out.println("Configuration parsing failed!");
                return EXIT_FAILURE_CONFIGURATION;
            }
            
            List<Configuration.ConfigurationTest> configurationTests = config.getConfigurationTests();
            
            for (Configuration.ConfigurationTest test : configurationTests)
            {
              //TODO: Add the CSoar version of this
                Test jsoarTest = jsoarTestFactory.createTest(test.getTestName(), test.getTestFile());
                jsoarTests.add(jsoarTest);
                
                TestCategory category = TestCategory.getTestCategory(test.getTestCategory(), jsoarTestCategories);
                
                if (category == null)
                {
                    category = new TestCategory(test.getTestCategory(), new ArrayList<Test>());
                    jsoarTestCategories.add(category);
                }
                
                category.addTest(jsoarTest);
            }
        }
        
        out.println("Performance Testing - Starting Tests\n");
        out.flush();
        
        //TODO: Implement this with ability to run CSoar and JSoar one after another (and just the ability to run CSoar tests
        List<TestRunner> testRunners = new ArrayList<TestRunner>();
        
        for (TestCategory category : jsoarTestCategories)
        {
            List<Test> tests = category.getCategoryTests();
            
            for (Test test : tests)
            {
                TestRunner testRunner = new TestRunner(test, out);
                
                out.println("Starting Test: " + test.getTestName());
                out.flush();
                
                try
                {
                    testRunner.runTestsForAverage(3, 1);
                }
                catch (SoarException e)
                {
                    out.println("Failed with a Soar Exception: " + e.getMessage());
                    return EXIT_FAILURE;
                }
                
                out.println(test.getTestName() + " Results:\n" +
                            "Total CPU Time: " + testRunner.getTotalCPUTime() + "\n" +
                            "Average CPU Time Per Run: " + testRunner.getAverageCPUTime() + "\n" +
                            "Total Kernel Time: " + testRunner.getTotalKernelTime() + "\n" +
                            "Average Kernel Time Per Run: " + testRunner.getAverageKernelTime() + "\n" +
                            "Decision Cycles Run For: " + testRunner.getTotalDecisionCycles() + "\n" +
                            "Average Decision Cycles Per Run: " + testRunner.getAverageDecisionCycles() + "\n" +
                            "Memory Used: " + testRunner.getTotalMemoryLoad()/1000.0/1000.0 + "M\n" +
                            "Average Memory Used Per Run: " + testRunner.getAverageMemoryLoad()/1000.0/1000.0 + "M\n\n");
                
                out.flush();
                
                testRunners.add(testRunner);
            }
        }
        
        out.println("Performance Testing - Done");
        
        double totalTimeTaken = 0.0;
        long totalDecisionCycles = 0;
        long totalMemoryUsed = 0;
        
        for (TestRunner testRunner : testRunners)
        {
            totalTimeTaken += testRunner.getTotalCPUTime();
            totalDecisionCycles += testRunner.getTotalDecisionCycles();
            totalMemoryUsed += testRunner.getTotalMemoryLoad();
        }
        
        out.println("Total Time Taken: " + totalTimeTaken + "\n" +
                    "Total Decision Cycles Run For: " + totalDecisionCycles + "\n" +
                    "Total Memory Used: " + totalMemoryUsed/1000.0/1000.0 + "M\n");
        
        return EXIT_SUCCESS;
    }

}

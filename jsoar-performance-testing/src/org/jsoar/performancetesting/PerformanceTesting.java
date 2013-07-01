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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jsoar.kernel.SoarException;
import org.jsoar.performancetesting.Configuration.InvalidTestNameException;
import org.jsoar.performancetesting.Configuration.MalformedTestCategory;
import org.jsoar.performancetesting.Configuration.UnknownPropertyException;
import org.jsoar.performancetesting.csoar.CSoarTestFactory;
import org.jsoar.performancetesting.jsoar.JSoarTestFactory;
import org.jsoar.util.commands.OptionProcessor;

import com.google.common.collect.Lists;

/**
 * @author ALT
 *
 */
public class PerformanceTesting
{
    private static enum Options { help, directory, recursive, configuration, test };
    
    private final PrintWriter out;
    
    private JSoarTestFactory jsoarTestFactory;
    private List<CSoarTestFactory> csoarTestFactories;
    
    private final HashMap<String, List<TestCategory>> csoarTestCategories;
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
        
        this.csoarTestCategories = new HashMap<String, List<TestCategory>>();
        this.csoarTests = new ArrayList<Test>();
        
        this.jsoarTestFactory = new JSoarTestFactory();
        this.csoarTestFactories = new ArrayList<CSoarTestFactory>();
        
        this.jsoarTestCategories.add(new TestCategory("Uncategorized Tests", new ArrayList<Test>()));
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
            catch (IOException | UnknownPropertyException | InvalidTestNameException | MalformedTestCategory e)
            {
                out.println(e.getMessage());
                return EXIT_FAILURE;
            }
            
            if (result != Configuration.PARSE_SUCCESS)
            {
                out.println("Configuration parsing failed!");
                return EXIT_FAILURE_CONFIGURATION;
            }
            

            HashMap<String, String> csoarDirectory = config.getCSoarDirectory();
            
            List<String> keys = new ArrayList<String>();
            List<String> values = new ArrayList<String>();
            
            for (Iterator<String> it = csoarDirectory.keySet().iterator();it.hasNext();)
            {
                String key = it.next();
                keys.add(key);
            }
            
            for (Iterator<String> it = csoarDirectory.values().iterator();it.hasNext();)
            {
                String value = it.next();
                values.add(value);
            }
            
            for (int i = 0;i < keys.size();i++)
            {
                CSoarTestFactory factory = new CSoarTestFactory(keys.get(i), values.get(i));
                csoarTestFactories.add(factory);
                
                List<TestCategory> testCategories = new ArrayList<TestCategory>();
                testCategories.add(new TestCategory("Uncategorized Tests", new ArrayList<Test>()));
                this.csoarTestCategories.put(keys.get(i), testCategories);
            }
            
            List<Configuration.ConfigurationTest> configurationTests = config.getConfigurationTests();
            
            for (Configuration.ConfigurationTest test : configurationTests)
            {
                TestCategory jsoarCategory = TestCategory.getTestCategory(test.getTestCategory(), jsoarTestCategories);
                
                if (jsoarCategory == null)
                {
                    //If JSoar Category is null then CSoar ones are too
                    jsoarCategory = new TestCategory(test.getTestCategory(), new ArrayList<Test>());
                    TestCategory csoarCategory = new TestCategory(test.getTestCategory(), new ArrayList<Test>());
                    
                    jsoarTestCategories.add(jsoarCategory);
                    
                    for (String key : csoarTestCategories.keySet())
                    {
                        csoarTestCategories.get(key).add(csoarCategory);
                    }
                }
                
                for (CSoarTestFactory factory : csoarTestFactories)
                {
                    Test csoarTest = factory.createTest(test.getTestName(), test.getTestFile());
                    csoarTests.add(csoarTest);
                    
                    for (String key : csoarTestCategories.keySet())
                    {
                        List<TestCategory> csoarCategories = csoarTestCategories.get(key);
                        TestCategory csoarCategory = TestCategory.getTestCategory(test.getTestCategory(), csoarCategories);
                        
                        csoarCategory.addTest(csoarTest);
                    }
                }
                
                Test jsoarTest = jsoarTestFactory.createTest(test.getTestName(), test.getTestFile());
                jsoarTests.add(jsoarTest);
                
                jsoarCategory.addTest(jsoarTest);
            }
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
                                
                for (CSoarTestFactory factory : csoarTestFactories)
                {
                    Test csoarTest = factory.createTest(testName, path.toString());
                    csoarTests.add(csoarTest);
                    
                    for (String key : csoarTestCategories.keySet())
                    {
                        List<TestCategory> csoarCategories = csoarTestCategories.get(key);
                        TestCategory csoarCategory = TestCategory.getTestCategory("Uncategorized Tests", csoarCategories);
                        
                        csoarCategory.addTest(csoarTest);
                    }
                }
                
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
            
            for (CSoarTestFactory factory : csoarTestFactories)
            {
                Test csoarTest = factory.createTest(testName, testPath);
                csoarTests.add(csoarTest);
                
                for (String key : csoarTestCategories.keySet())
                {
                    List<TestCategory> csoarCategories = csoarTestCategories.get(key);
                    TestCategory csoarCategory = TestCategory.getTestCategory("Uncategorized Tests", csoarCategories);
                    
                    csoarCategory.addTest(csoarTest);
                }
            }
            
            Test jsoarTest = jsoarTestFactory.createTest(testName, testPath);
            jsoarTests.add(jsoarTest);
            
            TestCategory.getTestCategory("Uncategorized Tests", jsoarTestCategories).addTest(jsoarTest);
        }
        
        out.println("Performance Testing - Starting Tests\n");
        out.flush();
        
        List<TestRunner> testRunners = new ArrayList<TestRunner>();
                
        for (int i = 0;i < jsoarTestCategories.size();i++)
        {
            // So because these category lists SHOULD be identical, I can get
            // away with doing this only for jsoarTestCategories.  If there is
            // an issue this will show me that bug anyways
            // - ALT
            
            TestCategory jsoarCategory = jsoarTestCategories.get(i);
            HashMap<String, TestCategory> csoarCategories = new HashMap<String, TestCategory>();
            
            for (String key : csoarTestCategories.keySet())
            {
                csoarCategories.put(key, csoarTestCategories.get(key).get(i));
            }
            
            List<Test> jsoarTests = jsoarCategory.getCategoryTests();
            HashMap<String, List<Test>> csoarTests = new HashMap<String, List<Test>>();
            
            for (String key : csoarCategories.keySet())
            {
                csoarTests.put(key, csoarCategories.get(key).getCategoryTests());
            }
            
            out.println("Starting " + jsoarCategory.getCategoryName() + ": \n");
            out.flush();            
            
            for (int j = 0;j < jsoarTests.size();j++)
            {
                // Same thing as the above comment
                // - ALT
                
                Test jsoarTest = jsoarTests.get(i);
                HashMap<String, Test> csoarTest = new HashMap<String, Test>();
                
                for (String key : csoarTests.keySet())
                {
                    csoarTest.put(key, csoarTests.get(key).get(i));
                }
                
                TestRunner jsoarTestRunner = new TestRunner(jsoarTest, out);
                HashMap<String, TestRunner> csoarTestRunner = new HashMap<String, TestRunner>();
                
                for (String key : csoarTest.keySet())
                {
                    csoarTestRunner.put(key, new TestRunner(csoarTest.get(key), out));
                }
                
                out.println("Starting Test: " + jsoarTest.getTestName());
                out.flush();
                
                //Run JSoar First
//                out.println("JSoar: ");
//                out.flush();
//                try
//                {
//                    jsoarTestRunner.runTestsForAverage(3, 1);
//                }
//                catch (SoarException e)
//                {
//                    out.println("Failed with a Soar Exception: " + e.getMessage());
//                    return EXIT_FAILURE;
//                }
//                
//                out.println(jsoarTest.getTestName() + " Results:\n" +
//                            "Total CPU Time: " + jsoarTestRunner.getTotalCPUTime() + "\n" +
//                            "Average CPU Time Per Run: " + jsoarTestRunner.getAverageCPUTime() + "\n" +
//                            "Total Kernel Time: " + jsoarTestRunner.getTotalKernelTime() + "\n" +
//                            "Average Kernel Time Per Run: " + jsoarTestRunner.getAverageKernelTime() + "\n" +
//                            "Decision Cycles Run For: " + jsoarTestRunner.getTotalDecisionCycles() + "\n" +
//                            "Average Decision Cycles Per Run: " + jsoarTestRunner.getAverageDecisionCycles() + "\n" +
//                            "Memory Used: " + jsoarTestRunner.getTotalMemoryLoad()/1000.0/1000.0 + "M\n" +
//                            "Average Memory Used Per Run: " + jsoarTestRunner.getAverageMemoryLoad()/1000.0/1000.0 + "M\n\n");
//                
//                out.flush();
//                
//                testRunners.add(jsoarTestRunner);
                
                //Run CSoar Second
                for (String key : csoarTestRunner.keySet())
                {
                    out.println("CSoar " + key +": ");
                    out.flush();

                    try
                    {
                        csoarTestRunner.get(key).runTestsForAverage(2, 0);
                    }
                    catch (SoarException e)
                    {
                    }

                    out.println(csoarTest.get(key).getTestName() + " Results:\n" +
                            "Total CPU Time: " + csoarTestRunner.get(key).getTotalCPUTime() + "\n" +
                            "Average CPU Time Per Run: " + csoarTestRunner.get(key).getAverageCPUTime() + "\n" + 
                            "Total Kernel Time: " + csoarTestRunner.get(key).getTotalKernelTime() + "\n" + 
                            "Average Kernel Time Per Run: " + csoarTestRunner.get(key).getAverageKernelTime() + "\n" + 
                            "Decision Cycles Run For: " + csoarTestRunner.get(key).getTotalDecisionCycles() + "\n" +
                            "Average Decision Cycles Per Run: " + csoarTestRunner.get(key).getAverageDecisionCycles() + "\n" +
                            "Memory Used: " + csoarTestRunner.get(key).getTotalMemoryLoad() / 1000.0 / 1000.0 + "M\n" + 
                            "Average Memory Used Per Run: " + csoarTestRunner.get(key).getAverageMemoryLoad() / 1000.0 / 1000.0 + "M\n\n");

                    out.flush();

                    testRunners.add(csoarTestRunner.get(key));
                }
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

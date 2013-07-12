/**
 * 
 */
package org.jsoar.performancetesting;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

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
    private static final String SUMMARY_FILE_NAME = "test-summaries.txt";

    private static enum Options
    {
        help, directory, recursive, configuration, test
    };

    private final PrintWriter out;

    private JSoarTestFactory jsoarTestFactory;

    private CSoarTestFactory csoarTestFactory;

    private final List<TestCategory> csoarTestCategories;

    private final List<Test> csoarTests;

    private final List<TestCategory> jsoarTestCategories;

    private final List<Test> jsoarTests;

    private Long seed = 123456789L;
    
    private int runCount = 20;

    private int warmUpCount = 10;

    private boolean jsoarEnabled = true;

    private boolean csoarEnabled = false;
    
    private List<String> categoriesToRun;
    private List<String> testsToRun;
    
    private String csvDirectory = "";

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

    /**
     * 
     * @param out
     */
    public PerformanceTesting(PrintWriter out)
    {
        this.out = out;

        this.jsoarTestCategories = new ArrayList<TestCategory>();
        this.jsoarTests = new ArrayList<Test>();

        this.csoarTestCategories = new ArrayList<TestCategory>();
        this.csoarTests = new ArrayList<Test>();

        this.jsoarTestFactory = new JSoarTestFactory();
        this.csoarTestFactory = new CSoarTestFactory();

        this.jsoarTestCategories.add(new TestCategory("Uncategorized Tests", new ArrayList<Test>()));
    }

    /**
     * Outputs to the PrintWriter the usage string.
     */
    public void usage()
    {
        out.println("Performance Testing - Performance testing framework for JSoar and CSoar\n" + "performance-testing [options]\n" + "\n" + "Options:\n" + "   -h, --help              This message.\n"
                + "   -d, --directory         Load all tests (.soar files) from this directory recursively.\n" + "   -c, --configuration     Load a configuration file to use for testing.\n"
                + "   -t, --test              Manually specify a test to load.\n" + "\n" + "Note: When running with CSoar, CSoar's bin directory must be on the system\n" + "      path or in java.library.path or in ./csoar/.\n" + "");
    }

    // CODEREVIEW: this method is huge! break it up!
    /**
     * 
     * @param args
     * @return Whether performance testing was successful or not.
     */
    public int doPerformanceTesting(String[] args)
    {
        //This is the same options processor for JSoar and so has the same limitations.
        final OptionProcessor<Options> options = new OptionProcessor<Options>();
        options.newOption(Options.help).newOption(Options.directory).requiredArg().newOption(Options.recursive).newOption(Options.configuration).requiredArg().newOption(Options.test).requiredArg().done();

        try
        {
            options.process(Lists.asList("PerformanceTesting", args));
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

            //Make sure there are no duplicate keys and then parse the properties file
            try
            {
                config.checkPropertiesFile(out);
                
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
            
            seed = config.getSeed();

            if (config.getRunCount() > 0)
                runCount = config.getRunCount();
            else
                out.println("Defaulting to running tests 20 times");

            if (config.getWarmUpCount() >= 0)
                warmUpCount = config.getWarmUpCount();
            else
                out.println("Defaulting to warming up tests 10 times");

            jsoarEnabled = config.getJSoarEnabled();
            csoarEnabled = config.getCSoarEnabled();

            if (!jsoarEnabled && !csoarEnabled)
            {
                out.println("WARNING: You must select something to run.  Defaulting to JSoar.");
                jsoarEnabled = true;
            }
            
            categoriesToRun = config.getCategoriesToRun();
            testsToRun = config.getTestsToRun();

            String csoarDirectory = config.getCSoarDirectory();
            String csoarLabel = config.getCSoarLabel();
            
            csvDirectory = config.getCSVDirectory().trim();

            csoarTestFactory.setLabel(csoarLabel);
            csoarTestFactory.setCSoarDirectory(csoarDirectory);

            this.csoarTestCategories.add(new TestCategory("Uncategorized Tests", new ArrayList<Test>()));

            SortedSet<Configuration.ConfigurationTest> configurationTests = config.getConfigurationTests();

            //Convert all the ConfigurationTest holders to actual tests.
            for (Configuration.ConfigurationTest test : configurationTests)
            {
                if (jsoarEnabled)
                {
                    TestCategory jsoarCategory = TestCategory.getTestCategory(test.getTestCategory(), jsoarTestCategories);

                    if (jsoarCategory == null)
                    {
                        jsoarCategory = new TestCategory(test.getTestCategory(), new ArrayList<Test>());

                        jsoarTestCategories.add(jsoarCategory);
                    }

                    Test jsoarTest = jsoarTestFactory.createTest(test.getTestName(), test.getTestFile(), config.getDecisionCyclesToRunTest(test.getTestName()));
                    jsoarTests.add(jsoarTest);

                    jsoarCategory.addTest(jsoarTest);
                }

                if (csoarEnabled)
                {
                    TestCategory csoarCategory = TestCategory.getTestCategory(test.getTestCategory(), csoarTestCategories);

                    if (csoarCategory == null)
                    {
                        csoarCategory = new TestCategory(test.getTestCategory(), new ArrayList<Test>());

                        csoarTestCategories.add(csoarCategory);
                    }

                    Test csoarTest = csoarTestFactory.createTest(test.getTestName(), test.getTestFile(), config.getDecisionCyclesToRunTest(test.getTestName()));
                    csoarTests.add(csoarTest);

                    csoarCategory.addTest(csoarTest);
                }
            }
        }

        //This will load all tests from a directory into the uncategorized tests category.
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

                testName = testName.substring(0, testName.length() - 5);

                if (jsoarEnabled)
                {
                    Test jsoarTest = jsoarTestFactory.createTest(testName, path.toString(), 0);
                    jsoarTests.add(jsoarTest);

                    TestCategory.getTestCategory("Uncategorized Tests", jsoarTestCategories).addTest(jsoarTest);
                }

                if (csoarEnabled)
                {
                    Test csoarTest = csoarTestFactory.createTest(testName, path.toString(), 0);
                    csoarTests.add(csoarTest);

                    TestCategory.getTestCategory("Uncategorized Tests", csoarTestCategories).addTest(csoarTest);
                }
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

        // This will load an individual test into the uncategorized tests category, only really useful
        // for single tests that you don't want to create a configuration file for
        if (options.has(Options.test))
        {
            String testPath = options.get(Options.test);

            if (!testPath.endsWith(".soar"))
            {
                out.println("Tests need to end with .soar");
                return EXIT_FAILURE_TEST;
            }

            String testName = testPath.substring(0, testPath.length() - 5);

            if (jsoarEnabled)
            {
                Test jsoarTest = jsoarTestFactory.createTest(testName, testPath, 0);
                jsoarTests.add(jsoarTest);

                TestCategory.getTestCategory("Uncategorized Tests", jsoarTestCategories).addTest(jsoarTest);
            }

            if (csoarEnabled)
            {
                Test csoarTest = csoarTestFactory.createTest(testName, testPath, 0);
                csoarTests.add(csoarTest);

                TestCategory.getTestCategory("Uncategorized Tests", csoarTestCategories).addTest(csoarTest);
            }
        }

        // delete summary file so we don't append to old results
        {
            File summaryFile = new File(csvDirectory + "/" + SUMMARY_FILE_NAME);
            summaryFile.delete();
        }
        
        out.println("Performance Testing - Starting Tests\n");
        out.flush();

        List<TestRunner> testRunners = new ArrayList<TestRunner>();

        List<TestCategory> testCategories = null;

        if (jsoarEnabled)
            testCategories = jsoarTestCategories;
        else
            testCategories = csoarTestCategories;

        for (int i = 0; i < testCategories.size(); i++)
        {
            // So because these category lists SHOULD be identical (if you're
            // running
            // both JSoar and CSoar), I can get away with doing this only for
            // one of the categories. If there is an issue this will show me
            // that bug anyways
            // - ALT

            TestCategory category = testCategories.get(i);
            
            if (categoriesToRun.size() != 0)
            {
                if (categoriesToRun.contains(category.getCategoryName()) == false)
                    continue;
            }

            out.println("Starting " + category.getCategoryName() + ": \n");
            out.flush();

            List<Test> tests = null;

            if (jsoarEnabled)
                tests = jsoarTests;
            else
                tests = csoarTests;

            for (int j = 0; j < tests.size(); j++)
            {
                // Same thing as the above comment
                // - ALT
                
                if (testsToRun.size() != 0)
                {
                    if (testsToRun.contains(tests.get(j).getTestName()) == false)
                        continue;
                }
                
                if (category.containsTest(tests.get(j)) == false)
                        continue;

                out.println("Starting Test: " + tests.get(j).getTestName());
                out.flush();
                
                //Construct a table for the data
                
                Table table = new Table();
                
                for (int k = 0;k < 14;k++)
                {
                    Row row = new Row();
                    
                    switch (k)
                    {
                    case 0:
                        row.add(new Cell(tests.get(j).getTestName()));
                        row.add(new Cell("JSoar"));
                        row.add(new Cell("CSoar " + csoarTestFactory.getLabel()));
                        break;
                    case 1:
                        row.add(new Cell("Total CPU Time (s)"));
                        break;
                    case 2:
                        row.add(new Cell("Average CPU Time Per Run (s)"));
                        break;
                    case 3:
                        row.add(new Cell("Median CPU Time Per Run (s)"));
                        break;
                    case 4:
                        row.add(new Cell("Total Kernel Time (s)"));
                        break;
                    case 5:
                        row.add(new Cell("Average Kernel Time Per Run (s)"));
                        break;
                    case 6:
                        row.add(new Cell("Median Kernel Time Per Run (s)"));
                        break;
                    case 7:
                        row.add(new Cell("Decision Cycles Run For"));
                        break;
                    case 8:
                        row.add(new Cell("Average Decision Cycles Per Run"));
                        break;
                    case 9:
                        row.add(new Cell("Median Decision Cycles Per Run"));
                        break;
                    case 10:
                        row.add(new Cell("Memory Used (M)"));
                        break;
                    case 11:
                        row.add(new Cell("Average Memory Used Per Run (M)"));
                        break;
                    case 12:
                        row.add(new Cell("Median Memory Used Per Run (M)"));
                        break;
                    case 13:
                        row.add(new Cell("Memory Deviation from Average (M)"));
                        break;
                    }
                    
                    table.addRow(row);
                }

                // Run JSoar First
                if (jsoarEnabled)
                {
                    out.println("JSoar: ");
                    out.flush();

                    Test jsoarTest = jsoarTests.get(j);

                    TestRunner jsoarTestRunner = new TestRunner(jsoarTest, out);
                    try
                    {
                        jsoarTestRunner.runTestsForAverage(runCount, warmUpCount, seed);
                    }
                    catch (SoarException e)
                    {
                        out.println("Failed with a Soar Exception: " + e.getMessage());
                        return EXIT_FAILURE;
                    }
                    
                    table.setOrAddValueAtLocation(new Double(jsoarTestRunner.getTotalCPUTime()).toString(),          2-1,   2-1);
                    table.setOrAddValueAtLocation(new Double(jsoarTestRunner.getAverageCPUTime()).toString(),        3-1,   2-1);
                    table.setOrAddValueAtLocation(new Double(jsoarTestRunner.getMedianCPUTime()).toString(),         4-1,   2-1);
                    table.setOrAddValueAtLocation(new Double(jsoarTestRunner.getTotalKernelTime()).toString(),       5-1,   2-1);
                    table.setOrAddValueAtLocation(new Double(jsoarTestRunner.getAverageKernelTime()).toString(),     6-1,   2-1);
                    table.setOrAddValueAtLocation(new Double(jsoarTestRunner.getMedianKernelTime()).toString(),      7-1,   2-1);
                    table.setOrAddValueAtLocation(new Double(jsoarTestRunner.getTotalDecisionCycles()).toString(),   8-1,   2-1);
                    table.setOrAddValueAtLocation(new Double(jsoarTestRunner.getAverageDecisionCycles()).toString(), 9-1,   2-1);
                    table.setOrAddValueAtLocation(new Double(jsoarTestRunner.getMedianDecisionCycles()).toString(),  10-1,  2-1);
                    table.setOrAddValueAtLocation(new Double(jsoarTestRunner.getTotalMemoryLoad()/ 1000.0 / 1000.0).toString(),       11-1,  2-1);
                    table.setOrAddValueAtLocation(new Double(jsoarTestRunner.getAverageMemoryLoad()/ 1000.0 / 1000.0).toString(),     12-1,  2-1);
                    table.setOrAddValueAtLocation(new Double(jsoarTestRunner.getMedianMemoryLoad()/ 1000.0 / 1000.0).toString(),      13-1,  2-1);                    
                    table.setOrAddValueAtLocation(new Double(jsoarTestRunner.getMemoryLoadDeviation()/ 1000.0 / 1000.0).toString(),   14-1,  2-1);
                    
//                    out.println(
//                            "\n" + jsoarTest.getTestName() + " Results:\n" +
//                            "Total CPU Time (s): " + jsoarTestRunner.getTotalCPUTime() + "\n" +
//                            "Average CPU Time Per Run (s): " + jsoarTestRunner.getAverageCPUTime() + "\n" +
//                            "Median CPU Time Per Run (s): " + jsoarTestRunner.getMedianCPUTime() + "\n" +
//                            "Total Kernel Time (s): " + jsoarTestRunner.getTotalKernelTime() + "\n" +
//                            "Average Kernel Time Per Run (s): " + jsoarTestRunner.getAverageKernelTime() + "\n" +
//                            "Median Kernel Time Per Run (s): " + jsoarTestRunner.getMedianKernelTime() + "\n" +
//                            "Decision Cycles Run For: " + jsoarTestRunner.getTotalDecisionCycles() + "\n" +
//                            "Average Decision Cycles Per Run: " + jsoarTestRunner.getAverageDecisionCycles() + "\n" +
//                            "Median Decision Cycles Per Run: " + jsoarTestRunner.getMedianDecisionCycles() + "\n" +
//                            "Memory Used (M): " + jsoarTestRunner.getTotalMemoryLoad() / 1000.0 / 1000.0 + "M\n" +
//                            "Average Memory Used Per Run (M): " + jsoarTestRunner.getAverageMemoryLoad() / 1000.0 / 1000.0 + "M\n" + 
//                            "Median Memory Used Per Run (M): " + jsoarTestRunner.getMedianMemoryLoad() / 1000.0 / 1000.0 + "M\n" +
//                            "Memory Deviation from Average (M): " + jsoarTestRunner.getMemoryLoadDeviation() / 1000.0 / 1000.0 + "M\n\n"
//                            );
//
//                    out.flush();

                    testRunners.add(jsoarTestRunner);
                }

                // Run CSoar Second
                if (csoarEnabled)
                {
                    Test csoarTest = csoarTests.get(j);
                    out.println("CSoar " + csoarTestFactory.getLabel() + ": ");
                    out.flush();

                    TestRunner csoarTestRunner = new TestRunner(csoarTest, out);
                    try
                    {
                        csoarTestRunner.runTestsForAverage(runCount, 0, seed);
                    }
                    catch (SoarException e)
                    {
                        out.println("Failed with a Soar Exception: " + e.getMessage());
                        return EXIT_FAILURE;
                    }
                    
                    table.setOrAddValueAtLocation(new Double(csoarTestRunner.getTotalCPUTime()).toString(),          2-1,   3-1);
                    table.setOrAddValueAtLocation(new Double(csoarTestRunner.getAverageCPUTime()).toString(),        3-1,   3-1);
                    table.setOrAddValueAtLocation(new Double(csoarTestRunner.getMedianCPUTime()).toString(),         4-1,   3-1);
                    table.setOrAddValueAtLocation(new Double(csoarTestRunner.getTotalKernelTime()).toString(),       5-1,   3-1);
                    table.setOrAddValueAtLocation(new Double(csoarTestRunner.getAverageKernelTime()).toString(),     6-1,   3-1);
                    table.setOrAddValueAtLocation(new Double(csoarTestRunner.getMedianKernelTime()).toString(),      7-1,   3-1);
                    table.setOrAddValueAtLocation(new Double(csoarTestRunner.getTotalDecisionCycles()).toString(),   8-1,   3-1);
                    table.setOrAddValueAtLocation(new Double(csoarTestRunner.getAverageDecisionCycles()).toString(), 9-1,   3-1);
                    table.setOrAddValueAtLocation(new Double(csoarTestRunner.getMedianDecisionCycles()).toString(),  10-1,  3-1);
                    table.setOrAddValueAtLocation(new Double(csoarTestRunner.getTotalMemoryLoad() / 1000.0 / 1000.0).toString(),       11-1,  3-1);
                    table.setOrAddValueAtLocation(new Double(csoarTestRunner.getAverageMemoryLoad() / 1000.0 / 1000.0).toString(),     12-1,  3-1);
                    table.setOrAddValueAtLocation(new Double(csoarTestRunner.getMedianMemoryLoad() / 1000.0 / 1000.0).toString(),      13-1,  3-1);                    
                    table.setOrAddValueAtLocation(new Double(csoarTestRunner.getMemoryLoadDeviation() / 1000.0 / 1000.0).toString(),   14-1,  3-1);

//                    out.println(
//                            "\n" + csoarTest.getTestName() + " Results:\n" +
//                            "Total CPU Time (s): " + csoarTestRunner.getTotalCPUTime() + "\n" +
//                            "Average CPU Time Per Run (s): " + csoarTestRunner.getAverageCPUTime() + "\n" +
//                            "Median CPU Time Per Run (s): " + csoarTestRunner.getMedianCPUTime() + "\n" +
//                            "Total Kernel Time (s): " + csoarTestRunner.getTotalKernelTime() + "\n" +
//                            "Average Kernel Time Per Run (s): " + csoarTestRunner.getAverageKernelTime() + "\n" +
//                            "Median Kernel Time Per Run (s): " + csoarTestRunner.getMedianKernelTime() + "\n" +
//                            "Decision Cycles Run For: " + csoarTestRunner.getTotalDecisionCycles() + "\n" +
//                            "Average Decision Cycles Per Run: " + csoarTestRunner.getAverageDecisionCycles() + "\n" +
//                            "Median Decision Cycles Per Run: " + csoarTestRunner.getMedianDecisionCycles() + "\n" +
//                            "Memory Used (M): " + csoarTestRunner.getTotalMemoryLoad() / 1000.0 / 1000.0 + "M\n" +
//                            "Average Memory Used Per Run (M): " + csoarTestRunner.getAverageMemoryLoad() / 1000.0 / 1000.0 + "M\n" + 
//                            "Median Memory Used Per Run (M): " + csoarTestRunner.getMedianMemoryLoad() / 1000.0 / 1000.0 + "M\n" +
//                            "Memory Deviation from Average (M): " + csoarTestRunner.getMemoryLoadDeviation() / 1000.0 / 1000.0 + "M\n\n"
//                            );
//
//                    out.flush();

                    testRunners.add(csoarTestRunner);
                }
                
                table.writeToWriter(out);
                
                if (csvDirectory.length() != 0)
                {
                    File out = new File(csvDirectory);
                    
                    if (!out.exists())
                    {
                        out.mkdirs();
                    }
                                        
                    String testNameWithoutSpaces = tests.get(j).getTestName().replaceAll("\\s+", "-");
                    
                    table.writeToCSV(csvDirectory + "/" + testNameWithoutSpaces + ".txt");
                    table.writeToCSV(csvDirectory + "/" + SUMMARY_FILE_NAME, true);
                }
                
                out.print("\n");
            }
        }

        out.println("Performance Testing - Done");

        double totalTimeTaken = 0.0;
        long totalDecisionCycles = 0;

        for (TestRunner testRunner : testRunners)
        {
            totalTimeTaken += testRunner.getTotalCPUTime();
            totalDecisionCycles += testRunner.getTotalDecisionCycles();
        }

        out.println("Total Time Taken: " + totalTimeTaken + "\n" +
                    "Total Decision Cycles Run For: " + totalDecisionCycles + "\n");

        out.flush();
        
        out.println("\n\nIndividual Run Results:\n");
        
        for (TestRunner testRunner : testRunners)
        {
            List<Double> cpuTimes = testRunner.getAllCPUTimes();
            List<Double> kernelTimes = testRunner.getAllKernelTimes();
            List<Integer> decisionCycles = testRunner.getAllDecisionCycles();
            List<Long> memoryLoads = testRunner.getAllMemoryLoads();
            
            for (int i = 0;i < runCount;i++)
            {
                Table table = new Table();
                
                for (int j = 0;j < 5;j++)
                {
                    Row row = new Row();
                    
                    switch (j)
                    {
                    case 0:
                        row.add(new Cell(testRunner.getTest().getTestName() + " - Run " + (i+1)));
                        row.add(new Cell(testRunner.getTest().getDisplayName()));
                        break;
                    case 1:
                        row.add(new Cell("CPU Time (s)"));
                        row.add(new Cell(new Double(cpuTimes.get(i)).toString()));
                        break;
                    case 2:
                        row.add(new Cell("Kernel Time (s)"));
                        row.add(new Cell(new Double(kernelTimes.get(i)).toString()));
                        break;
                    case 3:
                        row.add(new Cell("Decision Cycles Total"));
                        row.add(new Cell(new Double(decisionCycles.get(i)).toString()));
                        break;
                    case 4:
                        row.add(new Cell("Memory Load (M)"));
                        row.add(new Cell(new Double(memoryLoads.get(i) / 1000.0 / 1000.0 ).toString()));
                    }
                    
                    table.addRow(row);
                }

                table.writeToWriter(out);
                
                if (csvDirectory.length() != 0)
                {
                    File out = new File(csvDirectory);
                    
                    if (!out.exists())
                    {
                        out.mkdirs();
                    }
                                        
                    String testNameWithoutSpaces = testRunner.getTest().getTestName().replaceAll("\\s+", "-") + "-" + testRunner.getTest().getDisplayName() + "-Run" + (i+1);
                    
                    table.writeToCSV(csvDirectory + "/" + testNameWithoutSpaces + ".txt");
                }
                
//                out.println("Run " + (i+1) + " of '" + testRunner.getTest().getTestName() + "' using " + testRunner.getTest().getDisplayName() + ":");
//                out.println("CPU Time: " + cpuTimes.get(i));
//                out.println("Kernel Time: " + kernelTimes.get(i));
//                out.println("Decision Cycles Total: " + decisionCycles.get(i));
//                out.println("Memory Load: " + memoryLoads.get(i) / 1000.0 / 1000.0 + "M");
                
//                out.print("\n");
            }
        }
        
        out.flush();
        
        return EXIT_SUCCESS;
    }

}

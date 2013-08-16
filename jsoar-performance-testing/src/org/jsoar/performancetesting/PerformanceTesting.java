/**
 * 
 */
package org.jsoar.performancetesting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    // Static + Constants
    
    private static final String SUMMARY_FILE_NAME = "test-summaries.txt";

    private static enum Options
    {
        help, Configuration, Test, output, warmup, jsoar, soar, uniqueJVMs, decisions, run, name, NoSummary, Single
    };
    
    private static final int NON_EXIT = 1024;
    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_FAILURE = 255;
    private static final int EXIT_FAILURE_TEST = 254;
    private static final int EXIT_FAILURE_CONFIGURATION = 253;
    
    private static final TestSettings defaultTestSettings = new TestSettings(false, false, 2, 2, 0, 1, "", new HashMap<String, String>());
    
    // Locals
    
    private final PrintWriter out;

    // Tests
    
    private CSoarTestFactory csoarTestFactory;
    private final List<Test> csoarTests;

    private JSoarTestFactory jsoarTestFactory;
    private final List<Test> jsoarTests;
    
    // Class Specific Configuration
    
    private String name = "";
    private int runNumber = -1;
    private boolean runTestsInSeparateJVMs = true;
    private boolean outputToSummaryFile = true;
    private boolean singleTest = false;
    
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

        this.jsoarTests = new ArrayList<Test>();
        this.csoarTests = new ArrayList<Test>();

        this.jsoarTestFactory = new JSoarTestFactory();
        this.csoarTestFactory = new CSoarTestFactory();
    }
    
    /**
     * 
     * @param args
     * @return Whether performance testing was successful or not.
     */
    public int doPerformanceTesting(String[] args)
    {
        int optionsParseResult = parseOptions(args);
        
        if (optionsParseResult != NON_EXIT)
        {
        	return optionsParseResult;
        }

        // Delete summary file so we don't append to old results
        // but only do this if we're not running in separate JVMs
        // because otherwise we would overwrite the old results!
        if (!runTestsInSeparateJVMs)
        {
            File summaryFile = new File(defaultTestSettings.getCSVDirectory() + "/" + SUMMARY_FILE_NAME);
            summaryFile.delete();
        }
        
        if (!singleTest)
        {
            out.println("Performance Testing - Starting Tests\n");
            out.flush();
        }
        
        List<Test> tests = null;
        
        if (defaultTestSettings.isJSoarEnabled())
        {
            tests = jsoarTests;
        }
        else
        {
            tests = csoarTests;
        }
        
        if (tests.size() > 1 && runTestsInSeparateJVMs)
        {
            return runTestsInChildrenJVMs(tests);
        }
        else
        {
            return runTestsWithoutChildJVMs(tests);
        }
    }
  
    /**
     * Outputs to the PrintWriter the usage string.
     */
    public void usage()
    {
        out.println("Performance Testing - Performance testing framework for JSoar and CSoar\n" +
                    "performance-testing [options]\n" +
                    "\n" +
                    "Options:\n" +
                    "   -h, --help              This message.\n" +
                    "   -C, --configuration     Load a configuration file to use for testing.\n" +
                    "   -T, --test              Manually specify a test to load.\n" +
                    "   -o, --output            The directory for all the CSV test results.\n" +
                    "   -w, --warmup            Specify the number of warm up runs for JSoar.\n" +
                    "   -c, --category              Specify the test category.\n" +
                    "   -j, --jsoar             Run the tests in JSoar.\n" +
                    "   -s, --soar              Run the tests in CSoar specifying the directory as well.\n" +
                    "   -u, --uniqueJVMs        Whether to run the tests in seperate jvms or not." +
                    "   -d, --decisions         Run the tests specified number of decisions." +
                    "   -r, --run               The run number." +
                    "   -n, --name              Used in conjunction with -T, specifies the test's name." +
                    "   -N, --nosummary         Don't output results to a summary file." +
                    "   -S, --single            Don't output any of the start and done, only the results." +
                    "\n" +
                    "Note: When running with CSoar, CSoar's bin directory must be on the system\n" +
                    "      path or in java.library.path or specified in a configuration directory.\n");
    }
    
    private int parseOptions(String[] args)
    {
        //This is the same options processor for JSoar and so has the same limitations.
        final OptionProcessor<Options> options = new OptionProcessor<Options>();
        options.newOption(Options.help)
               .newOption(Options.Configuration).requiredArg()
               .newOption(Options.Test).requiredArg()
               .newOption(Options.jsoar)
               .newOption(Options.output).requiredArg()
               .newOption(Options.soar).requiredArg()
               .newOption(Options.warmup).requiredArg()
               .newOption(Options.uniqueJVMs).requiredArg()
               .newOption(Options.decisions).requiredArg()
               .newOption(Options.run).requiredArg()
               .newOption(Options.name).requiredArg()
               .newOption(Options.NoSummary)
               .newOption(Options.Single)
               .done();

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
        
        if (options.has(Options.Configuration))
        {
            parseConfiguration(options);
            return NON_EXIT;
        }
        
        return parseCLIOptions(options);
    }
    
    private int parseConfiguration(OptionProcessor<Options> options)
    {
        String configurationPath = options.get(Options.Configuration);

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
        
        defaultTestSettings.setSeed(config.getSeed());

        if (config.getRunCount() > 0)
            defaultTestSettings.setRunCount(config.getRunCount());
        else
            out.println("Defaulting to running tests 20 times");

        if (config.getWarmUpCount() >= 0)
            defaultTestSettings.setWarmUpCount(config.getWarmUpCount());
        else
            out.println("Defaulting to warming up tests 10 times");

        defaultTestSettings.setJSoarEnabled(config.getJSoarEnabled());
        defaultTestSettings.setCSoarEnabled(config.getCSoarEnabled());
        
        runTestsInSeparateJVMs = config.getRunTestsInSeparateJVMs();

        if (!defaultTestSettings.isJSoarEnabled() && !defaultTestSettings.isCSoarEnabled())
        {
            out.println("WARNING: You must select something to run.  Defaulting to JSoar.");
            defaultTestSettings.setJSoarEnabled(true);
        }
        
        String csoarDirectory = config.getCSoarDirectory();
        String csoarLabel = config.getCSoarLabel();
        
        defaultTestSettings.setCSVDirectory(config.getCSVDirectory().trim());

        csoarTestFactory.setLabel(csoarLabel);
        csoarTestFactory.setCSoarDirectory(csoarDirectory);
        Map<String, String> csoarDirectories = new HashMap<String,String>();
        csoarDirectories.put("CSoar", csoarDirectory);
        
        defaultTestSettings.setCSoarVersions(csoarDirectories);

        SortedSet<Configuration.ConfigurationTest> configurationTests = config.getConfigurationTests();

        //Convert all the ConfigurationTest holders to actual tests.
        for (Configuration.ConfigurationTest test : configurationTests)
        {
            TestSettings newSettings = new TestSettings(defaultTestSettings);
            newSettings.setDecisionCycles(config.getDecisionCyclesToRunTest(test.getTestName()));
            
            if (defaultTestSettings.isJSoarEnabled())
            {
                Test jsoarTest = jsoarTestFactory.createTest(test.getTestName(), test.getTestFile(), newSettings);
                jsoarTests.add(jsoarTest);
            }

            if (defaultTestSettings.isCSoarEnabled())
            {
                Test csoarTest = csoarTestFactory.createTest(test.getTestName(), test.getTestFile(), newSettings);
                csoarTests.add(csoarTest);
            }
        }
        
        return NON_EXIT;
    }
    
    private int parseCLIOptions(OptionProcessor<Options> options)
    {
        if (options.has(Options.output))
        {
            defaultTestSettings.setCSVDirectory(options.get(Options.output));
        }
        
        if (options.has(Options.warmup))
        {
            defaultTestSettings.setWarmUpCount(Integer.parseInt(options.get(Options.warmup)));
        }
        
        if (options.has(Options.jsoar))
        {
            defaultTestSettings.setJSoarEnabled(true);
        }
        
        if (options.has(Options.soar))
        {
            defaultTestSettings.setCSoarEnabled(true);
            Map<String, String> tempMap = new HashMap<String, String>();
            tempMap.put("CSoar", options.get(Options.soar));
            defaultTestSettings.setCSoarVersions(tempMap);
            
            csoarTestFactory.setCSoarDirectory(options.get(Options.soar));
        }
        
        if (options.has(Options.decisions))
        {
            defaultTestSettings.setDecisionCycles(Integer.parseInt(options.get(Options.decisions)));
        }
        
        if (options.has(Options.run))
        {
            runNumber = Integer.parseInt(options.get(Options.run));
            defaultTestSettings.setRunCount(1);
        }
        
        if (options.has(Options.name))
        {
            name = options.get(Options.name);
        }
        
        if (options.has(Options.NoSummary))
        {
            this.outputToSummaryFile = false;
        }
        
        if (options.has(Options.Single))
        {
            singleTest = true;
        }
        
        // This will load an individual test into the uncategorized tests category, only really useful
        // for single tests that you don't want to create a configuration file for
        if (options.has(Options.Test))
        {
            String testPath = options.get(Options.Test);

            if (!testPath.endsWith(".soar"))
            {
                out.println("Tests need to end with .soar");
                return EXIT_FAILURE_TEST;
            }

            String testName = (new File(testPath)).getName();
            
            if (this.name.length() != 0)
            {
                testName = this.name;
            }

            if (defaultTestSettings.isJSoarEnabled())
            {
                Test jsoarTest = jsoarTestFactory.createTest(testName, testPath, defaultTestSettings);
                jsoarTests.add(jsoarTest);
            }

            if (defaultTestSettings.isCSoarEnabled())
            {
                Test csoarTest = csoarTestFactory.createTest(testName, testPath, defaultTestSettings);
                csoarTests.add(csoarTest);
            }
        }
        
        if (options.has(Options.uniqueJVMs))
        {
            runTestsInSeparateJVMs = Boolean.parseBoolean(options.get(Options.uniqueJVMs));
        }
        
        return NON_EXIT;
    }
    
    /**
     * 
     * @param tests All the tests
     * @param testCategories The categories of all the tests
     * @return Whether running the tests in child JVMs was successful
     */
    private int runTestsInChildrenJVMs(List<Test> tests)
    {
        // Since we have more than one test to run, spawn a separate JVM for each run.
        for (int j = 0; j < tests.size(); j++)
        {
            Test test = tests.get(j);
                
            if (test.getTestSettings().isJSoarEnabled())
            {                    
                spawnChildJVMForTest(jsoarTests.get(j), true);
            }
                
            if (test.getTestSettings().isCSoarEnabled())
            {                    
                spawnChildJVMForTest(csoarTests.get(j), false);
            }
            
            // Generate a summary file
            appendToSummaryFile(test);
        }
        
        out.println("Performance Testing - Done");
        
        return EXIT_SUCCESS;
    }
    
    /**
     * Spawns a child JVM for the test and waits for it to exit.
     * 
     * @param test The test to run
     * @param jsoar Whether this is a JSoar or CSoar test
     * @return Whether the run was successful or not
     */
    private int spawnChildJVMForTest(Test test, boolean jsoar)
    {
        // Arguments to the process builder including the command to run
        List<String> arguments = new ArrayList<String>();
        
        URL baseURL = PerformanceTesting.class.getProtectionDomain().getCodeSource().getLocation();
        String jarPath = null;
        try
        {
            // Get the directory for the jar file or class path
            jarPath = new File(baseURL.toURI().getPath()).getCanonicalPath();
        }
        catch (IOException | URISyntaxException e1)
        {
            throw new AssertionError(e1);
        }
        
        // Replaces windows style paths with unix.
        jarPath = jarPath.replace("\\","/");
                
        if (jarPath.endsWith(".jar") != true)
        {
            // We're not in a jar so most likely eclipse
            // Set up class path
            
            // This is going to make the assumption that we're in eclipse
            // and so it will assume a repository structure and if this
            // changes or is wrong, this code will break and this will fail.
            // - ALT
            
            // Add the performance testing framework class path
            String originalPath = jarPath;
            
            jarPath += ";";
            // Add the jsoar core class path
            jarPath += originalPath + "/../../jsoar-core/bin/;";
            
            // Add all the require libs from jsoar core
            File directory = new File(originalPath + "/../../jsoar-core/lib/");
            File[] listOfFiles = directory.listFiles();
            for (File file : listOfFiles)
            {
                if (file.isFile())
                {
                    String path = file.getPath();
                    
                    if (!path.endsWith(".jar"))
                    {
                        continue;
                    }
                    
                    path = path.replace("\\", "/");
                    
                    jarPath += path + ";";
                }
            }
            
            // Add all the required libs from the performance testing framework
            directory = new File(originalPath + "/../lib/");
            listOfFiles = directory.listFiles();
            for (File file : listOfFiles)
            {
                if (file.isFile())
                {
                    String path = file.getPath();
                    
                    if (!path.endsWith(".jar"))
                    {
                        continue;
                    }
                    
                    path = path.replace("\\", "/");
                    
                    jarPath += path + ";";
                }
            }
        }
        
        // Construct the array with the command and arguments
        // Use javaw for no console window spawning
        arguments.add("java");
        arguments.add("-classpath"); // Always use class path.  This will even work for jars.
        arguments.add("\"" + jarPath + "\"");
        arguments.add(PerformanceTesting.class.getCanonicalName()); // Get the class name to load
        arguments.add("--test");
        arguments.add(test.getTestFile());
        arguments.add("--output");
        arguments.add(test.getTestSettings().getCSVDirectory());
        arguments.add("--warmup");
        arguments.add(new Integer(test.getTestSettings().getWarmUpCount()).toString());
        arguments.add("--" + (jsoar ? "j" : "") + "soar");
        if (!jsoar)
        {
            arguments.add(test.getTestSettings().getCSoarVersions().get("CSoar"));
        }
        arguments.add("--decisions");
        arguments.add(new Integer(test.getTestSettings().getDecisionCycles()).toString());
        arguments.add("--name");
        arguments.add(test.getTestName());
        arguments.add("--nosummary");
        arguments.add("--single");
        
        // Run the process and get the exit code
        int exitCode = 0;
        try
        {
            for (int i = 1; i <= test.getTestSettings().getRunCount();i++)
            {
                List<String> argumentsPerRun = new ArrayList<String>(arguments);
                argumentsPerRun.add("--run");
                argumentsPerRun.add(new Integer(i).toString());
                
                ProcessBuilder processBuilder = new ProcessBuilder(argumentsPerRun);

                // Redirect the output so we can see what is going on
                processBuilder.redirectError(Redirect.INHERIT);
                processBuilder.redirectOutput(Redirect.INHERIT);

                Process process = processBuilder.start();
                exitCode = process.waitFor();
                
                out.flush();
            }
        }
        catch (IOException e)
        {
            throw new AssertionError(e);
        }
        catch (InterruptedException e)
        {
            throw new AssertionError(e);
        }
        
        if (exitCode != 0)
        {
            return EXIT_FAILURE_TEST;
        }
        else
        {
            return EXIT_SUCCESS;
        }
    }
    
    private void appendToSummaryFile(Test test)
    {
        TestSettings settings = test.getTestSettings();
        Table summaryTable = new Table();
        
        for (int k = 0;k < 17;k++)
        {
            Row row = new Row();
            
            switch (k)
            {
            case 0:
                row.add(new Cell(test.getTestName()));
                row.add(new Cell("JSoar"));
                row.add(new Cell("CSoar"));
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
                row.add(new Cell("CPU Time Deviation from Average (s)"));
                break;
            case 5:
                row.add(new Cell("Total Kernel Time (s)"));
                break;
            case 6:
                row.add(new Cell("Average Kernel Time Per Run (s)"));
                break;
            case 7:
                row.add(new Cell("Median Kernel Time Per Run (s)"));
                break;
            case 8:
                row.add(new Cell("Kernel Time Deviation from Average (s)"));
                break;
            case 9:
                row.add(new Cell("Decision Cycles Run For"));
                break;
            case 10:
                row.add(new Cell("Average Decision Cycles Per Run"));
                break;
            case 11:
                row.add(new Cell("Median Decision Cycles Per Run"));
                break;
            case 12:
                row.add(new Cell("Decision Cycles Deviation from Average"));
                break;
            case 13:
                row.add(new Cell("Memory Used (M)"));
                break;
            case 14:
                row.add(new Cell("Average Memory Used Per Run (M)"));
                break;
            case 15:
                row.add(new Cell("Median Memory Used Per Run (M)"));
                break;
            case 16:
                row.add(new Cell("Memory Deviation from Average (M)"));
                break;
            }
            
            summaryTable.addRow(row);
        }
        
        if (settings.isJSoarEnabled())
        {
            // JSoar
            List<Double> cpuTimes = new ArrayList<Double>();
            List<Double> kernelTimes = new ArrayList<Double>();
            List<Double> decisionCycles = new ArrayList<Double>();
            List<Double> memoryLoads = new ArrayList<Double>();
            
            for (int i = 1;i <= settings.getRunCount();i++)
            {
                File testFile = new File(settings.getCSVDirectory() + "/" + test.getTestName() + "-JSoar-" + (new Integer(i)).toString() + ".txt");
                
                try
                {
                    BufferedReader br = new BufferedReader(new FileReader(testFile));
                    String line;
                    // This will skip the first and last fields and only use the total fields
                    // for getting values since these are individual runs.
                    
                    // CPU Time, Kernel Time, Decisions
                    for (int j = 0;j <= 10;j++)
                    {
                        line = br.readLine();
                        
                        assert(line != null);
                        
                        String[] list = line.split("\t");
                        
                        switch (j)
                        {
                        case 1:
                            // CPU Time
                            cpuTimes.add(Double.parseDouble(list[1]));
                            break;
                        case 4:
                            kernelTimes.add(Double.parseDouble(list[1]));
                            break;
                        case 7:
                            decisionCycles.add(Double.parseDouble(list[1]));
                            break;
                        case 10:
                            memoryLoads.add(Double.parseDouble(list[1]));
                            break;
                        }
                    }
                    br.close();
                }
                catch (IOException e)
                {
                    throw new AssertionError(e);
                }
            }
            
            // Now calculate everything
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateTotal(cpuTimes)),            1, 2-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateAverage(cpuTimes)),          2, 2-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateMedian(cpuTimes)),           3, 2-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateDeviation(cpuTimes)),        4, 2-1);
            
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateTotal(kernelTimes)),         5, 2-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateAverage(kernelTimes)),       6, 2-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateMedian(kernelTimes)),        7, 2-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateDeviation(kernelTimes)),     8, 2-1);
            
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateTotal(decisionCycles)),      9, 2-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateAverage(decisionCycles)),    10, 2-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateMedian(decisionCycles)),     11, 2-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateDeviation(decisionCycles)),  12, 2-1);
            
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateTotal(memoryLoads)),         13, 2-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateAverage(memoryLoads)),       14, 2-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateMedian(memoryLoads)),        15, 2-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateDeviation(memoryLoads)),     16, 2-1);
        }
        
        if (settings.isCSoarEnabled())
        {
            // CSoar
            List<Double> cpuTimes = new ArrayList<Double>();
            List<Double> kernelTimes = new ArrayList<Double>();
            List<Double> decisionCycles = new ArrayList<Double>();
            List<Double> memoryLoads = new ArrayList<Double>();
            
            for (int i = 1;i <= settings.getRunCount();i++)
            {
                File testFile = new File(settings.getCSVDirectory() + "/" + test.getTestName() + "-CSoar-" + (new Integer(i)).toString() + ".txt");
                
                try
                {
                    BufferedReader br = new BufferedReader(new FileReader(testFile));
                    String line;
                    // This will skip the first and last fields and only use the total fields
                    // for getting values since these are individual runs.
                    
                    // CPU Time, Kernel Time, Decisions
                    for (int j = 0;j <= 10;j++)
                    {
                        line = br.readLine();
                                                
                        assert(line != null);
                        
                        String[] list = line.split("\t");
                        
                        switch (j)
                        {
                        case 1:
                            // CPU Time
                            cpuTimes.add(Double.parseDouble(list[2]));
                            break;
                        case 4:
                            kernelTimes.add(Double.parseDouble(list[2]));
                            break;
                        case 7:
                            decisionCycles.add(Double.parseDouble(list[2]));
                            break;
                        case 10:
                            memoryLoads.add(Double.parseDouble(list[2]));
                            break;
                        }
                    }
                    br.close();
                }
                catch (IOException e)
                {
                    throw new AssertionError(e);
                }
            }
            
            // Now calculate everything
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateTotal(cpuTimes)),            1, 3-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateAverage(cpuTimes)),          2, 3-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateMedian(cpuTimes)),           3, 3-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateDeviation(cpuTimes)),        4, 3-1);
            
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateTotal(kernelTimes)),         5, 3-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateAverage(kernelTimes)),       6, 3-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateMedian(kernelTimes)),        7, 3-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateDeviation(kernelTimes)),     8, 3-1);
            
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateTotal(decisionCycles)),      9, 3-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateAverage(decisionCycles)),    10, 3-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateMedian(decisionCycles)),     11, 3-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateDeviation(decisionCycles)),  12, 3-1);
            
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateTotal(memoryLoads)),         13, 3-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateAverage(memoryLoads)),       14, 3-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateMedian(memoryLoads)),        15, 3-1);
            summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateDeviation(memoryLoads)),     16, 3-1);
        }
        
        String summaryFilePath = settings.getCSVDirectory() + "/" + SUMMARY_FILE_NAME;
        
        summaryTable.writeToCSV(summaryFilePath, '\t', true);
    }
    
    private String doubleToString(Double d)
    {
        DecimalFormat df = new DecimalFormat("#0.000######");
        return df.format(d);
    }
    
    /**
     * 
     * @param tests All the tests
     * @param testCategories All the categories for tests
     * @return Whether running the tests without child JVMs was successful or not
     */
    private int runTestsWithoutChildJVMs(List<Test> tests)
    {
        List<TestRunner> testRunners = new ArrayList<TestRunner>();

        int runTestsResult = runTests(testRunners);
        
        if (runTestsResult != NON_EXIT)
        {
            return runTestsResult;
        }

        if (!singleTest)
        {
            out.println("Performance Testing - Done");
        }
        
        return EXIT_SUCCESS;
    }
    
    /**
     * 
     * @param testRunners All the tests
     * @param testCategories All the test categories
     * @return Whether running the tests was successful or not
     */
    private int runTests(List<TestRunner> testRunners)
    {
        List<Test> tests = null;

        if (defaultTestSettings.isJSoarEnabled())
            tests = jsoarTests;
        else
            tests = csoarTests;

        for (int j = 0; j < tests.size(); j++)
        {
            Test test = tests.get(j);

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
            if (defaultTestSettings.isJSoarEnabled())
            {
                out.println("JSoar: ");
                out.flush();

                Test jsoarTest = jsoarTests.get(j);

                TestRunner jsoarTestRunner = new TestRunner(jsoarTest, out);
                try
                {
                    jsoarTestRunner.runTestsForAverage(jsoarTest.getTestSettings());
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

                testRunners.add(jsoarTestRunner);
            }

            // Run CSoar Second
            if (defaultTestSettings.isCSoarEnabled())
            {
                Test csoarTest = csoarTests.get(j);
                out.println("CSoar " + csoarTestFactory.getLabel() + ": ");
                out.flush();

                TestRunner csoarTestRunner = new TestRunner(csoarTest, out);
                try
                {
                    csoarTestRunner.runTestsForAverage(csoarTest.getTestSettings());
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

                testRunners.add(csoarTestRunner);
            }

            if (test.getTestSettings().getCSVDirectory().length() != 0)
            {
                File outDirectory = new File(test.getTestSettings().getCSVDirectory());

                if (!outDirectory.exists())
                {
                    outDirectory.mkdirs();
                }

                String testNameWithoutSpaces = tests.get(j).getTestName().replaceAll("\\s+", "-");

                if (runNumber != -1)
                {
                    if (defaultTestSettings.isJSoarEnabled())
                    {
                        testNameWithoutSpaces += "-JSoar";
                    }
                    else
                    {
                        testNameWithoutSpaces += "-CSoar";
                    }

                    testNameWithoutSpaces += "-" + (new Integer(runNumber)).toString();
                }

                table.writeToCSV(test.getTestSettings().getCSVDirectory() + "/" + testNameWithoutSpaces + ".txt");

                if (outputToSummaryFile)
                {
                    table.writeToCSV(test.getTestSettings().getCSVDirectory() + "/" + SUMMARY_FILE_NAME, true);
                }
            }

            out.print("\n");
        }
        
        out.flush();

        return NON_EXIT;
    }
}

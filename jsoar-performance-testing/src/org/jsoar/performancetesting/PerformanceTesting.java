/**
 * 
 */
package org.jsoar.performancetesting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    
    private static enum Options
    {
        help, Configuration, Test, output, warmup, jsoar, soar, decisions, run, name, NoSummary, Single
    };
    
    private static final int NON_EXIT = 1024;
    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_FAILURE = 255;
    private static final int EXIT_FAILURE_TEST = 254;
    private static final int EXIT_FAILURE_CONFIGURATION = 253;
    
    private static TestSettings defaultTestSettings = new TestSettings(false, false, 0, 0, 0, false, 1, "", "", new ArrayList<String>(), "");
    
    // Locals
    
    private final PrintWriter out;

    // Tests
    
    private CSoarTestFactory csoarTestFactory;
    private Test csoarTest;

    private JSoarTestFactory jsoarTestFactory;
    private Test jsoarTest;
    
    // Class Specific Configuration
    
    private String name = "";
    private int runNumber = -1;
    private boolean outputToSummaryFile = true;
    private boolean singleTest = false;
    private Set<Configuration.ConfigurationTest> configurationTests;
    
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
        
        if (!singleTest)
        {
            out.printf("Performance Testing - Starting Tests - %d Tests Loaded\n\n", configurationTests.size());
            out.flush();
        }
        
        if (configurationTests != null)
        {
            return runTestsInChildrenJVMs(configurationTests);
        }
        else
        {
            if (jsoarTest != null)
            {
                return runTest(new TestRunner(jsoarTest, out));
            }
            else
            {
                return runTest(new TestRunner(csoarTest, out));
            }
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

        if (!configurationPath.endsWith(".yaml"))
        {
            out.println("Configuration files need to yaml files.");
            return EXIT_FAILURE_CONFIGURATION;
        }

        Configuration config = new Configuration(configurationPath);
        int result = Configuration.PARSE_FAILURE;

        //Make sure there are no duplicate keys and then parse the properties file
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
        
        defaultTestSettings = config.getDefaultSettings();
        
        if (!defaultTestSettings.isJSoarEnabled() && !defaultTestSettings.isCSoarEnabled())
        {
            out.println("WARNING: You must select something to run.  Defaulting to JSoar.");
            defaultTestSettings.setJSoarEnabled(true);
        }

        configurationTests = config.getConfigurationTests();

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
            List<String> tempArray = new ArrayList<String>();
            tempArray.add(options.get(Options.soar));
            defaultTestSettings.setCSoarVersions(tempArray);
            
            String path = options.get(Options.soar);
            
            csoarTestFactory.setLabel(path);
            csoarTestFactory.setCSoarDirectory(path);
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
                jsoarTest = jsoarTestFactory.createTest(testName, testPath, defaultTestSettings);
            }

            if (defaultTestSettings.isCSoarEnabled())
            {
                csoarTest = csoarTestFactory.createTest(testName, testPath, defaultTestSettings);
            }
        }
        
        return NON_EXIT;
    }
    
    /**
     * 
     * @param tests All the tests
     * @param testCategories The categories of all the tests
     * @return Whether running the tests in child JVMs was successful
     */
    private int runTestsInChildrenJVMs(Set<Configuration.ConfigurationTest> tests)
    {
        Set<String> previousSummaryFiles = new HashSet<String>();
        
        // Since we have more than one test to run, spawn a separate JVM for each run.
        for (Configuration.ConfigurationTest test : tests)
        {
            File dir = new File(test.getTestSettings().getCSVDirectory());
            
            if (!dir.exists())
            {
                dir.mkdirs();
            }
            
            String summaryFilePath = test.getTestSettings().getCSVDirectory() + "/" + test.getTestSettings().getSummaryFile();
            
            if (!previousSummaryFiles.contains(summaryFilePath))
            {
                PrintWriter summaryFileAppender;
                try
                {
                    summaryFileAppender = new PrintWriter(new BufferedWriter(new FileWriter(summaryFilePath, false)));
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
                
                DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd - HH:mm:ss");
                
                summaryFileAppender.println("Test Results - " + dateFormat.format(new Date()) + "\n");
                
                summaryFileAppender.close();
                
                previousSummaryFiles.add(summaryFilePath);
            }
            
            if (test.getTestSettings().isJSoarEnabled())
            {
                spawnChildJVMForTest(test, true);
            }
            
            if (test.getTestSettings().isCSoarEnabled())
            {                    
                spawnChildJVMForTest(test, false);
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
    private int spawnChildJVMForTest(Configuration.ConfigurationTest test, boolean jsoar)
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
            
            Character pathSeperator = File.pathSeparatorChar;
            
            // Add the performance testing framework class path
            String originalPath = jarPath;
            
            jarPath += pathSeperator;
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
                    
                    jarPath += path + pathSeperator;
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
                    
                    jarPath += path + pathSeperator;
                }
            }
        }
        
        // Construct the array with the command and arguments
        // Use javaw for no console window spawning
        arguments.add("java");
        arguments.addAll(Arrays.asList(test.getTestSettings().getJVMSettings().split("\\s")));
        arguments.add("-classpath"); // Always use class path.  This will even work for jars.
        arguments.add(jarPath);
        arguments.add(PerformanceTesting.class.getCanonicalName()); // Get the class name to load
        arguments.add("--test");
        arguments.add(test.getTestFile());
        arguments.add("--output");
        arguments.add(test.getTestSettings().getCSVDirectory());
        arguments.add("--warmup");
        arguments.add(new Integer(test.getTestSettings().getWarmUpCount()).toString());
        arguments.add("--decisions");
        arguments.add(new Integer(test.getTestSettings().getDecisionCycles()).toString());
        arguments.add("--name");
        arguments.add(test.getTestName());
        arguments.add("--nosummary");
        arguments.add("--single");
        
        // Run the process and get the exit code
        int exitCode = 0;
        
        if (jsoar)
        {
            arguments.add("--jsoar");

            exitCode = runJVM(test, arguments);
        }
        else
        {
            arguments.add("--soar");

            for (String path : test.getTestSettings().getCSoarVersions())
            {
                List<String> argumentsPerTest = new ArrayList<String>(arguments);
                argumentsPerTest.add("\"" + path + "\"");

                exitCode = runJVM(test, argumentsPerTest);
            }
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
    
    private int runJVM(Configuration.ConfigurationTest test, List<String> arguments)
    {
        int exitCode = 0;
        
        for (int i = 1; i <= test.getTestSettings().getRunCount();i++)
        {
            List<String> argumentsPerRun = new ArrayList<String>(arguments);
            argumentsPerRun.add("--run");
            argumentsPerRun.add(new Integer(i).toString());

            ProcessBuilder processBuilder = new ProcessBuilder(argumentsPerRun);

            // Redirect the output so we can see what is going on
            processBuilder.redirectError(Redirect.INHERIT);
            processBuilder.redirectOutput(Redirect.INHERIT);

            Process process;
            try
            {
                process = processBuilder.start();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            try
            {
                exitCode = process.waitFor();
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }

            out.flush();
        }
        
        return exitCode;
    }
    
    private void appendToSummaryFile(Configuration.ConfigurationTest test)
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
                for (String csoarPath : test.getTestSettings().getCSoarVersions())
                {
                    row.add(new Cell("CSoar " + csoarPath));
                }
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
            int column = 3-1;
            
            for (String csoarPath : test.getTestSettings().getCSoarVersions())
            {
                String csoarLabel = "-CSoar-" + csoarPath.replaceAll("[^a-zA-Z0-9]+", "") + "-";
                
                // CSoar
                List<Double> cpuTimes = new ArrayList<Double>();
                List<Double> kernelTimes = new ArrayList<Double>();
                List<Double> decisionCycles = new ArrayList<Double>();
                List<Double> memoryLoads = new ArrayList<Double>();

                for (int i = 1;i <= settings.getRunCount();i++)
                {
                    File testFile = new File(settings.getCSVDirectory() + "/" + test.getTestName() + csoarLabel + (new Integer(i)).toString() + ".txt");

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
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateTotal(cpuTimes)),            1, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateAverage(cpuTimes)),          2, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateMedian(cpuTimes)),           3, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateDeviation(cpuTimes)),        4, column);

                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateTotal(kernelTimes)),         5, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateAverage(kernelTimes)),       6, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateMedian(kernelTimes)),        7, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateDeviation(kernelTimes)),     8, column);

                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateTotal(decisionCycles)),      9, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateAverage(decisionCycles)),    10, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateMedian(decisionCycles)),     11, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateDeviation(decisionCycles)),  12, column);

                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateTotal(memoryLoads)),         13, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateAverage(memoryLoads)),       14, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateMedian(memoryLoads)),        15, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics.calculateDeviation(memoryLoads)),     16, column);
                
                column++;
            }
        }
        
        String summaryFilePath = settings.getCSVDirectory() + "/" + settings.getSummaryFile();
        
        summaryTable.writeToCSV(summaryFilePath, '\t', true);
    }
    
    private String doubleToString(Double d)
    {
        DecimalFormat df = new DecimalFormat("#0.000######");
        return df.format(d);
    }
    
    /**
     * 
     * @param testRunners All the tests
     * @param testCategories All the test categories
     * @return Whether running the tests was successful or not
     */
    private int runTest(TestRunner testRunner)
    {
        Test test = testRunner.getTest();
        TestSettings settings = test.getTestSettings();

        out.println("Starting Test: " + test.getTestName());
        out.flush();

        //Construct a table for the data

        Table table = new Table();

        for (int k = 0;k < 14;k++)
        {
            Row row = new Row();

            switch (k)
            {
            case 0:
                row.add(new Cell(test.getTestName()));
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
        
        int column = 2-1;

        // Run JSoar
        if (settings.isJSoarEnabled())
        {
            out.println("JSoar: ");
            out.flush();

            column = 2-1;
        }
        else if (settings.isCSoarEnabled()) // Or CSoar
        {
            out.println("CSoar " + csoarTestFactory.getLabel() + ": ");
            out.flush();

            column = 3-1;
        }
        
        try
        {
            testRunner.runTestsForAverage(settings);
        }
        catch (SoarException e)
        {
            out.println("Failed with a Soar Exception: " + e.getMessage());
            return EXIT_FAILURE;
        }
        
        table.setOrAddValueAtLocation(new Double(testRunner.getTotalCPUTime()).toString(),          2-1,   column);
        table.setOrAddValueAtLocation(new Double(testRunner.getAverageCPUTime()).toString(),        3-1,   column);
        table.setOrAddValueAtLocation(new Double(testRunner.getMedianCPUTime()).toString(),         4-1,   column);
        table.setOrAddValueAtLocation(new Double(testRunner.getTotalKernelTime()).toString(),       5-1,   column);
        table.setOrAddValueAtLocation(new Double(testRunner.getAverageKernelTime()).toString(),     6-1,   column);
        table.setOrAddValueAtLocation(new Double(testRunner.getMedianKernelTime()).toString(),      7-1,   column);
        table.setOrAddValueAtLocation(new Double(testRunner.getTotalDecisionCycles()).toString(),   8-1,   column);
        table.setOrAddValueAtLocation(new Double(testRunner.getAverageDecisionCycles()).toString(), 9-1,   column);
        table.setOrAddValueAtLocation(new Double(testRunner.getMedianDecisionCycles()).toString(),  10-1,  column);
        table.setOrAddValueAtLocation(new Double(testRunner.getTotalMemoryLoad()/ 1000.0 / 1000.0).toString(),       11-1,  column);
        table.setOrAddValueAtLocation(new Double(testRunner.getAverageMemoryLoad()/ 1000.0 / 1000.0).toString(),     12-1,  column);
        table.setOrAddValueAtLocation(new Double(testRunner.getMedianMemoryLoad()/ 1000.0 / 1000.0).toString(),      13-1,  column);                    
        table.setOrAddValueAtLocation(new Double(testRunner.getMemoryLoadDeviation()/ 1000.0 / 1000.0).toString(),   14-1,  column);

        if (settings.getCSVDirectory().length() != 0)
        {
            File outDirectory = new File(test.getTestSettings().getCSVDirectory());

            if (!outDirectory.exists())
            {
                outDirectory.mkdirs();
            }

            String testNameWithoutSpaces = test.getTestName().replaceAll("\\s+", "-");

            if (runNumber != -1)
            {
                if (settings.isJSoarEnabled())
                {
                    testNameWithoutSpaces += "-JSoar";
                }
                else
                {
                    testNameWithoutSpaces += "-CSoar";
                    
                    if (csoarTestFactory.getLabel().length() != 0)
                    {
                        testNameWithoutSpaces += "-";
                        testNameWithoutSpaces += csoarTestFactory.getLabel().replaceAll("[^a-zA-Z0-9]+", "");
                    }
                }

                testNameWithoutSpaces += "-" + (new Integer(runNumber)).toString();
            }

            table.writeToCSV(settings.getCSVDirectory() + "/" + testNameWithoutSpaces + ".txt");

            if (outputToSummaryFile)
            {
                table.writeToCSV(settings.getCSVDirectory() + "/" + settings.getSummaryFile(), true);
            }
        }

        out.print("\n");

        out.flush();

        return NON_EXIT;
    }
}

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
    
    private static TestSettings defaultTestSettings = new TestSettings(false, false, 0, 0, new ArrayList<Integer>(), false, 1, "", "", new ArrayList<String>(), "");
    
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
    
    // Used for killing the current child process in the shutdown hook
    private Process currentChildProcess = null;
    
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
        // This adds a shutdown hook to the runtime
        // to kill any child processes spawned that
        // are still running.  This handles CTRL-C
        // as well as normal kills, SIGKILL and
        // SIGHALT I believe
        // - ALT
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
           @Override
           public void run()
           {
               if (currentChildProcess != null)
               {
                   currentChildProcess.destroy();
               }
           }
        });
        
        // Parse the CLI options and Configuration options
        // if there are any
        int optionsParseResult = parseOptions(args);
        
        if (optionsParseResult != NON_EXIT)
        {
        	return optionsParseResult;
        }
        
        // If this is not a single test and configurationTests is null
        // exit because the configuration file didn't work out
        // or something else bad happened.
        if (!singleTest && configurationTests == null)
        {
            out.println("Did not load any tests or configuration.");
            usage();
            return EXIT_SUCCESS;
        }
        
        // Only output starting information like this if we're not in a child process
        if (!singleTest)
        {
            out.printf("Performance Testing - Starting Tests - %d Tests Loaded\n\n", configurationTests.size());
            out.flush();
        }
        
        // If we have multiple tests to run, then run
        // them in children JVMs
        if (configurationTests != null)
        {
            return runTestsInChildrenJVMs(configurationTests);
        }
        else
        {
            // In this case, we are running just one test which means
            // the test is either going to be jsoarTest or csoarTest.
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
                    "   -c, --category          Specify the test category.\n" +
                    "   -j, --jsoar             Run the tests in JSoar.\n" +
                    "   -s, --soar              Run the tests in CSoar specifying the directory as well.\n" +
                    "   -u, --uniqueJVMs        Whether to run the tests in seperate jvms or not.\n" +
                    "   -d, --decisions         Run the tests specified number of decisions.\n" +
                    "   -r, --run               The run number.\n" +
                    "   -n, --name              Used in conjunction with -T, specifies the test's name.\n" +
                    "   -N, --nosummary         Don't output results to a summary file.\n" +
                    "\n" +
                    "Note: When running with CSoar, CSoar's bin directory must be on the system\n" +
                    "      path or in java.library.path or specified in a configuration file.\n");
    }
    
    /**
     * Parses the CLI and Configuration Options
     * 
     * @param args
     * @return whether the parsing was successful or not
     */
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
        
        if (options.has(Options.help) || args.length == 0)
        {
            usage();
            return EXIT_SUCCESS;
        }
        
        // If there is a configuration option,
        // ignore any CLI options beyond that.
        if (options.has(Options.Configuration))
        {
            parseConfiguration(options);
            
            return NON_EXIT;
        }
        
        // Since we don't have a configuration
        // option, parse the CLI arguments.
        return parseCLIOptions(options);
    }
    
    /**
     * Parse a configuration file from a given options processor.
     * This will do any validity checks on the configuration file
     * as well.
     * 
     * @param options
     * @return whether the parsing was successful or not.
     */
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
        catch (IOException e)
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
    
    /**
     * Parse the CLI arguments
     * 
     * @param options
     * @return whether the parsing was successful or not
     */
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
           List<Integer> decisions = new ArrayList<Integer>();
           
           decisions.add(Integer.parseInt(options.get(Options.decisions)));
           
            defaultTestSettings.setDecisionCycles(decisions);
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
        int i = 0;
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
            
            out.println("--------------------------------------------------");
            out.println("Starting " + test.getTestName() + " Test (" + (++i) + "/" + tests.size() + ")");
            out.println("--------------------------------------------------");
            
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
            throw new RuntimeException(e1);
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
           
            // If we are in a jar, then the jar file will contain jsoar-core
            // internally and so we don't need to setup the classpath
            // - ALT
            
            Character pathSeperator = File.pathSeparatorChar;
            
            // Add the performance testing framework class path
            String originalPath = jarPath;
            
            jarPath += pathSeperator;
            // Add the jsoar core class path
            jarPath += originalPath + "/../../jsoar-core/bin/" + pathSeperator;
            
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
            
            // Add the database driver from jsoar-core to the class path
            // and any other dependencies in here
            directory = new File(originalPath + "/../../jsoar-core/lib/db");
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

            // For each version of CSoar, run a child JVM
            if(test.getTestSettings().getCSoarVersions() == null){
                throw new RuntimeException("CSoar Enabled but no versions specified");
            }
            for (String path : test.getTestSettings().getCSoarVersions())
            {
                List<String> argumentsPerTest = new ArrayList<String>(arguments);
                argumentsPerTest.add(path);

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
    
    /**
     * This runs a test in a child JVM with the given arguments
     * 
     * @param test
     * @param arguments
     * @return whether running the child JVM was successful or not
     */
    private int runJVM(Configuration.ConfigurationTest test, List<String> arguments)
    {
        int exitCode = 0;
        
        // Run the test in a new child JVM for each run
        for (int i = 1; i <= test.getTestSettings().getRunCount();i++)
        {
            List<String> argumentsPerRun = new ArrayList<String>(arguments);
            argumentsPerRun.add("--run");
            argumentsPerRun.add(new Integer(i).toString());
            
            out.println("Starting Test - " + test.getTestName() + " - " + i + "/" + test.getTestSettings().getRunCount());
            
            // For each decision cycle count in the list, run a new child JVM
            for (Integer j : test.getTestSettings().getDecisionCycles())
            {
                List<String> argumentsPerCycle = new ArrayList<String>(argumentsPerRun);
                argumentsPerCycle.add("--decisions");
                argumentsPerCycle.add(j.toString());
                
                String decisionsString;
                
                if (j == 0)
                {
                    decisionsString = "Forever";
                }
                else
                {
                    decisionsString = "for " + j + " decisions";
                }
                
                out.println("Running " + decisionsString);
                out.flush();
                
                // This is Java 1.7 only
                
                // Create a new process builder for the child JVM
                ProcessBuilder processBuilder = new ProcessBuilder(argumentsPerCycle);
                
                Process process = null;
                try
                {
                    // Start the process
                    process = processBuilder.start();
                    // Setup the parameters for the shutdown hook,
                    // in case we're killed off early.
                    currentChildProcess = process;
                    
                    // Create some StreamGobblers to handle output to the screen
                    // We don't redirectIO here because on Windows in CMD, we
                    // won't redirect to the console output.  Instead we will
                    // redirect to the original java System.out which isn't the
                    // screen.  This means that the output both, won't show up
                    // and we could potentially crash the JVM if we output enough.
                    // - ALT
                    StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), out);
                    StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), out);
                    
                    // Start the gobblers
                    outputGobbler.start();
                    errorGobbler.start();
                    
                    // Wait for the process to exit and then get the exit code
                    exitCode = process.waitFor();
                    // Make sure we don't try to kill a non-existent process
                    // if we are shutdown after this.
                    currentChildProcess = null;
                }
                catch (IOException | InterruptedException e)
                {
                    throw new RuntimeException(e);
                }
                finally
                {
                    // Make sure to always destroy the process if exceptions occur
                    process.destroy();
                }
                
                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {} // Do nothing.  We still need to flush the output.
                
                // Flush the output to make sure we have everything, probably not needed
                // but there are cases when it is.
                out.flush();
            }
        }
        
        return exitCode;
    }
    
    /**
     * Output the results to a summary file for a given test.  This
     * reads in the individual run results and then computes the summary
     * for the test.  This summary does not split among seperate decisions!
     * So if you need decisions to be seperated, you need to create a
     * seperate test, or modify the code to append per decisions.
     * 
     * @param test
     */
    private void appendToSummaryFile(Configuration.ConfigurationTest test)
    {
        TestSettings settings = test.getTestSettings();
        Table summaryTable = new Table();
        
        // Magic numbers for table
        for (int k = 0;k < 17;k++)
        {
            Row row = new Row();
            
            // Construct the table rows
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
        
        String csvDirectoryString = test.getTestSettings().getCSVDirectory();
        
        String testNameWithoutSpaces = test.getTestName().replaceAll("\\s+", "-");
        
        String testDirectoryString = csvDirectoryString + "/" + testNameWithoutSpaces;
        
        if (settings.isJSoarEnabled())
        {
            // JSoar
            List<Double> cpuTimes = new ArrayList<Double>();
            List<Double> kernelTimes = new ArrayList<Double>();
            List<Double> decisionCycles = new ArrayList<Double>();
            List<Double> memoryLoads = new ArrayList<Double>();
            
            String categoryDirectoryString = testDirectoryString + "/JSoar";
                                    
            for (int i = 1;i <= settings.getRunCount();i++)
            {
                for (Integer j : settings.getDecisionCycles())
                {
                    String finalTestName = testNameWithoutSpaces;
                    
                    if (j != 0)
                    {
                        finalTestName += "-" + j;
                    }
                    else
                    {
                        finalTestName += "-Forever";
                    }
                    
                    finalTestName += "-" + i;
                    
                    File testFile = new File(categoryDirectoryString + "/" + finalTestName + ".txt");

                    try
                    {
                        BufferedReader br = new BufferedReader(new FileReader(testFile));
                        String line;
                        // This will skip the first and last fields and only use the total fields
                        // for getting values since these are individual runs.

                        // CPU Time, Kernel Time, Decisions
                        for (int k = 0;k <= 10;k++)
                        {
                            line = br.readLine();

                            assert(line != null);

                            String[] list = line.split("\t");

                            switch (k)
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
                        out.println("Failed to load " + test.getTestName() + " results (" + categoryDirectoryString + "/" + finalTestName + ".txt" + ").  Skipping summary.");
                        out.flush();
                        continue;
                    }
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
                String csoarLabel = "CSoar-" + csoarPath.replaceAll("[^a-zA-Z0-9]+", "");
                
                String categoryDirectoryString = testDirectoryString + "/" + csoarLabel;
                
                // CSoar
                List<Double> cpuTimes = new ArrayList<Double>();
                List<Double> kernelTimes = new ArrayList<Double>();
                List<Double> decisionCycles = new ArrayList<Double>();
                List<Double> memoryLoads = new ArrayList<Double>();

                for (int i = 1;i <= settings.getRunCount();i++)
                {
                    for (Integer j : settings.getDecisionCycles())
                    {
                        String finalTestName = testNameWithoutSpaces;
                        
                        if (j != 0)
                        {
                            finalTestName += "-" + j;
                        }
                        else
                        {
                            finalTestName += "-Forever";
                        }
                        
                        finalTestName += "-" + i;
                        
                        File testFile = new File(categoryDirectoryString + "/" + finalTestName + ".txt");

                        try
                        {
                            BufferedReader br = new BufferedReader(new FileReader(testFile));
                            String line;
                            // This will skip the first and last fields and only use the total fields
                            // for getting values since these are individual runs.

                            // CPU Time, Kernel Time, Decisions
                            for (int k = 0;k <= 10;k++)
                            {
                                line = br.readLine();

                                assert(line != null);

                                String[] list = line.split("\t");

                                switch (k)
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
                            out.println("Failed to load " + test.getTestName() + " results (" + categoryDirectoryString + "/" + finalTestName + ".txt" + ").  Skipping summary.");
                            out.flush();
                            continue;
                        }
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
    
    /**
     * Convert a double to a string but make sure to get good
     * precision but not over the top precision.
     * 
     * @param d
     * @return The double as a string
     */
    private String doubleToString(Double d)
    {
        // This says to optionally have more numbers
        // to the left of the decimal point, but at
        // least have a 0.  It also says to alyways
        // have at least three significant figures
        // but potentially up to nine sig-figs.
        DecimalFormat df = new DecimalFormat("#0.000######");
        
        // Format the decimal
        return df.format(d);
    }
    
    /**
     * This runs a test.  This assume we're already in the
     * child JVM or at least are only ever running one test.
     * 
     * @param testRunners All the tests
     * @param testCategories All the test categories
     * @return Whether running the tests was successful or not
     */
    private int runTest(TestRunner testRunner)
    {
        Test test = testRunner.getTest();
        TestSettings settings = test.getTestSettings();

        if (!singleTest)
        {
            out.println("Starting Test: " + test.getTestName());
            out.flush();
        }

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
        catch (RuntimeException e)
        {
            if (e.getMessage() != null)
            {
                String message = e.getMessage();
                
                if (message.equals("Could not load CSoar") && settings.getCSoarVersions().size() > 0)
                {
                    message += " - " + settings.getCSoarVersions().get(0);
                }
                
                out.println("Error: " + message);
            }
            else
            {
                out.println("Critical Failure.  Bailing out of test.");
            }
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
            String csvDirectoryString = test.getTestSettings().getCSVDirectory();
            
            String testNameWithoutSpaces = test.getTestName().replaceAll("\\s+", "-");
            
            String testDirectoryString = csvDirectoryString + "/" + testNameWithoutSpaces;
            
            String categoryDirectoryString = null;
            
            if (settings.isJSoarEnabled())
            {
                categoryDirectoryString = testDirectoryString + "/JSoar";
            }
            else
            {
                categoryDirectoryString = testDirectoryString + "/CSoar";
                
                if (csoarTestFactory.getLabel().length() != 0)
                {
                    categoryDirectoryString += "-";
                    categoryDirectoryString += csoarTestFactory.getLabel().replaceAll("[^a-zA-Z0-9]+", "");
                }
            }
            
            String finalPathDirectoryString = categoryDirectoryString;
            
            File finalPathDirectory = new File(finalPathDirectoryString);
            
            if (!finalPathDirectory.exists())
            {
                finalPathDirectory.mkdirs();
            }
            
            String finalTestName = testNameWithoutSpaces;
            
            if (test.getTestSettings().getDecisionCycles().get(0) != 0)
            {
                finalTestName += "-" + test.getTestSettings().getDecisionCycles().get(0);
            }
            else
            {
                finalTestName += "-Forever";
            }
            
            if (runNumber != -1)
            {
                finalTestName += "-" + runNumber;
            }

            table.writeToCSV(finalPathDirectoryString + "/" + finalTestName + ".txt");

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

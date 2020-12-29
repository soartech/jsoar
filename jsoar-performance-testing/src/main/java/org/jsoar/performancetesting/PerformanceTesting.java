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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.jsoar.kernel.SoarException;
import org.jsoar.performancetesting.csoar.CSoarTestFactory;
import org.jsoar.performancetesting.jsoar.JSoarTestFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.github.classgraph.ClassGraph;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

/**
 * @author ALT
 * 
 */
@Command(name="performance-testing", description="Performance Testing - Performance testing framework for JSoar and CSoar", subcommands={HelpCommand.class})
public class PerformanceTesting implements Callable<Integer>
{
    // Static + Constants

    private static final int NON_EXIT = 1024;

    private static final int EXIT_SUCCESS = 0;

    private static final int EXIT_FAILURE = 255;

    private static final int EXIT_FAILURE_TEST = 254;

    private static final int EXIT_FAILURE_CONFIGURATION = 253;

    private static TestSettings defaultTestSettings = new TestSettings(false,
            false, 0, 0, new ArrayList<Integer>(), false, 1, Paths.get("."), null,
            null, null, "");

    @Spec CommandSpec spec; // injected by picocli
    
    // command line options
    
    @Option(names = {"-C", "--configuration"}, description = "Load a configuration file to use for testing. Ignore the rest of the options.")
    Path configuration;
    
    @Option(names = {"-T", "--test"}, description = "Manually specify a .soar file to run tests on. Useful for one-off tests where you don't want to create a configuration file. Also see -n")
    Path testPath = null;
    
    @Option(names = {"-o", "--output"}, description = "The directory for all the CSV test results.")
    Path output;
    
    @Option(names = {"-w", "--warmup"}, defaultValue = "0", description = "Specify the number of warm up runs for JSoar.")
    int warmup;
    
    @Option(names = {"-c", "--category"}, description = "Specify the number of warm up runs for JSoar.")
    String category;
    
    @Option(names = {"-j", "--jsoar"}, arity = "0..1", fallbackValue="Internal", description = "Run the tests in JSoar optionally specifying the directory with the jsoar version to use. If not specified will default to the internal jsoar version.")
    Path jsoar = null;
    
    @Option(names = {"-s", "--soar"}, description = "Run the tests in CSoar specifying the directory of CSoar's bin. When running with CSoar, CSoar's bin directory must be on the system path or in java.library.path or specified in a configuration file.")
    Path csoar = null;
    
    @Option(names = {"-u", "--uniqueJVMs"}, defaultValue="false", description = "Whether to run the tests in seperate jvms or not.")
    boolean uniqueJVMs;
    
    @Option(names = {"-d", "--decisions"}, description = "Run the tests specified number of decisions.")
    Integer decisions = null;
    
    @Option(names = {"-r", "--run"}, description = "How many test runs to perform.")
    private Integer runNumber = null;
    
    @Option(names = {"-n", "--name"}, description = "Used in conjunction with -T; specifies the test's name.")
    String name = "";
    
    @Option(names = {"-N", "--nosummary"}, defaultValue = "true", description = "Don't output results to a summary file.")
    boolean outputToSummaryFile;
    
    @Option(names = {"-S", "--single"}, hidden = true, description = "Used internally when spawning child JVMs")
    boolean singleTest = false;
    
    
    // Locals

    private final PrintWriter out;

    // Tests

    private CSoarTestFactory csoarTestFactory;

    private Test csoarTest;

    private JSoarTestFactory jsoarTestFactory;

    private Test jsoarTest;

    // Class Specific Configuration


    private List<ConfigurationTest> configurationTests;

    // Used for killing the current child process in the shutdown hook
    private Process currentChildProcess = null;

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        final PrintWriter writer = new PrintWriter(System.out);
        final int result = new CommandLine(new PerformanceTesting(writer)).execute(args);

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
     * @throws URISyntaxException 
     * @throws IOException 
     * @throws JsonMappingException 
     * @throws JsonParseException 
     */
    public Integer call() throws URISyntaxException, JsonParseException, JsonMappingException, IOException
    {
        // This adds a shutdown hook to the runtime
        // to kill any child processes spawned that
        // are still running. This handles CTRL-C
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
        int optionsParseResult = parseOptions();

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
            
            throw new ParameterException(this.spec.commandLine(), "Did not load any tests or configuration.");
        }

        // Only output starting information like this if we're not in a child
        // process
        if (!singleTest)
        {
            out.printf(
                    "Performance Testing - Starting Tests - %d Tests Loaded\n\n",
                    configurationTests.size());
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
     * Parses the CLI and Configuration Options
     * 
     * @param args
     * @return whether the parsing was successful or not
     * @throws IOException 
     * @throws JsonMappingException 
     * @throws JsonParseException 
     */
    private int parseOptions() throws JsonParseException, JsonMappingException, IOException
    {
        // If there is a configuration option,
        // ignore any CLI options beyond that.
        if (this.configuration != null)
        {
            parseConfiguration(configuration);

            return NON_EXIT;
        }

        // Since we don't have a configuration
        // option, parse the CLI arguments.
        return parseCLIOptions();
    }

    /**
     * Parse a configuration file from a given options processor. This will do
     * any validity checks on the configuration file as well.
     * 
     * @param options
     * @return whether the parsing was successful or not.
     * @throws IOException 
     * @throws JsonMappingException 
     * @throws JsonParseException 
     */
    private int parseConfiguration(Path configuration) throws JsonParseException, JsonMappingException, IOException
    {
        if (!configuration.toString().endsWith(".yaml"))
        {
            out.println("Configuration files need to be yaml files; got: " + configuration);
            return EXIT_FAILURE_CONFIGURATION;
        }

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Configuration config = mapper.readValue(configuration.toFile(), Configuration.class);

        defaultTestSettings = config.getDefaultSettings();

        if (!defaultTestSettings.isJsoarEnabled()
                && !defaultTestSettings.isCsoarEnabled())
        {
            out.println("WARNING: Neither jsoar nor csoar selected to run in defaults.");
        }

        configurationTests = config.getTests();

        return NON_EXIT;
    }

    /**
     * Parse the CLI arguments
     * 
     * @param options
     * @return whether the parsing was successful or not
     */
    private int parseCLIOptions()
    {
        if (output != null)
        {
            defaultTestSettings.setCsvDirectory(output);
        }

        defaultTestSettings.setWarmUpCount(this.warmup);

        if (this.jsoar != null)
        {
            defaultTestSettings.setJsoarEnabled(true);

            if(this.jsoar.toString().equals("Internal")) {
                jsoarTestFactory.setLabel("Internal");
            } else {
                List<Path> tempArray = new ArrayList<>();
                tempArray.add(this.jsoar);
                defaultTestSettings.setJsoarDirectories(tempArray);

                jsoarTestFactory.setLabel(this.jsoar.toString());
                jsoarTestFactory.setJSoarDirectory(this.jsoar);
            }
        }

        if (this.csoar != null)
        {
            defaultTestSettings.setCsoarEnabled(true);
            List<Path> tempArray = new ArrayList<>();
            tempArray.add(this.csoar);
            defaultTestSettings.setCsoarDirectories(tempArray);

            Path path = this.csoar;

            csoarTestFactory.setLabel(path.toString());
            csoarTestFactory.setCSoarDirectory(path);
        }

        if (this.decisions != null)
        {
            List<Integer> decisions = new ArrayList<>();

            decisions.add(this.decisions);

            defaultTestSettings.setDecisionCycles(decisions);
        }

        if (this.runNumber != null)
        {
            defaultTestSettings.setRunCount(1);
        }

        // This will load an individual test into the uncategorized tests
        // category, only really useful
        // for single tests that you don't want to create a configuration file
        // for
        if (this.testPath != null)
        {

            if (!testPath.toString().endsWith(".soar"))
            {
                out.println("Tests need to end with .soar");
                return EXIT_FAILURE_TEST;
            }

            String testName = testPath.getFileName().toString();

            if (this.name.length() != 0)
            {
                testName = this.name;
            }

            if (defaultTestSettings.isJsoarEnabled())
            {
                jsoarTest = jsoarTestFactory.createTest(testName, testPath,
                        defaultTestSettings);
            }

            if (defaultTestSettings.isCsoarEnabled())
            {
                csoarTest = csoarTestFactory.createTest(testName, testPath,
                        defaultTestSettings);
            }
        }

        return NON_EXIT;
    }

    /**
     * 
     * @param tests
     *            All the tests
     * @param testCategories
     *            The categories of all the tests
     * @return Whether running the tests in child JVMs was successful
     * @throws URISyntaxException 
     */
    private int runTestsInChildrenJVMs(
            List<ConfigurationTest> tests) throws URISyntaxException
    {
        Set<Path> previousSummaryFiles = new HashSet<>();

        // Since we have more than one test to run, spawn a separate JVM for
        // each run.
        int i = 0;
        for (ConfigurationTest test : tests)
        {
            Path dir = test.getTestSettings().getCsvDirectory();

            if (!Files.exists(dir))
            {
                try
                {
                    Files.createDirectories(dir);
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }

            Path summaryFilePath = test.getTestSettings().getCsvDirectory().resolve(test.getTestSettings().getSummaryFile());

            if (!previousSummaryFiles.contains(summaryFilePath))
            {
                PrintWriter summaryFileAppender;
                try
                {
                    summaryFileAppender = new PrintWriter(new BufferedWriter(
                            new FileWriter(summaryFilePath.toFile(), false)));
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }

                DateFormat dateFormat = new SimpleDateFormat(
                        "yyyy/MM/dd - HH:mm:ss");

                summaryFileAppender.println("Test Results - "
                        + dateFormat.format(new Date()) + "\n");

                summaryFileAppender.close();

                previousSummaryFiles.add(summaryFilePath);
            }

            out.println("--------------------------------------------------");
            out.println("Starting " + test.getName() + " Test (" + (++i)
                    + "/" + tests.size() + ")");
            out.println("--------------------------------------------------");

            if (test.getTestSettings().isJsoarEnabled())
            {
                spawnChildJVMForTest(test, true);
            }

            if (test.getTestSettings().isCsoarEnabled())
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
     * @param test
     *            The test to run
     * @param jsoar
     *            Whether this is a JSoar or CSoar test
     * @return Whether the run was successful or not
     * @throws URISyntaxException 
     */
    private int spawnChildJVMForTest(ConfigurationTest test, boolean jsoar) throws URISyntaxException
    {
        // Arguments to the process builder including the command to run
        List<String> arguments = new ArrayList<String>();

        URL baseURL = PerformanceTesting.class.getProtectionDomain()
                .getCodeSource().getLocation();
        //Path jarPath = null;
        // Get the directory for the jar file or class path
        String classpathString = baseURL.toString();

        
        if (!classpathString.endsWith(".jar"))
        {
            List<URI> classpath = new ClassGraph().getClasspathURIs();
            classpathString = classpath.stream()
                    .map(Paths::get)
                    .map(Path::toString)
                    .collect(Collectors.joining(String.valueOf(File.pathSeparatorChar)));
        }
        
        // Construct the array with the command and arguments
        // Use javaw for no console window spawning
        arguments.add("java");
        arguments.addAll(Arrays.asList(test.getTestSettings().getJvmSettings()
                .split("\\s")));
        arguments.add("-classpath"); // Always use class path. This will even
                                     // work for jars.
        arguments.add(classpathString);
        arguments.add(PerformanceTesting.class.getCanonicalName()); // Get the
                                                                    // class
                                                                    // name to
                                                                    // load
        arguments.add("--test");
        arguments.add(test.getFile().toString());
        arguments.add("--output");
        arguments.add(test.getTestSettings().getCsvDirectory().toString());
        arguments.add("--warmup");
        arguments.add(String.valueOf(test.getTestSettings().getWarmUpCount()));
        arguments.add("--name");
        arguments.add(test.getName());
        arguments.add("--nosummary");
        arguments.add("--single");

        // Run the process and get the exit code
        int exitCode = 0;

        if (jsoar)
        {
            arguments.add("--jsoar");

            // For each version of JSoar, run a child JVM
            if (test.getTestSettings().getJsoarDirectories() == null)
            {
                // Default to internal representation
                exitCode = runJVM(test, arguments);
            }
            else
            {
                List<Path> paths = test.getTestSettings().getJsoarDirectories();
                for (Path path : paths)
                {
                    List<String> argumentsPerTest = new ArrayList<>(arguments);
                    argumentsPerTest.add(path.toString());

                    exitCode = runJVM(test, argumentsPerTest);

                    if (exitCode != 0)
                    {
                        return EXIT_FAILURE_TEST;
                    }
                }
            }
        }
        else
        {
            arguments.add("--soar");

            // For each version of CSoar, run a child JVM
            if (test.getTestSettings().getCsoarDirectories() == null)
            {
                throw new RuntimeException(
                        "CSoar Enabled but no versions specified");
            }
            for (Path path : test.getTestSettings().getCsoarDirectories())
            {
                List<String> argumentsPerTest = new ArrayList<>(arguments);
                argumentsPerTest.add(path.toString());

                exitCode = runJVM(test, argumentsPerTest);

                if (exitCode != 0)
                {
                    return EXIT_FAILURE_TEST;
                }
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
    private int runJVM(ConfigurationTest test,
            List<String> arguments)
    {
        int exitCode = 0;

        // Run the test in a new child JVM for each run
        for (int i = 1; i <= test.getTestSettings().getRunCount(); ++i)
        {
            List<String> argumentsPerRun = new ArrayList<String>(arguments);

            int soarLocation = argumentsPerRun.size() - 1;

            List<String> soarArguments = new ArrayList<String>();

            soarArguments.add(argumentsPerRun.get(soarLocation));
            argumentsPerRun.remove(soarLocation);

            if (soarArguments.get(0).contains("--") != true
                    || soarArguments.get(0).contains("soar") != true)
            {
                --soarLocation;
                soarArguments.add(0, argumentsPerRun.get(soarLocation));
                argumentsPerRun.remove(soarLocation);
            }

            argumentsPerRun.add("--run");
            argumentsPerRun.add(String.valueOf(i));

            out.println("Starting Test - " + test.getName() + " - " + i
                    + "/" + test.getTestSettings().getRunCount());

            // For each decision cycle count in the list, run a new child JVM
            for (Integer j : test.getTestSettings().getDecisionCycles())
            {
                List<String> argumentsPerCycle = new ArrayList<String>(
                        argumentsPerRun);
                argumentsPerCycle.add("--decisions");
                argumentsPerCycle.add(j.toString());

                argumentsPerCycle.addAll(soarArguments);

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
                ProcessBuilder processBuilder = new ProcessBuilder(
                        argumentsPerCycle);

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
                    // won't redirect to the console output. Instead we will
                    // redirect to the original java System.out which isn't the
                    // screen. This means that the output both, won't show up
                    // and we could potentially crash the JVM if we output
                    // enough.
                    // - ALT
                    StreamGobbler outputGobbler = new StreamGobbler(
                            process.getInputStream(), out);
                    StreamGobbler errorGobbler = new StreamGobbler(
                            process.getErrorStream(), out);

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
                    // Make sure to always destroy the process if exceptions
                    // occur
                    process.destroy();
                }

                try
                {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e)
                {
                } // Do nothing. We still need to flush the output.

                // Flush the output to make sure we have everything, probably
                // not needed
                // but there are cases when it is.
                out.flush();
            }
        }

        return exitCode;
    }

    /**
     * Output the results to a summary file for a given test. This reads in the
     * individual run results and then computes the summary for the test. This
     * summary does not split among separate decisions! So if you need decisions
     * to be separated, you need to create a separate test, or modify the code
     * to append per decisions.
     * 
     * @param test
     */
    private void appendToSummaryFile(ConfigurationTest test)
    {
        TestSettings settings = test.getTestSettings();
        Table summaryTable = new Table();

        // Magic numbers for table
        for (int k = 0; k < 17; k++)
        {
            Row row = new Row();

            // Construct the table rows
            switch (k)
            {
            case 0:
                row.add(new Cell(test.getName()));

                if (test.getTestSettings().isJsoarEnabled())
                {
                    if (test.getTestSettings().getJsoarDirectories() != null)
                    {
                        for (Path jsoarPath : test.getTestSettings().getJsoarDirectories())
                        {
                            row.add(new Cell("JSoar " + jsoarPath));
                        }
                    }
                    else
                        row.add(new Cell("JSoar Internal"));
                }

                if (test.getTestSettings().isCsoarEnabled())
                {
                    for (Path csoarPath : test.getTestSettings().getCsoarDirectories())
                    {
                        row.add(new Cell("CSoar " + csoarPath));
                    }
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

        Path csvDirectoryString = test.getTestSettings().getCsvDirectory();

        String testNameWithoutSpaces = test.getName().replaceAll("\\s+",
                "-");

        String testDirectoryString = csvDirectoryString + "/" + testNameWithoutSpaces;

        int column = 2 - 1;

        if (settings.isJsoarEnabled())
        {
            List<Path> jsoarVersions = settings.getJsoarDirectories();

            if (jsoarVersions == null)
            {
                jsoarVersions = new ArrayList<>();
                jsoarVersions.add(Paths.get("Internal"));
            }

            for (Path jsoarPath : jsoarVersions)
            {
                String jsoarLabel = "JSoar-" + jsoarPath.toString().replaceAll("[^a-zA-Z0-9]+", "");

                String categoryDirectoryString = testDirectoryString + "/" + jsoarLabel;

                // JSoar
                List<Double> cpuTimes = new ArrayList<Double>();
                List<Double> kernelTimes = new ArrayList<Double>();
                List<Double> decisionCycles = new ArrayList<Double>();
                List<Double> memoryLoads = new ArrayList<Double>();

                for (int i = 1; i <= settings.getRunCount(); i++)
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

                        File testFile = new File(categoryDirectoryString + "/"
                                + finalTestName + ".txt");

                        try
                        {
                            BufferedReader br = new BufferedReader(
                                    new FileReader(testFile));
                            String line;
                            // This will skip the first and last fields and only
                            // use
                            // the total fields
                            // for getting values since these are individual
                            // runs.

                            // CPU Time, Kernel Time, Decisions
                            for (int k = 0; k <= 10; k++)
                            {
                                line = br.readLine();

                                assert (line != null);

                                String[] list = line.split("\t");

                                switch (k)
                                {
                                case 1:
                                    // CPU Time
                                    cpuTimes.add(Double.parseDouble(list[1]));
                                    break;
                                case 4:
                                    kernelTimes
                                            .add(Double.parseDouble(list[1]));
                                    break;
                                case 7:
                                    decisionCycles.add(Double
                                            .parseDouble(list[1]));
                                    break;
                                case 10:
                                    memoryLoads
                                            .add(Double.parseDouble(list[1]));
                                    break;
                                }
                            }
                            br.close();
                        }
                        catch (IOException e)
                        {
                            out.println("Failed to load " + test.getName()
                                    + " results (" + categoryDirectoryString
                                    + "/" + finalTestName + ".txt"
                                    + ").  Skipping summary.");
                            out.flush();
                            continue;
                        }
                    }
                }

                // Now calculate everything
                summaryTable.setOrAddValueAtLocation(
                        doubleToString(Statistics.calculateTotal(cpuTimes)), 1,
                        column);
                summaryTable.setOrAddValueAtLocation(
                        doubleToString(Statistics.calculateAverage(cpuTimes)),
                        2, column);
                summaryTable.setOrAddValueAtLocation(
                        doubleToString(Statistics.calculateMedian(cpuTimes)),
                        3, column);
                summaryTable
                        .setOrAddValueAtLocation(doubleToString(Statistics
                                .calculateDeviation(cpuTimes)), 4, column);

                summaryTable.setOrAddValueAtLocation(
                        doubleToString(Statistics.calculateTotal(kernelTimes)),
                        5, column);
                summaryTable
                        .setOrAddValueAtLocation(doubleToString(Statistics
                                .calculateAverage(kernelTimes)), 6, column);
                summaryTable
                        .setOrAddValueAtLocation(doubleToString(Statistics
                                .calculateMedian(kernelTimes)), 7, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics
                        .calculateDeviation(kernelTimes)), 8, column);

                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics
                        .calculateTotal(decisionCycles)), 9, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics
                        .calculateAverage(decisionCycles)), 10, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics
                        .calculateMedian(decisionCycles)), 11, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics
                        .calculateDeviation(decisionCycles)), 12, column);

                summaryTable.setOrAddValueAtLocation(
                        doubleToString(Statistics.calculateTotal(memoryLoads)),
                        13, column);
                summaryTable
                        .setOrAddValueAtLocation(doubleToString(Statistics
                                .calculateAverage(memoryLoads)), 14, column);
                summaryTable
                        .setOrAddValueAtLocation(doubleToString(Statistics
                                .calculateMedian(memoryLoads)), 15, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics
                        .calculateDeviation(memoryLoads)), 16, column);

                ++column;
            }
        }

        if (settings.isCsoarEnabled())
        {
            for (Path csoarPath : test.getTestSettings().getCsoarDirectories())
            {
                String csoarLabel = "CSoar-"
                        + csoarPath.toString().replaceAll("[^a-zA-Z0-9]+", "");

                String categoryDirectoryString = testDirectoryString + "/"
                        + csoarLabel;

                // CSoar
                List<Double> cpuTimes = new ArrayList<Double>();
                List<Double> kernelTimes = new ArrayList<Double>();
                List<Double> decisionCycles = new ArrayList<Double>();
                List<Double> memoryLoads = new ArrayList<Double>();

                for (int i = 1; i <= settings.getRunCount(); i++)
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

                        File testFile = new File(categoryDirectoryString + "/"
                                + finalTestName + ".txt");

                        try
                        {
                            BufferedReader br = new BufferedReader(
                                    new FileReader(testFile));
                            String line;
                            // This will skip the first and last fields and only
                            // use the total fields
                            // for getting values since these are individual
                            // runs.

                            // CPU Time, Kernel Time, Decisions
                            for (int k = 0; k <= 10; k++)
                            {
                                line = br.readLine();

                                assert (line != null);

                                String[] list = line.split("\t");

                                switch (k)
                                {
                                case 1:
                                    // CPU Time
                                    cpuTimes.add(Double.parseDouble(list[2]));
                                    break;
                                case 4:
                                    kernelTimes
                                            .add(Double.parseDouble(list[2]));
                                    break;
                                case 7:
                                    decisionCycles.add(Double
                                            .parseDouble(list[2]));
                                    break;
                                case 10:
                                    memoryLoads
                                            .add(Double.parseDouble(list[2]));
                                    break;
                                }
                            }
                            br.close();
                        }
                        catch (IOException e)
                        {
                            out.println("Failed to load " + test.getName()
                                    + " results (" + categoryDirectoryString
                                    + "/" + finalTestName + ".txt"
                                    + ").  Skipping summary.");
                            out.flush();
                            continue;
                        }
                    }
                }

                // Now calculate everything
                summaryTable.setOrAddValueAtLocation(
                        doubleToString(Statistics.calculateTotal(cpuTimes)), 1,
                        column);
                summaryTable.setOrAddValueAtLocation(
                        doubleToString(Statistics.calculateAverage(cpuTimes)),
                        2, column);
                summaryTable.setOrAddValueAtLocation(
                        doubleToString(Statistics.calculateMedian(cpuTimes)),
                        3, column);
                summaryTable
                        .setOrAddValueAtLocation(doubleToString(Statistics
                                .calculateDeviation(cpuTimes)), 4, column);

                summaryTable.setOrAddValueAtLocation(
                        doubleToString(Statistics.calculateTotal(kernelTimes)),
                        5, column);
                summaryTable
                        .setOrAddValueAtLocation(doubleToString(Statistics
                                .calculateAverage(kernelTimes)), 6, column);
                summaryTable
                        .setOrAddValueAtLocation(doubleToString(Statistics
                                .calculateMedian(kernelTimes)), 7, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics
                        .calculateDeviation(kernelTimes)), 8, column);

                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics
                        .calculateTotal(decisionCycles)), 9, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics
                        .calculateAverage(decisionCycles)), 10, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics
                        .calculateMedian(decisionCycles)), 11, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics
                        .calculateDeviation(decisionCycles)), 12, column);

                summaryTable.setOrAddValueAtLocation(
                        doubleToString(Statistics.calculateTotal(memoryLoads)),
                        13, column);
                summaryTable
                        .setOrAddValueAtLocation(doubleToString(Statistics
                                .calculateAverage(memoryLoads)), 14, column);
                summaryTable
                        .setOrAddValueAtLocation(doubleToString(Statistics
                                .calculateMedian(memoryLoads)), 15, column);
                summaryTable.setOrAddValueAtLocation(doubleToString(Statistics
                        .calculateDeviation(memoryLoads)), 16, column);

                ++column;
            }
        }

        String summaryFilePath = settings.getCsvDirectory() + "/"
                + settings.getSummaryFile();

        summaryTable.writeToCSV(summaryFilePath, '\t', true);
    }

    /**
     * Convert a double to a string but make sure to get good precision but not
     * over the top precision.
     * 
     * @param d
     * @return The double as a string
     */
    private String doubleToString(Double d)
    {
        // This says to optionally have more numbers
        // to the left of the decimal point, but at
        // least have a 0. It also says to alyways
        // have at least three significant figures
        // but potentially up to nine sig-figs.
        DecimalFormat df = new DecimalFormat("#0.000######");

        // Format the decimal
        return df.format(d);
    }

    /**
     * This runs a test. This assume we're already in the child JVM or at least
     * are only ever running one test.
     * 
     * @param testRunners
     *            All the tests
     * @param testCategories
     *            All the test categories
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

        // Construct a table for the data

        Table table = new Table();

        for (int k = 0; k < 14; k++)
        {
            Row row = new Row();

            switch (k)
            {
            case 0:
                row.add(new Cell(test.getTestName()));

                if (settings.isJsoarEnabled())
                {
                    row.add(new Cell("JSoar " + jsoarTestFactory.getLabel()));
                }

                if (settings.isCsoarEnabled())
                {
                    row.add(new Cell("CSoar " + csoarTestFactory.getLabel()));
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

        int column = 2 - 1;

        // Run JSoar
        if (settings.isJsoarEnabled())
        {
            out.println("JSoar: ");
            out.flush();

            column = 2 - 1;
        }
        else if (settings.isCsoarEnabled()) // Or CSoar
        {
            out.println("CSoar " + csoarTestFactory.getLabel() + ": ");
            out.flush();

            column = 3 - 1;
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

                if (message.equals("Could not load CSoar")
                        && settings.getCsoarDirectories().size() > 0)
                {
                    message += " - " + settings.getCsoarDirectories().get(0);
                }

                out.println("Error: " + message);
            }
            else
            {
                out.println("Critical Failure.  Bailing out of test.");
            }
            return EXIT_FAILURE;
        }

        table.setOrAddValueAtLocation(
                String.valueOf(testRunner.getTotalCPUTime()), 2 - 1, column);
        table.setOrAddValueAtLocation(
                String.valueOf(testRunner.getAverageCPUTime()), 3 - 1, column);
        table.setOrAddValueAtLocation(
                String.valueOf(testRunner.getMedianCPUTime()).toString(), 4 - 1, column);
        table.setOrAddValueAtLocation(
                String.valueOf(testRunner.getTotalKernelTime()).toString(), 5 - 1, column);
        table.setOrAddValueAtLocation(
                String.valueOf(testRunner.getAverageKernelTime()).toString(), 6 - 1, column);
        table.setOrAddValueAtLocation(
                String.valueOf(testRunner.getMedianKernelTime()).toString(), 7 - 1, column);
        table.setOrAddValueAtLocation(
                String.valueOf(testRunner.getTotalDecisionCycles()).toString(), 8 - 1, column);
        table.setOrAddValueAtLocation(
                String.valueOf(testRunner.getAverageDecisionCycles()).toString(), 9 - 1, column);
        table.setOrAddValueAtLocation(
                String.valueOf(testRunner.getMedianDecisionCycles()).toString(), 10 - 1, column);
        table.setOrAddValueAtLocation(
                String.valueOf(testRunner.getTotalMemoryLoad() / 1000.0 / 1000.0), 11 - 1, column);
        table.setOrAddValueAtLocation(
                String.valueOf(testRunner.getAverageMemoryLoad() / 1000.0 / 1000.0), 12 - 1, column);
        table.setOrAddValueAtLocation(
                String.valueOf(testRunner.getMedianMemoryLoad() / 1000.0 / 1000.0), 13 - 1, column);
        table.setOrAddValueAtLocation(
                String.valueOf(testRunner.getMemoryLoadDeviation() / 1000.0 / 1000.0), 14 - 1, column);

        if (settings.getCsvDirectory().getNameCount() != 0)
        {
            String csvDirectoryString = test.getTestSettings()
                    .getCsvDirectory().toString();

            String testNameWithoutSpaces = test.getTestName().replaceAll(
                    "\\s+", "-");

            String testDirectoryString = csvDirectoryString + "/"
                    + testNameWithoutSpaces;

            String categoryDirectoryString = null;

            if (settings.isJsoarEnabled())
            {
                categoryDirectoryString = testDirectoryString + "/JSoar";

                if (jsoarTestFactory.getLabel().length() != 0)
                {
                    categoryDirectoryString += "-";
                    categoryDirectoryString += jsoarTestFactory.getLabel()
                            .replaceAll("[^a-zA-Z0-9]+", "");
                }
            }
            else
            {
                categoryDirectoryString = testDirectoryString + "/CSoar";

                if (csoarTestFactory.getLabel().length() != 0)
                {
                    categoryDirectoryString += "-";
                    categoryDirectoryString += csoarTestFactory.getLabel()
                            .replaceAll("[^a-zA-Z0-9]+", "");
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
                finalTestName += "-"
                        + test.getTestSettings().getDecisionCycles().get(0);
            }
            else
            {
                finalTestName += "-Forever";
            }

            if (runNumber != null)
            {
                finalTestName += "-" + runNumber;
            }

            table.writeToCSV(finalPathDirectoryString + "/" + finalTestName
                    + ".txt");

            if (outputToSummaryFile)
            {
                table.writeToCSV(
                        settings.getCsvDirectory() + "/"
                                + settings.getSummaryFile(), true);
            }
        }

        out.print("\n");

        out.flush();

        return NON_EXIT;
    }

}

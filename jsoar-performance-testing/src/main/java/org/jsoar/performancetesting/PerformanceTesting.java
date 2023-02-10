/**
 * 
 */
package org.jsoar.performancetesting;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jsoar.kernel.SoarException;
import org.jsoar.performancetesting.csoar.CSoarTestFactory;
import org.jsoar.performancetesting.jsoar.JSoarTestFactory;
import org.jsoar.performancetesting.yaml.Configuration;
import org.jsoar.performancetesting.yaml.ConfigurationTest;
import org.jsoar.performancetesting.yaml.TestSettings;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

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
@Command(name = "performance-testing", description = "Performance Testing - Performance testing framework for JSoar and CSoar", subcommands = { HelpCommand.class })
public class PerformanceTesting implements Runnable
{
    private static TestSettings defaultTestSettings = new TestSettings(false,
            false, 0, 0, new ArrayList<Integer>(), false, 1, Paths.get("."), null,
            null, null, "");
    
    @Spec
    CommandSpec spec; // injected by picocli
    
    // command line options
    
    @Option(names = { "-C", "--configuration" }, description = "Load a configuration file to use for testing. Ignore the rest of the options.")
    Path configuration;
    
    @Option(names = { "-T", "--test" }, description = "Manually specify a .soar file to run tests on. Useful for one-off tests where you don't want to create a configuration file. Also see -n")
    Path testPath = null;
    
    @Option(names = { "-o", "--output" }, description = "The directory for all the CSV test results.")
    Path output;
    
    @Option(names = { "-w", "--warmup" }, defaultValue = "0", description = "Specify the number of warm up runs for JSoar.")
    int warmup;
    
    @Option(names = { "-c", "--category" }, description = "Specify the number of warm up runs for JSoar.")
    String category;
    
    @Option(names = { "-j",
            "--jsoar" }, arity = "0..1", fallbackValue = "Internal", description = "Run the tests in JSoar optionally specifying the jsoar jar. If not specified will default to the internal jsoar version.")
    Path jsoar = null;
    
    @Option(names = { "-s",
            "--soar" }, description = "Run the tests in CSoar specifying the directory of CSoar's bin. When running with CSoar, CSoar's bin directory must be on the system path or in java.library.path or specified in a configuration file.")
    Path csoar = null;
    
    @Option(names = { "-u", "--uniqueJVMs" }, defaultValue = "false", description = "Whether to run the tests in seperate jvms or not.")
    boolean uniqueJVMs;
    
    @Option(names = { "-d", "--decisions" }, description = "Run the tests specified number of decisions.")
    Integer decisions = null;
    
    @Option(names = { "-r", "--run" }, description = "How many test runs to perform.")
    private Integer runNumber = null;
    
    @Option(names = { "-n", "--name" }, description = "Used in conjunction with -T; specifies the test's name.")
    String name = "";
    
    @Option(names = { "-N", "--nosummary" }, defaultValue = "false", description = "Don't output results to a summary file.")
    boolean nosummary;
    
    @Option(names = { "-S", "--single" }, hidden = true, description = "Used internally when spawning child JVMs")
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
        
        System.exit(result);
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
    
    public void run()
    {
        try
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
                    if(currentChildProcess != null)
                    {
                        currentChildProcess.destroy();
                    }
                }
            });
            
            // Parse the CLI options and Configuration options
            // if there are any
            parseOptions();
            
            // If this is not a single test and configurationTests is null
            // exit because the configuration file didn't work out
            // or something else bad happened.
            if(!singleTest && configurationTests == null)
            {
                out.println("Did not load any tests or configuration.");
                
                throw new ParameterException(this.spec.commandLine(), "Did not load any tests or configuration.");
            }
            
            // Only output starting information like this if we're not in a child
            // process
            if(!singleTest)
            {
                out.printf(
                        "Performance Testing - Starting Tests - %d Tests Loaded\n\n",
                        configurationTests.size());
                out.flush();
            }
            
            // If we have multiple tests to run, then run
            // them in children JVMs
            if(configurationTests != null)
            {
                runTestsInChildrenJVMs(configurationTests);
            }
            else
            {
                // In this case, we are running just one test which means
                // the test is either going to be jsoarTest or csoarTest.
                if(jsoarTest != null)
                {
                    runTest(new TestRunner(jsoarTest, out));
                }
                else
                {
                    runTest(new TestRunner(csoarTest, out));
                }
            }
        }
        catch(CsvDataTypeMismatchException | CsvRequiredFieldEmptyException | IllegalStateException | URISyntaxException | IOException | SoarException | ClassNotFoundException | IllegalAccessException
                | NoSuchFieldException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Parses the CLI and Configuration Options
     * 
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws Exception
     */
    private void parseOptions() throws JsonParseException, JsonMappingException, IOException, ClassNotFoundException, IllegalAccessException, NoSuchFieldException
    {
        // If there is a configuration option,
        // ignore any CLI options beyond that.
        if(this.configuration != null)
        {
            parseConfiguration(configuration);
        }
        
        // Since we don't have a configuration
        // option, parse the CLI arguments.
        parseCLIOptions();
    }
    
    /**
     * Parse a configuration file from a given options processor. This will do
     * any validity checks on the configuration file as well.
     * 
     * @param configuration Path to the configuration file
     * @throws IOException
     * @throws JsonMappingException
     * @throws JsonParseException
     */
    private void parseConfiguration(Path configuration) throws JsonParseException, JsonMappingException, IOException
    {
        if(!configuration.toString().endsWith(".yaml"))
        {
            throw new ParameterException(spec.commandLine(), "Configuration files need to be yaml files; got: " + configuration);
        }
        
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        Configuration config = mapper.readValue(configuration.toFile(), Configuration.class);
        
        defaultTestSettings = config.getDefaultSettings();
        
        if(!defaultTestSettings.isJsoarEnabled()
                && !defaultTestSettings.isCsoarEnabled())
        {
            out.println("WARNING: Neither jsoar nor csoar selected to run in defaults.");
        }
        
        configurationTests = config.getTests();
    }
    
    /**
     * Parse the CLI arguments
     * 
     * @throws ClassNotFoundException
     * @throws MalformedURLException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private void parseCLIOptions() throws MalformedURLException, ClassNotFoundException, IllegalAccessException, NoSuchFieldException
    {
        if(output != null)
        {
            defaultTestSettings.setCsvDirectory(output);
        }
        
        defaultTestSettings.setWarmUpCount(this.warmup);
        
        if(this.jsoar != null)
        {
            defaultTestSettings.setJsoarEnabled(true);
            
            if(this.jsoar.toString().equals("Internal"))
            {
                jsoarTestFactory.setJsoarCoreJar(Paths.get("Internal"));
            }
            else
            {
                List<Path> tempArray = new ArrayList<>();
                tempArray.add(this.jsoar);
                defaultTestSettings.setJsoarCoreJars(tempArray);
                
                jsoarTestFactory.setJsoarCoreJar(this.jsoar);
            }
        }
        
        if(this.csoar != null)
        {
            defaultTestSettings.setCsoarEnabled(true);
            List<Path> tempArray = new ArrayList<>();
            tempArray.add(this.csoar);
            defaultTestSettings.setCsoarDirectories(tempArray);
            
            Path path = this.csoar;
            
            csoarTestFactory.setCSoarDirectory(path);
        }
        
        if(this.decisions != null)
        {
            List<Integer> decisions = new ArrayList<>();
            
            decisions.add(this.decisions);
            
            defaultTestSettings.setDecisionCycles(decisions);
        }
        
        if(this.runNumber != null)
        {
            defaultTestSettings.setRunCount(1);
        }
        
        // This will load an individual test into the uncategorized tests
        // category, only really useful
        // for single tests that you don't want to create a configuration file
        // for
        if(this.testPath != null)
        {
            
            if(!testPath.toString().endsWith(".soar"))
            {
                throw new ParameterException(spec.commandLine(), "Tests need to end with .soar");
            }
            
            String testName = testPath.getFileName().toString();
            
            if(this.name.length() != 0)
            {
                testName = this.name;
            }
            
            if(defaultTestSettings.isJsoarEnabled())
            {
                jsoarTest = jsoarTestFactory.createTest(testName, testPath,
                        defaultTestSettings);
            }
            
            if(defaultTestSettings.isCsoarEnabled())
            {
                csoarTest = csoarTestFactory.createTest(testName, testPath,
                        defaultTestSettings);
            }
        }
        
    }
    
    /**
     * 
     * @param tests All the tests
     * @throws URISyntaxException
     * @throws IOException
     * @throws IllegalStateException
     * @throws CsvRequiredFieldEmptyException
     * @throws CsvDataTypeMismatchException
     * @throws Exception
     */
    private void runTestsInChildrenJVMs(
            List<ConfigurationTest> tests) throws URISyntaxException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException, IllegalStateException, IOException
    {
        // Since we have more than one test to run, spawn a separate JVM for
        // each run.
        int i = 0;
        for(ConfigurationTest test : tests)
        {
            Path dir = test.getTestSettings().getCsvDirectory();
            
            if(!Files.exists(dir))
            {
                try
                {
                    Files.createDirectories(dir);
                }
                catch(IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
            
            out.println("--------------------------------------------------");
            out.println("Starting " + test.getName() + " Test (" + (++i)
                    + "/" + tests.size() + ")");
            out.println("--------------------------------------------------");
            
            if(test.getTestSettings().isJsoarEnabled())
            {
                spawnChildJVMForTest(test, true);
            }
            
            if(test.getTestSettings().isCsoarEnabled())
            {
                spawnChildJVMForTest(test, false);
            }
            
            // Generate a summary file
            appendToSummaryFile(test);
        }
        
        out.println("Performance Testing - Done");
    }
    
    /**
     * Spawns a child JVM for the test and waits for it to exit.
     * 
     * @param test The test to run
     * @param jsoar Whether this is a JSoar or CSoar test
     * @throws URISyntaxException
     */
    private void spawnChildJVMForTest(ConfigurationTest test, boolean jsoar) throws URISyntaxException
    {
        // Arguments to the process builder including the command to run
        List<String> arguments = new ArrayList<String>();
        
        URL baseURL = PerformanceTesting.class.getProtectionDomain()
                .getCodeSource().getLocation();
        // Path jarPath = null;
        // Get the directory for the jar file or class path
        String classpathString = baseURL.toString();
        
        if(!classpathString.endsWith(".jar"))
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
        
        if(jsoar)
        {
            arguments.add("--jsoar");
            
            // For each version of JSoar, run a child JVM
            if(test.getTestSettings().getJsoarCoreJars() == null)
            {
                // Default to internal representation
                runJVM(test, arguments);
            }
            else
            {
                List<Path> paths = test.getTestSettings().getJsoarCoreJars();
                for(Path path : paths)
                {
                    List<String> argumentsPerTest = new ArrayList<>(arguments);
                    argumentsPerTest.add(path.toString());
                    
                    runJVM(test, argumentsPerTest);
                    
                }
            }
        }
        else
        {
            arguments.add("--soar");
            
            // For each version of CSoar, run a child JVM
            if(test.getTestSettings().getCsoarDirectories() == null)
            {
                throw new RuntimeException(
                        "CSoar Enabled but no versions specified");
            }
            for(Path path : test.getTestSettings().getCsoarDirectories())
            {
                List<String> argumentsPerTest = new ArrayList<>(arguments);
                argumentsPerTest.add(path.toString());
                
                runJVM(test, argumentsPerTest);
            }
        }
        
    }
    
    /**
     * This runs a test in a child JVM with the given arguments
     * 
     * @param test
     * @param arguments
     */
    private void runJVM(ConfigurationTest test,
            List<String> arguments)
    {
        // Run the test in a new child JVM for each run
        for(int i = 1; i <= test.getTestSettings().getRunCount(); ++i)
        {
            List<String> argumentsPerRun = new ArrayList<String>(arguments);
            
            int soarLocation = argumentsPerRun.size() - 1;
            
            List<String> soarArguments = new ArrayList<String>();
            
            soarArguments.add(argumentsPerRun.get(soarLocation));
            argumentsPerRun.remove(soarLocation);
            
            if(soarArguments.get(0).contains("--") != true
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
            for(Integer j : test.getTestSettings().getDecisionCycles())
            {
                List<String> argumentsPerCycle = new ArrayList<String>(
                        argumentsPerRun);
                argumentsPerCycle.add("--decisions");
                argumentsPerCycle.add(j.toString());
                
                argumentsPerCycle.addAll(soarArguments);
                
                String decisionsString;
                
                if(j == 0)
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
                    if(process.waitFor() != 0)
                    {
                        throw new RuntimeException("Child jvm exited with error");
                    }
                    // Make sure we don't try to kill a non-existent process
                    // if we are shutdown after this.
                    currentChildProcess = null;
                }
                catch(IOException e)
                {
                    throw new RuntimeException(e);
                }
                catch(InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                finally
                {
                    // Make sure to always destroy the process if exceptions
                    // occur
                    if(process != null)
                    {
                        process.destroy();
                    }
                }
                
                try
                {
                    Thread.sleep(1000);
                }
                catch(InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                finally
                {
                    // Flush the output to make sure we have everything
                    // probably not needed but there are cases when it is
                    out.flush();
                }
            }
        }
        
    }
    
    /**
     * Output the results to a summary file for a given test. This reads in the
     * individual run results and then computes the summary for the test. The
     * reading is necessary as these results were (probably) generated in another
     * jvm.
     * 
     * @throws IOException
     * @throws IllegalStateException
     * @throws CsvRequiredFieldEmptyException
     * @throws CsvDataTypeMismatchException
     */
    private void appendToSummaryFile(ConfigurationTest test) throws CsvDataTypeMismatchException, CsvRequiredFieldEmptyException, IllegalStateException, IOException
    {
        TestSettings settings = test.getTestSettings();
        
        if(settings.isJsoarEnabled())
        {
            List<Path> jsoarVersions = settings.getJsoarCoreJars();
            
            if(jsoarVersions == null)
            {
                jsoarVersions = new ArrayList<>();
                jsoarVersions.add(Paths.get("Internal"));
            }
            
            for(Path jsoarPath : jsoarVersions)
            {
                for(int dcs : settings.getDecisionCycles())
                {
                    appendToSummaryFileInternal(test, "JSoar", jsoarPath, dcs, "-raw");
                }
            }
        }
        
        if(settings.isCsoarEnabled())
        {
            for(Path csoarPath : test.getTestSettings().getCsoarDirectories())
            {
                for(Integer dcs : settings.getDecisionCycles())
                {
                    appendToSummaryFileInternal(test, "CSoar", csoarPath, dcs, "-raw");
                }
            }
        }
        
    }
    
    private Path getTestPath(TestSettings settings, String testName, String soarVariant, Path soarPath, int dcs, String fileSuffix)
    {
        
        String testNameWithoutSpaces = testName.replaceAll("\\s+", "-");
        
        Path testDirectory = settings.getCsvDirectory().resolve(testNameWithoutSpaces);
        
        String label = soarVariant + "-" + soarPath.toString().replaceAll("[^a-zA-Z0-9]+", "");
        
        Path categoryDirectory = testDirectory.resolve(label);
        
        String finalTestName = testNameWithoutSpaces;
        if(dcs != 0)
        {
            finalTestName += "-" + dcs;
        }
        else
        {
            finalTestName += "-Forever";
        }
        
        Path testPath = categoryDirectory.resolve(finalTestName + fileSuffix + ".csv");
        return testPath;
    }
    
    private void appendToSummaryFileInternal(ConfigurationTest test, String soarVariant, Path soarPath, int dcs, String fileSuffix)
            throws IllegalStateException, IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException
    {
        File testFile = getTestPath(test.getSettings(), test.getName(), soarVariant, soarPath, dcs, "-raw").toFile();
        
        List<RawResults> rawResults = new CsvToBeanBuilder<RawResults>(new FileReader(testFile))
                .withType(RawResults.class).withSkipLines(1).build().parse();
        
        RawResults combinedRawResults = rawResults.stream()
                .reduce(new RawResults(), (subtotal, element) -> subtotal.accumulate(element));
        
        Results summary = new Results(test.getName() + "-" + dcs, test.getFile(), soarVariant, soarPath);
        summary.updateStats(combinedRawResults);
        
        Path finalPath = test.getSettings().getCsvDirectory().resolve(test.getSettings().getSummaryFile());
        boolean newFile = !Files.exists(finalPath);
        try(Writer writer = new FileWriter(finalPath.toFile(), true))
        {
            if(newFile)
            {
                writer.write(String.join(",", Results.header) + "\n");
            }
            StatefulBeanToCsv<Results> beanToCsv = new StatefulBeanToCsvBuilder<Results>(writer).build();
            beanToCsv.write(summary);
        }
    }
    
    /**
     * This runs a test. This assume we're already in the child JVM or at least
     * are only ever running one test.
     * 
     * @param testRunner A test
     * @throws SoarException
     * @throws IOException
     * @throws CsvRequiredFieldEmptyException
     * @throws CsvDataTypeMismatchException
     * @throws Exception
     */
    private void runTest(TestRunner testRunner) throws SoarException, IOException, CsvDataTypeMismatchException, CsvRequiredFieldEmptyException
    {
        Test test = testRunner.getTest();
        TestSettings settings = test.getTestSettings();
        
        if(!singleTest)
        {
            out.println("Starting Test: " + test.getTestName());
            out.flush();
        }
        
        testRunner.runTestsForAverage(settings);
        
        if(settings.getCsvDirectory().getNameCount() != 0)
        {
            Path finalPath = getTestPath(test.getTestSettings(), test.getTestName(), test.getSoarVariant(), test.getSoarPath(), test.getTestSettings().getDecisionCycles().get(0), "-raw");
            Files.createDirectories(finalPath.getParent()); // this will create the dirs needed for the rest of the files below
            
            boolean newFile = !Files.exists(finalPath);
            
            try(Writer writer = new FileWriter(finalPath.toFile(), true))
            {
                if(newFile)
                {
                    writer.write(String.join(",", RawResults.header) + "\n");
                }
                StatefulBeanToCsv<RawResults> beanToCsv = new StatefulBeanToCsvBuilder<RawResults>(writer).build();
                beanToCsv.write(testRunner.getRawResults());
            }
            
            finalPath = getTestPath(test.getTestSettings(), test.getTestName(), test.getSoarVariant(), test.getSoarPath(), test.getTestSettings().getDecisionCycles().get(0), "");
            try(Writer writer = new FileWriter(finalPath.toFile()))
            {
                writer.write(String.join(",", Results.header) + "\n");
                StatefulBeanToCsv<Results> beanToCsv = new StatefulBeanToCsvBuilder<Results>(writer).build();
                beanToCsv.write(testRunner.getResults());
            }
            
            if(!nosummary)
            {
                finalPath = settings.getCsvDirectory().resolve(settings.getSummaryFile());
                newFile = !Files.exists(finalPath);
                try(Writer writer = new FileWriter(finalPath.toFile(), true))
                {
                    if(newFile)
                    {
                        writer.write(String.join(",", Results.header) + "\n");
                    }
                    StatefulBeanToCsv<Results> beanToCsv = new StatefulBeanToCsvBuilder<Results>(writer).build();
                    beanToCsv.write(testRunner.getResults());
                }
            }
        }
        
        out.print("\n");
        
        out.flush();
    }
    
}

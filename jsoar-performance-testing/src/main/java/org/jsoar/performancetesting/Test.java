/**
 * 
 */
package org.jsoar.performancetesting;

import java.nio.file.Path;

import org.jsoar.kernel.SoarException;

/**
 * This is an interface for both JSoar and CSoar tests
 * 
 * @author ALT
 *
 */
public interface Test
{
    /**
     * This initializes a test and is where the testName, testFile, and number
     * of decisionCycles to run for is set. This is the equivalent of a
     * constructor.
     * 
     * @param testName
     * @param testFile
     * @param decisionCycles
     */
    public void initialize(String testName, Path testFile,
            TestSettings settings);

    /**
     * 
     * @return a test's name
     */
    public String getTestName();

    /**
     * 
     * @return the path to the Soar file
     */
    public Path getTestFile();

    /**
     * Runs the test for a given runCount with a given seed.
     * 
     * @param runCount
     * @param seed
     * @return whether running a test was successful or not.
     * @throws SoarException
     */
    public boolean run(int runCount) throws SoarException;

    /**
     * Resets the test. Resets epmem, smem, and init's soar
     * 
     * @return whether the reset was successful.
     */
    public boolean reset();

    /**
     * 
     * @return the display name for the test, either 'JSoar' or 'CSoar'.
     */
    public String getDisplayName();

    /**
     * Gets the time in seconds that the test ran for on the CPU (total). This
     * is actually a wallclock not a process timer!
     * 
     * @return the cpu run time for the last run.
     */
    public double getCPURunTime();

    /**
     * Gets the time in seconds that the test ran for on the CPU (total)
     * including a few extras. This is actually a wallclock not a process timer!
     * 
     * @return the kernel run time for the last run.
     */
    public double getKernelRunTime();

    /**
     * 
     * @return the number of decisions the last test ran for.
     */
    public int getDecisionCyclesRunFor();

    /**
     * 
     * @return the memory used by the last run measured at the end of the test's
     *         run.
     */
    public long getMemoryForRun();

    /**
     * 
     * @return The settings for the test
     */
    public TestSettings getTestSettings();
}

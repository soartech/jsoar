/**
 * 
 */
package org.jsoar.performancetesting;

import org.jsoar.kernel.SoarException;

/**
 * @author Alex
 *
 */
public interface Test
{
    /**
     * This initializes a test and is where the testName, testFile, and number of
     * decisionCycles to run for is set.  This is the equivilent of a constructor.
     * 
     * @param testName
     * @param testFile
     * @param decisionCycles
     */
    public void initialize(String testName, String testFile, Integer decisionCycles);
    
    public String getTestName();
    public String getTestFile();
    
    public boolean run(int runCount, Long seed) throws SoarException;
    //Resets the test.  Resets epmem, smem, and init's soar
    public boolean reset();
    
    public String getDisplayName();
    
    // Gets the time in seconds that the test ran for on the CPU (total).  This is
    // actually a wallclock not a process timer!
    public double getCPURunTime();
    public double getKernelRunTime();
    
    public int getDecisionCyclesRunFor();
    public long getMemoryForRun();
}

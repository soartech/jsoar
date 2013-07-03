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
    public void initialize(String testName, String testFile, Integer decisionCycles);
    
    public String getTestName();
    public String getTestFile();
    
    public boolean run(int runCount, int seed) throws SoarException;
    //Resets the test.  Resets epmem, smem, and init's soar
    public boolean reset();
    
    //Gets the time in seconds that the test ran for on the CPU (total).  This is actually a wallclock not a process timer!
    public double getCPURunTime();
    public double getKernelRunTime();
    
    public int getDecisionCyclesRunFor();
    public long getMemoryForRun();
}

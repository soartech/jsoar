/** */
package org.jsoar.performancetesting;

import java.io.PrintWriter;
import org.jsoar.kernel.SoarException;
import org.jsoar.performancetesting.yaml.TestSettings;

/**
 * A class for running tests. This can run both CSoar and JSoar tests and doesn't make a difference.
 *
 * @author ALT
 */
public class TestRunner {
  private final PrintWriter out;

  private Test test;

  private RawResults rawResults = new RawResults();
  private Results results;

  public TestRunner(Test test, PrintWriter out) {
    this.test = test;
    this.results = new Results(test);
    this.out = out;
  }

  /**
   * This runs a test a single iterator and records all the statistics.
   *
   * @param runCount
   * @return Whether the run was successful
   * @throws SoarException
   */
  public boolean runSingleIteration(int runCount) throws SoarException {
    test.reset();

    boolean result = test.run(runCount);

    rawResults.cpuTimes.add(test.getCPURunTime());
    rawResults.kernelTimes.add(test.getKernelRunTime());

    rawResults.decisionCycles.add(test.getDecisionCyclesRunFor());

    rawResults.memoryLoads.add(test.getMemoryForRun());

    return result;
  }

  /**
   * Runs a test for a passed runCount and for each JSoar test, a passed warmUpCount. Also sets the
   * seed of the test from the passed parameter.
   *
   * @return Whether running all the tests was successful or not.
   * @throws SoarException
   */
  boolean runTestsForAverage(TestSettings settings) throws SoarException {
    if (settings.isJsoarEnabled() && settings.getWarmUpCount() > 0) {
      out.print("Warming Up: ");
      out.flush();

      for (int i = 0; i < settings.getWarmUpCount(); i++) {
        test.reset();

        boolean result = test.run(i);

        if (!result) return false;

        out.print(".");
        out.flush();
      }

      out.print("\n");
    }

    out.print("Running Test: ");
    out.flush();

    for (int i = 0; i < settings.getRunCount(); i++) {
      boolean result = runSingleIteration(i);

      if (!result) return false;

      out.print(".");
      out.flush();
    }

    out.print("\n");
    out.flush();

    return true;
  }

  /** @return the test this test runner was running. */
  public Test getTest() {
    return test;
  }

  public RawResults getRawResults() {
    return rawResults;
  }

  public Results getResults() {
    results.updateStats(rawResults);
    return results;
  }
}

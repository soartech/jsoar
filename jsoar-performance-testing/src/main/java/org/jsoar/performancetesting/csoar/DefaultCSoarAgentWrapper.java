package org.jsoar.performancetesting.csoar;

import java.nio.file.Path;

/**
 * This class is just an assertion error version of the CSoarAgentWrapper class. It's purpose is to
 * let the user know that the Agent class isn't loaded and therefore you cannot use CSoar functions.
 * This can happen if CSoar wasn't found in the directory specified for instance.
 *
 * @author ALT
 */
public class DefaultCSoarAgentWrapper implements CSoarAgentWrapper {
  @Override
  public boolean LoadProductions(Path file) {
    System.out.println("Cannot call method on an unloaded Agent class.");
    throw new RuntimeException("Could not load CSoar");
  }

  @Override
  public String RunSelfForever() {
    System.out.println("Cannot call method on an unloaded Agent class.");
    throw new RuntimeException("Could not load CSoar");
  }

  @Override
  public String ExecuteCommandLine(String command) {
    System.out.println("Cannot call method on an unloaded Agent class.");
    throw new RuntimeException("Could not load CSoar");
  }

  @Override
  public String RunSelf(Integer decisionCyclesToRun) {
    System.out.println("Cannot call method on an unloaded Agent class.");
    throw new RuntimeException("Could not load CSoar");
  }

  @Override
  public Object getAgentImpl() {
    System.out.println("Cannot call method on an unloaded agent class.");
    throw new RuntimeException("Could not load CSoar");
  }
}

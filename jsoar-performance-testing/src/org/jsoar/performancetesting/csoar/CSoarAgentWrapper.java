package org.jsoar.performancetesting.csoar;

public interface CSoarAgentWrapper
{
    boolean LoadProductions(String file);
    
    String RunSelfForever();
    String RunSelf(Integer decisionCyclesToRun);
    
    String ExecuteCommandLine(String command);
}

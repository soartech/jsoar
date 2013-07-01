package org.jsoar.performancetesting.csoar;

public interface CSoarAgentWrapper
{
    boolean LoadProductions(String file);
    
    String RunSelfForever();
    
    String ExecuteCommandLine(String command);
}

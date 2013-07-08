package org.jsoar.performancetesting.csoar;

/**
 * This is a wrapper class around the functions used by the test to
 * allow for CSoar at-runtime dependency resolution.
 * 
 * @author ALT
 *
 */
public interface CSoarAgentWrapper
{
    boolean LoadProductions(String file);
    
    String RunSelfForever();
    String RunSelf(Integer decisionCyclesToRun);
    
    String ExecuteCommandLine(String command);
    
    Object getAgentImpl();
}

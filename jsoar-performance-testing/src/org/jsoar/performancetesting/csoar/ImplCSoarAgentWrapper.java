/**
 * 
 */
package org.jsoar.performancetesting.csoar;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This is the implementation of a loaded CSoar sml.Agent class
 * around the CSoarAgentWrapper.  It implements all functions
 * that are used from sml.Agent.
 * 
 * @author ALT
 *
 */
public class ImplCSoarAgentWrapper implements CSoarAgentWrapper
{
    private Object agentImpl;
    private Class<?> agent;
        
    private Method loadProductions;
    private Method runSelfForever;
    private Method runSelf;
    private Method executeCommandLine;
    
    ImplCSoarAgentWrapper(Object agentImpl, Class<?> agent)
    {
        this.agentImpl = agentImpl;
        this.agent = agent;
        
        try
        {
            loadProductions = this.agent.getDeclaredMethod("LoadProductions", String.class);
            runSelfForever = this.agent.getDeclaredMethod("RunSelfForever");
            runSelf = this.agent.getDeclaredMethod("RunSelf", int.class);
            executeCommandLine = this.agent.getDeclaredMethod("ExecuteCommandLine", String.class);
        }
        catch (NoSuchMethodException | SecurityException e)
        {
            System.out.println("Failed to load methods for agent class! Everything will fail horribly!");
            System.out.println(e.getMessage());
        }
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.performancetesting.csoar.CSoarAgentWrapper#LoadProductions(java.lang.String)
     */
    @Override
    public boolean LoadProductions(String file)
    {
        try
        {
            return (boolean) loadProductions.invoke(agentImpl, file);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            System.out.println("Failed to load productions!");
            System.out.println(e.getMessage());
            return false;
        }
    }

    /* (non-Javadoc)
     * @see org.jsoar.performancetesting.csoar.CSoarAgentWrapper#RunSelfForever()
     */
    @Override
    public String RunSelfForever()
    {
        try
        {
            return (String) runSelfForever.invoke(agentImpl);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            System.out.println("Failed to run agent forever!");
            System.out.println(e.getMessage());
            return "Failure!";
        }
    }
    
    @Override
    public String RunSelf(Integer decisionCyclesToRun)
    {
        try
        {
            return (String) runSelf.invoke(agentImpl, decisionCyclesToRun);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            System.out.println("Failed to runSelf() for decisions " + decisionCyclesToRun);
            System.out.println(e.getMessage());
            return "Failure!";
        }
    }

    /* (non-Javadoc)
     * @see org.jsoar.performancetesting.csoar.CSoarAgentWrapper#ExecuteCommandLine(java.lang.String)
     */
    @Override
    public String ExecuteCommandLine(String command)
    {
        try
        {
            return (String) executeCommandLine.invoke(agentImpl, command);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            System.out.println("Failed to execute command line on agent!");
            System.out.println(e.getMessage());
            return "Failure!";
        }
    }
}

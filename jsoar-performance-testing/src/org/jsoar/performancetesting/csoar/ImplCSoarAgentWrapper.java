/**
 * 
 */
package org.jsoar.performancetesting.csoar;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * @author ALT
 *
 */
public class ImplCSoarAgentWrapper implements CSoarAgentWrapper
{
    private Object agentImpl;
    private HashMap<String, Class> agentMap;
    
    private String label;
    
    private Method loadProductions;
    private Method runSelfForever;
    private Method executeCommandLine;
    
    ImplCSoarAgentWrapper(Object agentImpl, HashMap<String, Class> agentMap, String label)
    {
        this.agentImpl = agentImpl;
        this.agentMap = agentMap;
        
        try
        {
            loadProductions = agentMap.get(label).getDeclaredMethod("LoadProductions", String.class);
            runSelfForever = agentMap.get(label).getDeclaredMethod("RunSelfForever");
            executeCommandLine = agentMap.get(label).getDeclaredMethod("ExecuteCommandLine", String.class);
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

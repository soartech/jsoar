/**
 * 
 */
package org.jsoar.performancetesting.csoar;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;

/**
 * @author ALT
 *
 */
public class ImplCSoarKernelWrapper implements CSoarKernelWrapper
{
    private Object kernelImpl;
    private HashMap<String, Class> kernelMap;
    private HashMap<String, Class> agentMap;
    
    private String label;
    
    private Method createAgent;
    
    public ImplCSoarKernelWrapper(Object kernelImpl, HashMap<String, Class> kernelMap, HashMap<String, Class> agentMap, String label)
    {
        this.label = label;
        this.kernelImpl = kernelImpl;
        this.kernelMap = kernelMap;
        this.agentMap = agentMap;
        
        try
        {
            createAgent = kernelMap.get(label).getDeclaredMethod("CreateAgent", String.class);
        }
        catch (NoSuchMethodException | SecurityException e)
        {
            System.out.println("Failed to load methods for kernel class! Everything will fail horribly!");
            System.out.println(e.getMessage());
        }
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.performancetesting.csoar.CSoarKernelWrapper#CreateAgent(java.lang.String)
     */
    @Override
    public CSoarAgentWrapper CreateAgent(String name)
    {
        try
        {
            Object agentImpl = createAgent.invoke(kernelImpl, name);
            return new ImplCSoarAgentWrapper(agentImpl, agentMap, label);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            System.out.println("Failed to create agent! Everything will fail horribly!");
            return new DefaultCSoarAgentWrapper();
        }
    }
}

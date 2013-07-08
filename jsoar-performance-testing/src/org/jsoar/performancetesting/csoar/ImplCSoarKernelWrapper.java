/**
 * 
 */
package org.jsoar.performancetesting.csoar;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This is the implementation of a loaded sml.Kernel class
 * using the CSoarKernelWrapper class.  It implements all
 * functions that are used by the tests.
 * 
 * @author ALT
 *
 */
public class ImplCSoarKernelWrapper implements CSoarKernelWrapper
{
    private Object kernelImpl;
    private Class<?> kernel;
    private Class<?> agent;
        
    private Method createAgent;
    private Method destroyAgent;
    
    public ImplCSoarKernelWrapper(Object kernelImpl, Class<?> kernel, Class<?> agent)
    {
        this.kernelImpl = kernelImpl;
        this.kernel = kernel;
        this.agent = agent;
        
        try
        {
            createAgent = this.kernel.getDeclaredMethod("CreateAgent", String.class);
            destroyAgent = this.kernel.getDeclaredMethod("DestroyAgent", agent);
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
            return new ImplCSoarAgentWrapper(agentImpl, agent);
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            System.out.println("Failed to create agent! Everything will fail horribly!");
            return new DefaultCSoarAgentWrapper();
        }
    }
    
    @Override
    public void DestroyAgent(CSoarAgentWrapper agent)
    {
        try
        {
            destroyAgent.invoke(kernelImpl, agent.getAgentImpl());
        }
        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
        {
            System.out.println("Failed to create agent! Everything will fail horribly!");
        }
    }
}

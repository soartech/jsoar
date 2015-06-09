package org.jsoar.performancetesting.csoar;

/**
 * This class is just an assertion error version of the CSoarKernelWrapper
 * class. It's purpose is to let the user know that the Kernel class isn't
 * loaded and therefore you cannot use CSoar functions. This can happen if CSoar
 * wasn't found in the directory specified for instance.
 * 
 * @author ALT
 *
 */
public class DefaultCSoarKernelWrapper implements CSoarKernelWrapper
{
    @Override
    public CSoarAgentWrapper CreateAgent(String name)
    {
        System.out.println("Cannot call methods on non-loaded sml!");
        throw new RuntimeException("Could not load CSoar");
    }

    @Override
    public void DestroyAgent(CSoarAgentWrapper agent)
    {
        System.out.println("Cannot call methods on a non-loaded sml!");
        throw new RuntimeException("Could not load CSoar");
    }

    @Override
    public void StopEventThread()
    {
        System.out.println("Cannot call methods on a non-loaded sml!");
        throw new RuntimeException("Could not load CSoar");
    }

    @Override
    public void Shutdown()
    {
        System.out.println("Cannot call methods on a non-loaded sml!");
        throw new RuntimeException("Could not load CSoar");
    }
}

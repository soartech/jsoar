package org.jsoar.performancetesting.csoar;

public class DefaultCSoarKernelWrapper implements CSoarKernelWrapper
{

    @Override
    public CSoarAgentWrapper CreateAgent(String name)
    {
        System.out.println("Cannot call methods on non-loaded sml!");
        throw new AssertionError();
    }
}

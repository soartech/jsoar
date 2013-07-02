package org.jsoar.performancetesting.csoar;

public class DefaultCSoarAgentWrapper implements CSoarAgentWrapper
{
    @Override
    public boolean LoadProductions(String file)
    {
        System.out.println("Cannot call method on an unloaded Agent class.");
        throw new AssertionError();
    }

    @Override
    public String RunSelfForever()
    {
        System.out.println("Cannot call method on an unloaded Agent class.");
        throw new AssertionError();
    }

    @Override
    public String ExecuteCommandLine(String command)
    {
        System.out.println("Cannot call method on an unloaded Agent class.");
        throw new AssertionError();
    }

    @Override
    public String RunSelf(Integer decisionCyclesToRun)
    {
        System.out.println("Cannot call method on an unloaded Agent class.");
        throw new AssertionError();
    }

}

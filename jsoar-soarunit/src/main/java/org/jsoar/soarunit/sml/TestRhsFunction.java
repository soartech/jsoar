/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 22, 2010
 */
package org.jsoar.soarunit.sml;

import sml.Kernel;
import sml.Kernel.RhsFunctionInterface;

/**
 * @author ray
 */
public class TestRhsFunction implements RhsFunctionInterface
{
    private boolean called = false;
    private String arguments;
    
    public static TestRhsFunction addTestFunction(Kernel kernel, String name)
    {
        final TestRhsFunction testFunction = new TestRhsFunction();
        kernel.AddRhsFunction(name, testFunction, kernel);
        return testFunction;
    }
    
    /**
     * @param name
     */
    public TestRhsFunction()
    {
    }

    /**
     * @return the called
     */
    public boolean isCalled()
    {
        return called;
    }

    /**
     * @return the arguments
     */
    public String getArguments()
    {
        return arguments;
    }

    /* (non-Javadoc)
     * @see sml.Kernel.RhsFunctionInterface#rhsFunctionHandler(int, java.lang.Object, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public String rhsFunctionHandler(int eventID, Object data,
            String agentName, String functionName, String argument)
    {
        this.called = true;
        this.arguments = argument;
        ((Kernel) data).StopAllAgents();
        return "";
    }
}

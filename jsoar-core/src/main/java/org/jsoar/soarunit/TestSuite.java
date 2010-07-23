/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 22, 2010
 */
package org.jsoar.soarunit;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.util.StringTools;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandInterpreter;

/**
 * @author ray
 */
public class TestSuite
{
    private final String name;
    private String setup = "";
    private final List<Test> tests = new ArrayList<Test>();
    
    public static TestSuite fromFile(File file) throws SoarException
    {
        final Agent agent = new Agent("TestSuite");
        try
        {
            final SoarCommandInterpreter interp = agent.getInterpreter();
            // TODO remove all commands...
            final TestSuite suite = new TestSuite(file.getName());
            interp.addCommand("setup", new SoarCommand() {
                @Override
                public String execute(String[] args) throws SoarException
                {
                    suite.setup += "\n";
                    suite.setup += args[1];
                    return "";
                }
            });
            interp.addCommand("test", new SoarCommand() {
                @Override
                public String execute(String[] args) throws SoarException
                {
                    final Test test = new Test(args[1], args[2]);
                    suite.addTest(test);
                    return "";
                }
            });
            
            interp.source(file);
            return suite;
        }
        finally
        {
            agent.dispose();
        }
    }
    
    public TestSuite(String name)
    {
        this.name = name;
    }

    /**
     * @return the setup
     */
    public String getSetup()
    {
        return setup;
    }

    /**
     * @param setup the setup to set
     */
    public void setSetup(String setup)
    {
        this.setup = setup;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }
    
    public void addTest(Test test)
    {
        tests.add(test);
    }
 
    public boolean run() throws SoarException
    {
        for(Test test : tests)
        {
            final Agent agent = new Agent(test.getName());
            agent.initialize();
            try
            {
                if(!runTest(test, agent))
                {
                    return false;
                }
            }
            finally
            {
                agent.dispose();
            }
        }
        return true;
    }
    
    private TestRhsFunction addTestFunction(Agent agent, String name)
    {
        final TestRhsFunction succeededFunction = new TestRhsFunction(agent, name);
        agent.getRhsFunctions().registerHandler(succeededFunction);
        return succeededFunction;
    }

    private boolean runTest(Test test, final Agent agent) throws SoarException
    {
        final StringWriter output = new StringWriter();
        agent.getTrace().setWatchLevel(5);
        agent.getPrinter().addPersistentWriter(output);
        
        final TestRhsFunction succeededFunction = addTestFunction(agent, "succeeded");
        final TestRhsFunction failedFunction = addTestFunction(agent, "failed");
        
        agent.getInterpreter().eval(setup);
        
        System.out.printf("Running test: %s%n", test.getName());
        agent.getInterpreter().eval(test.getContent());
        final int cycles = 50000; 
        agent.runFor(cycles, RunType.DECISIONS);
        agent.getPrinter().flush();
        
        if(failedFunction.isCalled())
        {
            System.out.printf("FAILED: test '%s' failed: %s%n", 
                              test.getName(), 
                              StringTools.join(failedFunction.getArguments(), ", "));
            System.out.println("Agent Output:");
            System.out.println("-----------------------------------------------------------");
            System.out.println(output.toString());
            System.out.println("-----------------------------------------------------------");
            return false;
        }
        else if(!succeededFunction.isCalled())
        {
            final Long actualCycles = agent.getProperties().get(SoarProperties.D_CYCLE_COUNT);
            System.out.printf("FAILED: test '%s' never called succeeded function. Ran %d decisions.%n", 
                              test.getName(), 
                              actualCycles);
            System.out.println("Agent Output:");
            System.out.println("-----------------------------------------------------------");
            System.out.println(output.toString());
            System.out.println("-----------------------------------------------------------");
            return false;
        }
        else
        {
            System.out.printf("PASSED: test '%s' passed: %s%n", 
                              test.getName(), 
                              StringTools.join(succeededFunction.getArguments(), ", "));
            return true;
        }
    }
    
    
}

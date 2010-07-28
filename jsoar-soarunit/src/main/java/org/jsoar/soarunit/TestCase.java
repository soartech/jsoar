/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 22, 2010
 */
package org.jsoar.soarunit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PushbackReader;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.DefaultInterpreterParser;

/**
 * @author ray
 */
public class TestCase
{
    private final File file;
    private final String name;
    private String setup = "";
    private final List<Test> tests = new ArrayList<Test>();
    
    private static String getNameFromFile(File file)
    {
        final String name = file.getName();
        final int dot = name.lastIndexOf('.');
        
        return dot > 0 ? name.substring(0, dot) : name;
    }
    
    public static TestCase fromFile(File file) throws SoarException, IOException
    {
        final PushbackReader reader = new PushbackReader(new BufferedReader(new FileReader(file)));
        try
        {
            final TestCase testCase = new TestCase(file, getNameFromFile(file));
            final DefaultInterpreterParser parser = new DefaultInterpreterParser();
            List<String> parsedCommand = parser.parseCommand(reader);
            while(!parsedCommand.isEmpty())
            {
                final String name = parsedCommand.get(0);
                if("setup".equals(name))
                {
                    testCase.setup += "\n";
                    testCase.setup += parsedCommand.get(1);
                }
                else if("test".equals(name))
                {
                    final Test test = new Test(testCase, parsedCommand.get(1), parsedCommand.get(2));
                    testCase.addTest(test);
                }
                else
                {
                    throw new SoarException("Unsupported SoarUnit command '" + name + "'");
                }
                
                parsedCommand = parser.parseCommand(reader);
            }
            return testCase;
        }
        finally
        {
            reader.close();
        }
    }
    
    public static int getTotalTests(List<TestCase> allCases)
    {
        int result = 0;
        for(TestCase testCase : allCases)
        {
            result += testCase.getTests().size();
        }
        return result;
    }
    
    public TestCase(File file, String name)
    {
        this.file = file;
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
    
    /**
     * @return the file
     */
    public File getFile()
    {
        return file;
    }

    public void addTest(Test test)
    {
        tests.add(test);
    }
 
    /**
     * @return the tests
     */
    public List<Test> getTests()
    {
        return tests;
    }

    public Test getTest(String name)
    {
        for(Test test : tests)
        {
            if(name.equals(test.getName()))
            {
                return test;
            }
        }
        return null;
    }
    
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return name;
    }
    
}

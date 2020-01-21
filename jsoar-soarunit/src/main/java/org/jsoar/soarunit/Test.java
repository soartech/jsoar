/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 22, 2010
 */
package org.jsoar.soarunit;

import java.io.IOException;

import org.jsoar.kernel.SoarException;


/**
 * @author ray
 */
public class Test
{
    private final TestCase testCase;
    private final String name;
    private final String content;
    
    public Test(TestCase testCase, String name, String content)
    {
        this.testCase = testCase;
        this.name = name;
        this.content = content;
    }

    
    /**
     * @return the owning test case
     */
    public TestCase getTestCase()
    {
        return testCase;
    }


    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return the content
     */
    public String getContent()
    {
        return content;
    }


    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return testCase + "/" + name;
    }


    public Test reload() throws SoarException, IOException
    {
        final TestCase reloadedTestCase = getTestCase().reload();
        
        return reloadedTestCase.getTest(getName());
    }

    
}

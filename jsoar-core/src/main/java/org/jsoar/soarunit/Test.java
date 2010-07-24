/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 22, 2010
 */
package org.jsoar.soarunit;


/**
 * @author ray
 */
public class Test
{
    private final TestSuite suite;
    private final String name;
    private final String content;
    
    public Test(TestSuite suite, String name, String content)
    {
        this.suite = suite;
        this.name = name;
        this.content = content;
    }

    
    /**
     * @return the suite
     */
    public TestSuite getSuite()
    {
        return suite;
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

}

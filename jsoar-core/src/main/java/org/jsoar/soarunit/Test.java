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
    private final String name;
    private final String content;
    
    public Test(String name, String content)
    {
        this.name = name;
        this.content = content;
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

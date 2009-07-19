/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 19, 2009
 */
package org.jsoar.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class StringToolsTest
{

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
    }

    @Test
    public void testJoiningEmptyCollectionReturnsEmptyString()
    {
        assertEquals("", StringTools.join(new ArrayList<String>(), ", "));
    }
    
    @Test
    public void testJoiningSingleElementCollectionReturnsSingleString()
    {
        assertEquals("hello there", StringTools.join(Arrays.asList("hello there"), ", "));
    }
    
    @Test
    public void testJoinMultipleArguments()
    {
        assertEquals("1, 2, 3", StringTools.join(Arrays.asList(1, 2, 3), ", "));
    }

}

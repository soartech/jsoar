/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 19, 2009
 */
package org.jsoar.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class StringToolsTest
{
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception
    {
    }
    
    /**
     * @throws java.lang.Exception
     */
    @AfterEach
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

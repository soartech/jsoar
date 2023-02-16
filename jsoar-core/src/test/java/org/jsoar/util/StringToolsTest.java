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
    void setUp() throws Exception
    {
    }
    
    /**
     * @throws java.lang.Exception
     */
    @AfterEach
    void tearDown() throws Exception
    {
    }
    
    @Test
    void testJoiningEmptyCollectionReturnsEmptyString()
    {
        assertEquals("", StringTools.join(new ArrayList<String>(), ", "));
    }
    
    @Test
    void testJoiningSingleElementCollectionReturnsSingleString()
    {
        assertEquals("hello there", StringTools.join(Arrays.asList("hello there"), ", "));
    }
    
    @Test
    void testJoinMultipleArguments()
    {
        assertEquals("1, 2, 3", StringTools.join(Arrays.asList(1, 2, 3), ", "));
    }
    
}

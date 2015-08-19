/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 19, 2009
 */
package org.jsoar.util;

import android.test.AndroidTestCase;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author ray
 */
public class StringToolsTest extends AndroidTestCase
{

    /**
     * @throws java.lang.Exception
     */
    @Override
    public void setUp() throws Exception
    {
    }

    /**
     * @throws java.lang.Exception
     */
    @Override
    public void tearDown() throws Exception
    {
    }

    public void testJoiningEmptyCollectionReturnsEmptyString()
    {
        assertEquals("", StringTools.join(new ArrayList<String>(), ", "));
    }
    
    public void testJoiningSingleElementCollectionReturnsSingleString()
    {
        assertEquals("hello there", StringTools.join(Arrays.asList("hello there"), ", "));
    }
    
    public void testJoinMultipleArguments()
    {
        assertEquals("1, 2, 3", StringTools.join(Arrays.asList(1, 2, 3), ", "));
    }

}

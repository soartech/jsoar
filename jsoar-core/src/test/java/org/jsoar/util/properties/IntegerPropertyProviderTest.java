/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 23, 2008
 */
package org.jsoar.util.properties;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class IntegerPropertyProviderTest
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

    /**
     * Test method for {@link org.jsoar.util.properties.IntegerPropertyProvider#toString()}.
     */
    @Test
    public void testToString()
    {
        final PropertyKey<Integer> key = PropertyKey.builder("testToString", Integer.class).defaultValue(1234).build();
        IntegerPropertyProvider provider = new IntegerPropertyProvider(key);
        assertEquals("1234", provider.toString());
    }

}

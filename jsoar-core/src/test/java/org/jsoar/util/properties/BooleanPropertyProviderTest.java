package org.jsoar.util.properties;
import static org.junit.Assert.*;

import org.jsoar.util.properties.BooleanPropertyProvider;
import org.jsoar.util.properties.PropertyKey;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 23, 2008
 */

/**
 * @author ray
 */
public class BooleanPropertyProviderTest
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
     * Test method for {@link org.jsoar.util.properties.BooleanPropertyProvider#toString()}.
     */
    @Test
    public void testToString()
    {
        final PropertyKey<Boolean> key = PropertyKey.builder("testToString", Boolean.class).defaultValue(true).build();
        final BooleanPropertyProvider provider = new BooleanPropertyProvider(key);
        assertEquals("true", provider.toString());
        provider.set(false);
        assertEquals("false", provider.toString());
    }

}

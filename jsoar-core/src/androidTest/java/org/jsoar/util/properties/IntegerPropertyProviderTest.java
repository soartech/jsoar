/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 23, 2008
 */
package org.jsoar.util.properties;

import android.test.AndroidTestCase;

/**
 * @author ray
 */
public class IntegerPropertyProviderTest extends AndroidTestCase
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

    /**
     * Test method for {@link org.jsoar.util.properties.IntegerPropertyProvider#toString()}.
     */
    public void testToString()
    {
        final PropertyKey<Integer> key = PropertyKey.builder("testToString", Integer.class).defaultValue(1234).build();
        IntegerPropertyProvider provider = new IntegerPropertyProvider(key);
        assertEquals("1234", provider.toString());
    }

}

/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 23, 2008
 */
package org.jsoar.util.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class IntegerPropertyProviderTest
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
    
    /**
     * Test method for {@link org.jsoar.util.properties.IntegerPropertyProvider#toString()}.
     */
    @Test
    void testToString()
    {
        final PropertyKey<Integer> key = PropertyKey.builder("testToString", Integer.class).defaultValue(1234).build();
        IntegerPropertyProvider provider = new IntegerPropertyProvider(key);
        assertEquals("1234", provider.toString());
    }
    
}

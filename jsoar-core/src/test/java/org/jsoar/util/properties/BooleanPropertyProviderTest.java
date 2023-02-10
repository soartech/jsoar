package org.jsoar.util.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

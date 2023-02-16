/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 18, 2008
 */
package org.jsoar.util.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.jsoar.util.ByRef;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class PropertyManagerTest
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
    public void testGetReturnsDefaultValue()
    {
        final PropertyKey<String> KEY = PropertyKey.builder("test", String.class).defaultValue("default").build();
        final PropertyKey<Integer> KEY2 = PropertyKey.builder("test2", Integer.class).defaultValue(666).build();
        PropertyManager pm = new PropertyManager();
        
        assertEquals("default", pm.get(KEY));
        assertEquals(666, pm.get(KEY2).intValue());
    }
    
    @Test
    public void testSetCreatesDefaultProvider()
    {
        final PropertyKey<String> KEY = PropertyKey.builder("test", String.class).defaultValue("default").build();
        final PropertyKey<Integer> KEY2 = PropertyKey.builder("test2", Integer.class).defaultValue(666).build();
        PropertyManager pm = new PropertyManager();
        
        assertEquals("default", pm.get(KEY));
        assertEquals("default", pm.set(KEY, "new"));
        assertEquals("new", pm.get(KEY));
        
        assertEquals(666, pm.get(KEY2).intValue());
        assertEquals(666, pm.set(KEY2, 555).intValue());
        assertEquals(555, pm.get(KEY2).intValue());
    }
    
    @Test
    public void testCustomProvider()
    {
        final PropertyKey<String> KEY = PropertyKey.builder("test", String.class).defaultValue("default").build();
        final ByRef<String> storage = ByRef.create("hello");
        final PropertyProvider<String> provider = new PropertyProvider<>()
        {
            
            @Override
            public String get()
            {
                return storage.value;
            }
            
            @Override
            public String set(String value)
            {
                String old = get();
                storage.value = value;
                return old;
            }
        };
        PropertyManager pm = new PropertyManager();
        
        assertEquals("default", pm.get(KEY));
        pm.setProvider(KEY, provider);
        assertEquals("hello", pm.get(KEY));
        pm.set(KEY, "goodbye");
        assertEquals("goodbye", pm.get(KEY));
        assertEquals("goodbye", storage.value);
    }
    
    @Test
    public void testEvents()
    {
        final PropertyKey<String> KEY = PropertyKey.builder("test", String.class).defaultValue("default").build();
        final PropertyKey<Integer> KEY2 = PropertyKey.builder("test2", Integer.class).defaultValue(666).build();
        PropertyManager pm = new PropertyManager();
        final int[] key1Count = { 0 };
        pm.addListener(KEY, new PropertyListener<String>()
        {
            
            @Override
            public void propertyChanged(PropertyChangeEvent<String> event)
            {
                assertNotNull(event);
                assertEquals(KEY, event.getKey());
                assertEquals("default", event.getOldValue());
                assertEquals("new", event.getNewValue());
                key1Count[0]++;
            }
        });
        final int[] key2Count = { 0 };
        pm.addListener(KEY2, new PropertyListener<Integer>()
        {
            
            @Override
            public void propertyChanged(PropertyChangeEvent<Integer> event)
            {
                assertNotNull(event);
                assertEquals(KEY2, event.getKey());
                assertEquals(666, event.getOldValue().intValue());
                assertEquals(555, event.getNewValue().intValue());
                key2Count[0]++;
            }
        });
        
        assertEquals("default", pm.get(KEY));
        assertEquals("default", pm.set(KEY, "new"));
        assertEquals("new", pm.get(KEY));
        
        assertEquals(666, pm.get(KEY2).intValue());
        assertEquals(666, pm.set(KEY2, 555).intValue());
        assertEquals(555, pm.get(KEY2).intValue());
        
        assertEquals(1, key1Count[0]);
        assertEquals(1, key2Count[0]);
    }
    
    @Test
    public void testCanGetAKeyByName()
    {
        final PropertyKey<String> KEY = PropertyKey.builder("test", String.class).defaultValue("default").build();
        final PropertyKey<Integer> KEY2 = PropertyKey.builder("test2", Integer.class).defaultValue(666).build();
        final PropertyManager pm = new PropertyManager();
        pm.set(KEY, "foo");
        pm.set(KEY2, 99);
        
        assertSame(KEY, pm.getKey("test"));
        assertSame(KEY2, pm.getKey("test2"));
        
    }
}

/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 18, 2008
 */
package org.jsoar.util.properties;

import static org.junit.Assert.*;

import org.jsoar.util.ByRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class PropertyManagerTest
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
    public void testGetReturnsDefaultValue()
    {
        final PropertyKey<String> KEY = PropertyKey.create("test", String.class, "default");
        final PropertyKey<Integer> KEY2 = PropertyKey.create("test2", Integer.class, 666);
        PropertyManager pm = new PropertyManager();
        
        assertEquals("default", pm.get(KEY));
        assertEquals(666, pm.get(KEY2).intValue());
    }
    
    @Test
    public void testSetCreatesDefaultProvider()
    {
        final PropertyKey<String> KEY = PropertyKey.create("test", String.class, "default");
        final PropertyKey<Integer> KEY2 = PropertyKey.create("test2", Integer.class, 666);
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
        final PropertyKey<String> KEY = PropertyKey.create("test", String.class, "default");
        final ByRef<String> storage = ByRef.create("hello");
        final PropertyProvider<String> provider = new PropertyProvider<String>() {

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
        final PropertyKey<String> KEY = PropertyKey.create("test", String.class, "default");
        final PropertyKey<Integer> KEY2 = PropertyKey.create("test2", Integer.class, 666);
        PropertyManager pm = new PropertyManager();
        final int[] key1Count = new int[] { 0 };
        pm.addListener(KEY, new PropertyListener<String>(){

            @Override
            public void propertyChanged(PropertyChangeEvent<String> event)
            {
                assertNotNull(event);
                assertEquals(KEY, event.getKey());
                assertEquals("default", event.getOldValue());
                assertEquals("new", event.getNewValue());
                key1Count[0]++;
            }});
        final int[] key2Count = new int[] { 0 };
        pm.addListener(KEY2, new PropertyListener<Integer>(){

            @Override
            public void propertyChanged(PropertyChangeEvent<Integer> event)
            {
                assertNotNull(event);
                assertEquals(KEY2, event.getKey());
                assertEquals(666, event.getOldValue().intValue());
                assertEquals(555, event.getNewValue().intValue());
                key2Count[0]++;
            }});
        
        assertEquals("default", pm.get(KEY));
        assertEquals("default", pm.set(KEY, "new"));
        assertEquals("new", pm.get(KEY));
        
        assertEquals(666, pm.get(KEY2).intValue());
        assertEquals(666, pm.set(KEY2, 555).intValue());
        assertEquals(555, pm.get(KEY2).intValue());
        
        assertEquals(1, key1Count[0]);
        assertEquals(1, key2Count[0]);
    }

}

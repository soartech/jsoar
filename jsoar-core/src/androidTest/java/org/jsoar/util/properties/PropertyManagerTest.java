/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 18, 2008
 */
package org.jsoar.util.properties;

import android.test.AndroidTestCase;

import org.jsoar.util.ByRef;

/**
 * @author ray
 */
public class PropertyManagerTest extends AndroidTestCase
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

    public void testGetReturnsDefaultValue()
    {
        final PropertyKey<String> KEY = PropertyKey.builder("test", String.class).defaultValue("default").build();
        final PropertyKey<Integer> KEY2 = PropertyKey.builder("test2", Integer.class).defaultValue(666).build();
        PropertyManager pm = new PropertyManager();
        
        assertEquals("default", pm.get(KEY));
        assertEquals(666, pm.get(KEY2).intValue());
    }
    
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
    
    public void testCustomProvider()
    {
        final PropertyKey<String> KEY = PropertyKey.builder("test", String.class).defaultValue("default").build();
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
    
    public void testEvents()
    {
        final PropertyKey<String> KEY = PropertyKey.builder("test", String.class).defaultValue("default").build();
        final PropertyKey<Integer> KEY2 = PropertyKey.builder("test2", Integer.class).defaultValue(666).build();
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

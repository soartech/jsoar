/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 19, 2009
 */
package org.jsoar.kernel.io.beans;

import static org.junit.Assert.*;

import java.awt.Point;
import java.lang.reflect.InvocationTargetException;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.events.OutputEvent;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.util.ByRef;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class SoarBeanReaderTest
{
    private Agent agent;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
        this.agent = new Agent();
        this.agent.initialize();
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
        this.agent = null;
    }

    public static class TestReadBeanWithPublicFields
    {
        public int integerProp;
        public double doubleProp;
        public String stringProp;
    }
    @Test
    public void testReadBeanWithPublicFields() throws Exception
    {
        agent.getProperties().set(SoarProperties.WAITSNC, false);         
        agent.getProductions().loadProduction("" +
                "testReadBeanWithNoSubObjects\n" +
                "(state <s> ^superstate nil ^io.output-link <ol>)\n" +
                "-->\n" +
                "(<ol> ^test <t>)\n" +
                "(<t> ^integerProp 99 ^doubleProp 3.14 ^stringProp |A String| ^arg |arg0| ^arg |arg1|)\n" +
                "");
        
        final SoarBeanReader converter = new SoarBeanReader();
        
        final ByRef<TestReadBeanWithPublicFields> bean = ByRef.create(null);
        agent.getEventManager().addListener(OutputEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                final Wme testCommand = agent.getInputOutput().getPendingCommands().get(0);
                assertNotNull(testCommand);
                try
                {
                    bean.value = converter.read(testCommand.getValue().asIdentifier(), TestReadBeanWithPublicFields.class);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }});
        
        agent.runFor(1, RunType.DECISIONS);
        assertNotNull(bean.value);
        assertEquals(99, bean.value.integerProp);
        assertEquals(3.14, bean.value.doubleProp, 0.0001);
        assertEquals("A String", bean.value.stringProp);
    }  
    
    public static class TestReadBeanWithNoSubObjects
    {
        private int integerProp;
        private double doubleProp;
        private String stringProp;

        public int getIntegerProp() { return integerProp; }
        public void setIntegerProp(int integerProp) { this.integerProp = integerProp; }
        public double getDoubleProp() { return doubleProp; }
        public void setDoubleProp(double doubleProp) { this.doubleProp = doubleProp; }
        public String getStringProp() { return stringProp; }
        public void setStringProp(String stringProp) { this.stringProp = stringProp;}
        
    }
    
    /**
     * Test method for {@link org.jsoar.kernel.io.beans.SoarBeanReader#read(org.jsoar.kernel.symbols.Identifier, java.lang.Class)}.
     */
    @Test
    public void testReadBeanWithNoSubObjects() throws Exception
    {
        agent.getProperties().set(SoarProperties.WAITSNC, false);         
        agent.getProductions().loadProduction("" +
                "testReadBeanWithNoSubObjects\n" +
                "(state <s> ^superstate nil ^io.output-link <ol>)\n" +
                "-->\n" +
                "(<ol> ^test <t>)\n" +
                "(<t> ^integerProp 99 ^doubleProp 3.14 ^stringProp |A String| ^arg |arg0| ^arg |arg1|)\n" +
                "");
        
        final SoarBeanReader converter = new SoarBeanReader();
        
        final ByRef<TestReadBeanWithNoSubObjects> bean = ByRef.create(null);
        agent.getEventManager().addListener(OutputEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                final Wme testCommand = agent.getInputOutput().getPendingCommands().get(0);
                assertNotNull(testCommand);
                try
                {
                    bean.value = converter.read(testCommand.getValue().asIdentifier(), TestReadBeanWithNoSubObjects.class);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }});
        
        agent.runFor(1, RunType.DECISIONS);
        assertNotNull(bean.value);
        assertEquals(99, bean.value.integerProp);
        assertEquals(3.14, bean.value.doubleProp, 0.0001);
        assertEquals("A String", bean.value.stringProp);
    }

    public static class TestReadBeanWithSubObjects
    {
        private TestReadBeanWithNoSubObjects subObject;

        public TestReadBeanWithNoSubObjects getSubObject() { return subObject; }
        public void setSubObject(TestReadBeanWithNoSubObjects subObject) { this.subObject = subObject; }
    }
    
    @Test
    public void testReadBeanWithSubObjects() throws Exception
    {
        agent.getProperties().set(SoarProperties.WAITSNC, false);         
        agent.getProductions().loadProduction("" +
                "testReadBeanWithNoSubObjects\n" +
                "(state <s> ^superstate nil ^io.output-link <ol>)\n" +
                "-->\n" +
                "(<ol> ^test <t>)\n" +
                "(<t> ^subObject <sub>)\n" +
                "(<sub> ^integerProp 99 ^doubleProp 3.14 ^stringProp |A String| ^arg |arg0| ^arg |arg1|)\n" +
                "");
        
        final SoarBeanReader converter = new SoarBeanReader();
        
        final ByRef<TestReadBeanWithSubObjects> bean = ByRef.create(null);
        agent.getEventManager().addListener(OutputEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                final Wme testCommand = agent.getInputOutput().getPendingCommands().get(0);
                assertNotNull(testCommand);
                try
                {
                    bean.value = converter.read(testCommand.getValue().asIdentifier(), TestReadBeanWithSubObjects.class);
                }
                catch (SoarBeanException e)
                {
                    throw new RuntimeException(e);
                }
            }});
        
        agent.runFor(1, RunType.DECISIONS);
        assertNotNull(bean.value);
        assertNotNull(bean.value.subObject);
        
        final TestReadBeanWithNoSubObjects sub = bean.value.subObject;
        assertEquals(99, sub.integerProp);
        assertEquals(3.14, sub.doubleProp, 0.0001);
        assertEquals("A String", sub.stringProp);
    }
    
    public static class TestReadBeanWithArrayField
    {
        public int[] integers;
        private TestReadBeanWithNoSubObjects[] objs;
        /**
         * @return the objs
         */
        public TestReadBeanWithNoSubObjects[] getObjs()
        {
            return objs;
        }
        /**
         * @param objs the objs to set
         */
        public void setObjs(TestReadBeanWithNoSubObjects[] objs)
        {
            this.objs = objs;
        }
        
        
    }
    
    @Test
    public void testReadBeanWithArrayField() throws Exception
    {
        agent.getProperties().set(SoarProperties.WAITSNC, false);         
        agent.getProductions().loadProduction("" +
                "testReadBeanWithNoSubObjects\n" +
                "(state <s> ^superstate nil ^io.output-link <ol>)\n" +
                "-->\n" +
                "(<ol> ^test <t>)\n" +
                "(<t> ^integers <ints> ^objs <objs>)" +
                "(<ints> ^int 1 ^int 2 ^int 3 ^int 4)\n" +
                "(<objs> ^obj <o1> ^obj <o2>)\n" +
                "(<o1> ^integerProp 99 ^doubleProp 3.14 ^stringProp |A String|)\n" +
                "(<o2> ^integerProp 100 ^doubleProp 4.14 ^stringProp |B String|)\n" +
                               "");
        
        final SoarBeanReader converter = new SoarBeanReader();
        
        final ByRef<TestReadBeanWithArrayField> bean = ByRef.create(null);
        agent.getEventManager().addListener(OutputEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                final Wme testCommand = agent.getInputOutput().getPendingCommands().get(0);
                assertNotNull(testCommand);
                try
                {
                    bean.value = converter.read(testCommand.getValue().asIdentifier(), TestReadBeanWithArrayField.class);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }});
        
        agent.runFor(1, RunType.DECISIONS);
        assertNotNull(bean.value);
        assertEquals(4, bean.value.integers.length);
        assertEquals(1, bean.value.integers[0]);
        assertEquals(2, bean.value.integers[1]);
        assertEquals(3, bean.value.integers[2]);
        assertEquals(4, bean.value.integers[3]);
        
        assertEquals(2, bean.value.objs.length);
        final TestReadBeanWithNoSubObjects  o1 = bean.value.objs[0];
        assertEquals(99, o1.integerProp);
        assertEquals(3.14, o1.doubleProp, 0.0001);
        assertEquals("A String", o1.stringProp);
        
        final TestReadBeanWithNoSubObjects  o2 = bean.value.objs[1];
        assertEquals(100, o2.integerProp);
        assertEquals(4.14, o2.doubleProp, 0.0001);
        assertEquals("B String", o2.stringProp);
    }  
}

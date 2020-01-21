/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 29, 2010
 */
package org.jsoar.soarunit.sml;


import static org.junit.Assert.*;

import org.jsoar.soarunit.FiringCounts;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class SmlTestAgentTest
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
    public void testCanExtractFiringCountsFromPrintedOutput()
    {
        final FiringCounts fc = SmlTestAgent.extractFiringCountsFromPrintedOutput(
                "   455:  water-jug*elaborate*empty\r\n" + 
        		"   392:  water-jug*monitor*state\r\n" + 
        		"   279:  water-jug*propose*pour\r\n" + 
        		"   257:  water-jug*propose*empty\r\n" + 
        		"   252:  water-jug*propose*fill\r\n" + 
        		"   169:  water-jug*monitor*operator-application*fill\r\n" + 
        		"   169:  water-jug*apply*fill\r\n" + 
        		"   160:  water-jug*monitor*operator-application*empty\r\n" + 
        		"   160:  water-jug*apply*empty\r\n" + 
        		"   124:  water-jug*monitor*operator-application*pour\r\n" + 
        		"    36:  water-jug*apply*pour*not-empty-source\r\n" + 
        		"    26:  water-jug*apply*pour*empty-source\r\n" + 
        		"     1:  water-jug*detect*goal*achieved\r\n" + 
        		"     1:  water-jug*propose*initialize-water-jug\r\n" + 
        		"     1:  water-jug*apply*initialize-water-jug\r\n" + 
        		"");
        
        assertNotNull(fc);
        assertEquals(455L, fc.getCount("water-jug*elaborate*empty").longValue());
        assertEquals(392L, fc.getCount("water-jug*monitor*state").longValue());
        assertEquals(279L, fc.getCount("water-jug*propose*pour").longValue());
        assertEquals(257L, fc.getCount("water-jug*propose*empty").longValue());
        assertEquals(252L, fc.getCount("water-jug*propose*fill").longValue());
        assertEquals(169L, fc.getCount("water-jug*monitor*operator-application*fill").longValue());
        assertEquals(160L, fc.getCount("water-jug*monitor*operator-application*empty").longValue());
        assertEquals(160L, fc.getCount("water-jug*apply*empty").longValue());
        assertEquals(124L, fc.getCount("water-jug*monitor*operator-application*pour").longValue());
        assertEquals(36L, fc.getCount("water-jug*apply*pour*not-empty-source").longValue());
        assertEquals(26L, fc.getCount("water-jug*apply*pour*empty-source").longValue());
        assertEquals(1L, fc.getCount("water-jug*detect*goal*achieved").longValue());
        assertEquals(1L, fc.getCount("water-jug*propose*initialize-water-jug").longValue());
        assertEquals(1L, fc.getCount("water-jug*apply*initialize-water-jug").longValue());
    }
}

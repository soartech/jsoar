/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 24, 2009
 */
package org.jsoar.legilimens.trace;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jsoar.kernel.Agent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class AgentTraceBufferTest
{
    private Agent agent;
    private AgentTraceBuffer traceBuffer;
    
    @BeforeEach
    public void setUp() throws Exception
    {
        agent = new Agent();
        traceBuffer = AgentTraceBuffer.attach(agent, 16);
    }
    
    @AfterEach
    public void tearDown() throws Exception
    {
        traceBuffer.detach();
        traceBuffer.getTraceFile().delete();
        
        agent.dispose();
    }
    
    @Test
    public void testRingBufferIsHitWhenRecentTraceIsRequested() throws Exception
    {
        assertEquals(0, traceBuffer.getTraceLength());
        assertEquals(0, traceBuffer.getPermBufferAccesses());
        assertEquals(0, traceBuffer.getRingBufferAccesses());
        
        agent.getPrinter().print("12345678").flush();
        assertEquals(8, traceBuffer.getTraceLength());
        
        final TraceRange range0 = traceBuffer.getRange(0, -1);
        assertEquals(0, range0.getStart());
        assertEquals(8, range0.getLength());
        assertEquals("12345678", new String(range0.getData()));
        assertEquals(1, traceBuffer.getRingBufferAccesses());
        assertEquals(0, traceBuffer.getPermBufferAccesses());
    }
    
    @Test
    public void testPermBufferIsHitWhenOlderTraceIsRequested() throws Exception
    {
        final String text = "123456789abcdefghijklmnop"; // larger than 16 above
        agent.getPrinter().print(text).flush();
        assertEquals(text.length(), traceBuffer.getTraceLength());
        
        final TraceRange range0 = traceBuffer.getRange(0, -1);
        assertEquals(0, range0.getStart());
        assertEquals(text.length(), range0.getLength());
        assertEquals(text, new String(range0.getData()));
        assertEquals(0, traceBuffer.getRingBufferAccesses());
        assertEquals(1, traceBuffer.getPermBufferAccesses());
        
        final TraceRange range1 = traceBuffer.getRange(4, 8);
        assertEquals(4, range1.getStart());
        assertEquals(8, range1.getLength());
        assertEquals(text.substring(4, 4 + 8), new String(range1.getData()));
        assertEquals(0, traceBuffer.getRingBufferAccesses());
        assertEquals(2, traceBuffer.getPermBufferAccesses());
        
    }
}

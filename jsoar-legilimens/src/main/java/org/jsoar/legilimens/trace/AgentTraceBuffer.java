/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 14, 2009
 */
package org.jsoar.legilimens.trace;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.properties.PropertyKey;
import org.jsoar.util.properties.PropertyProvider;

/**
 * This object manages a buffer of the trace of the agent in order to efficiently
 * support the trace in the Legilimens web app.
 * 
 * <p>This object installs a persistent writer with the agent's {@link Printer}
 * object. As data is written to the trace by the printer, two actual buffers are
 * maintained. First, the trace is logged to file in a background thread. Second,
 * a small ring buffer with the last nK of trace data is maintained. When the
 * web app requests part of the trace, first the ring buffer is checked. If the
 * data is there, it is returned. Otherwise, the data is retrieved from the file.
 * This optimizes for the case where a polling webapp will typically only be asking
 * for very recent data rather than the entire trace.
 * 
 * @author ray
 */
public class AgentTraceBuffer
{
    private static final Logger logger = LoggerFactory.getLogger(AgentTraceBuffer.class);
    
    public static final PropertyKey<AgentTraceBuffer> KEY = PropertyKey.builder("legilimens.trace", AgentTraceBuffer.class).readonly(true).build();
    
    private final Agent agent;
    private final RingBuffer ringBuffer;
    private final FileBuffer fileBuffer;
    
    private AtomicInteger ringBufferAccesses = new AtomicInteger();
    private AtomicInteger permBufferAccesses = new AtomicInteger();
    
    public static AgentTraceBuffer attach(Agent agent, int bufferSize) throws IOException
    {
        return new AgentTraceBuffer(agent, bufferSize);
    }
    
    public static AgentTraceBuffer attach(Agent agent) throws IOException
    {
        return new AgentTraceBuffer(agent, getDefaultBufferSize());
    }
    
    private static int getDefaultBufferSize()
    {
        final String prop = System.getProperty("jsoar.legilimens.trace.size");
        
        return prop != null ? Integer.parseInt(prop) : 256 * 1024;
    }
    
    private static boolean getDeleteTraceOnExit()
    {
        return Boolean.valueOf(System.getProperty("jsoar.legilimens.trace.deleteOnExit", "true")).booleanValue();
    }
    
    private AgentTraceBuffer(Agent agent, int bufferSize) throws IOException
    {
        this.agent = agent;
        this.ringBuffer = new RingBuffer(bufferSize);
        this.fileBuffer = new FileBuffer(agent.getName(), this.ringBuffer);
        if(getDeleteTraceOnExit())
        {
            this.fileBuffer.getFile().deleteOnExit();
        }
        logger.info("Attaching trace buffer to agent '" + agent + "' with ring buffer size " + bufferSize + " and perm buffer " + this.fileBuffer.getFile());
        
        agent.getPrinter().addPersistentWriter(fileBuffer);
        agent.getProperties().setProvider(KEY, new PropertyProvider<AgentTraceBuffer>()
        {
            @Override
            public AgentTraceBuffer get()
            {
                return AgentTraceBuffer.this;
            }

            @Override
            public AgentTraceBuffer set(AgentTraceBuffer value)
            {
                return null;
            }
        });
    }
       
    
    public void detach() throws IOException
    {
        // TODO remove from agent properties
        
        agent.getPrinter().removePersistentWriter(fileBuffer);
        
        fileBuffer.close();
    }
    /**
     * Returns up to the last {@code limit} characters of the trace.
     * 
     * @param max maximum number of characters, or -1 for the entire trace
     * @return the range of characters
     * @throws IOException 
     */
    public TraceRange getTail(int max) throws IOException
    {
        synchronized(ringBuffer)
        {
            if(max < 0)
            {
                max = getTraceLength();
            }
            max = Math.min(max, getTraceLength());
            
            return getRange(getTraceLength() - max, max);
        }
    }
    
    /**
     * Returns a range of the agent's trace from the given offset.
     * 
     * @param start since the desired starting offset in the trace
     * @param max the maximum number of characters to return, or -1 for
     *      no limit.
     * @return the range of characters
     * @throws IOException 
     */
    public TraceRange getRange(int start, int max) throws IOException
    {
        synchronized(ringBuffer)
        {
            final int traceLength = getTraceLength();
            
            // If entire range is in the ring buffer...
            if(start > traceLength)
            {
                logger.error("Request for trace offset " + start + " which is beyond end of trace " + traceLength);
                return new TraceRange(traceLength, new char[] {});
            }
            
            if(max < 0)
            {
                max = traceLength;
            }
            final int lengthToEndOfTrace = traceLength - start;
            max = Math.min(max, lengthToEndOfTrace);
            
            if(lengthToEndOfTrace <= ringBuffer.size())
            {
                logger.info("Retrieving last " + lengthToEndOfTrace + " chars from ring buffer");
                ringBufferAccesses.incrementAndGet();
                final char[] data = ringBuffer.getTail(lengthToEndOfTrace, max);
                return new TraceRange(start, data);
            }
        }
        
        // Fall back to permanent buffer. This is purposefully outside of the synchronized
        // block so we don't stop the agent while we do the potentially slow operation of
        // reading from the file. We're relying on the file system's thread-safety here.
        logger.info("Retrieving " + start + " to " + (start + max) + " from permanent buffer");
        permBufferAccesses.incrementAndGet();
        return fileBuffer.getRange(start, max);
    }
    
    public File getTraceFile()
    {
        return fileBuffer.getFile();
    }

    /**
     * @return the traceLength
     */
    public int getTraceLength()
    {
        return fileBuffer.getLength();
    }

    /**
     * @return the ringBufferAccesses
     */
    public int getRingBufferAccesses()
    {
        return ringBufferAccesses.get();
    }

    /**
     * @return the permBufferAccesses
     */
    public int getPermBufferAccesses()
    {
        return permBufferAccesses.get();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        int total = ringBufferAccesses.get() + permBufferAccesses.get(); 
        
        return String.format("%d characters. " + 
             "%d requests, " +
             "%d ring buffer, %d perm buffer." +
            " Ring buffer hit rate = %8.0f%%. " +
            " Ring buffer size = %d." +
            "Perm buffer: %s.", 
            getTraceLength(), total, ringBufferAccesses.get(), permBufferAccesses.get(), 
            (ringBufferAccesses.get() / (double) total) * 100.0,
            ringBuffer.size(), 
            fileBuffer.getFile());
    }

}

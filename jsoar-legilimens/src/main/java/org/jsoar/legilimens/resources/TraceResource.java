/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 14, 2009
 */
package org.jsoar.legilimens.resources;

import java.io.IOException;
import java.nio.CharBuffer;

import org.jsoar.legilimens.RestletTools;
import org.jsoar.legilimens.trace.AgentTraceBuffer;
import org.jsoar.legilimens.trace.TraceRange;
import org.jsoar.util.FileTools;
import org.restlet.data.Disposition;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * @author ray
 */
public class TraceResource extends BaseAgentResource
{
    private int tail;
    private int start;
    private int max;
    
    /**
     * 
     */
    public TraceResource()
    {
    }
    
    

    /* (non-Javadoc)
     * @see org.jsoar.legilimens.resources.BaseAgentResource#doInit()
     */
    @Override
    protected void doInit() throws ResourceException
    {
        super.doInit();
        
        final String tailString = getQuery().getFirstValue("tail");
        tail = tailString != null ? Integer.parseInt(tailString) : -1;

        final String startString = getQuery().getFirstValue("start");
        start = startString != null ? Integer.parseInt(startString) : 0;
        
        final String maxString = getQuery().getFirstValue("max");
        max = maxString != null ? Integer.parseInt(maxString) : -1;
    }


    @Get("txt")
    public Representation getTextRepresentation()
    {
        final AgentTraceBuffer state = agent.getProperties().get(AgentTraceBuffer.KEY);
        try
        {
            final TraceRange traceRange = getTraceRange(state);
            
            final StringRepresentation rep = new StringRepresentation(CharBuffer.wrap(traceRange.getData(), 0, traceRange.getLength()));
            
            if(rep.getDisposition() == null) { rep.setDisposition(new Disposition()); }
            rep.getDisposition().setFilename(FileTools.replaceIllegalCharacters(agent.getName(), "_") + ".log");
            rep.getDisposition().setType(Disposition.TYPE_ATTACHMENT);
            
            RestletTools.setResponseHeader(getResponse(), "X-trace-start", traceRange.getStart());
            RestletTools.setResponseHeader(getResponse(), "X-trace-end", traceRange.getEnd());
            
            
            if(traceRange.getLength() == 0)
            {
                getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
            }
            
            return rep;
        }
        catch(IOException e)
        {
            getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, e, e.getMessage());
            return new StringRepresentation(e.getMessage());
        }
    }
    
    private TraceRange getTraceRange(AgentTraceBuffer state) throws IOException
    {
        if(tail >= 0)
        {
            return state.getTail(tail);
        }
        else
        {
            return state.getRange(start, max);
        }
    }
    
}

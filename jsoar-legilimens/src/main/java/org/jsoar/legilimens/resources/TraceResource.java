/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 14, 2009
 */
package org.jsoar.legilimens.resources;

import org.jsoar.legilimens.RestletTools;
import org.jsoar.legilimens.trace.AgentTraceState;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * @author ray
 */
public class TraceResource extends BaseAgentResource
{
    private int offset;
    
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
        
        final String offsetString = getQuery().getFirstValue("offset");
        offset = offsetString != null ? Integer.parseInt(offsetString) : 0;
    }


    @Get("txt")
    public Representation getTextRepresentation()
    {
        //agent.runFor(1, RunType.DECISIONS);
        final AgentTraceState state = agent.getProperties().get(AgentTraceState.KEY);
        final byte[] bytes = state.getBytes();
        
        int realOffset = Math.max(0, offset);
        realOffset = Math.min(bytes.length, realOffset);
        int realLength = bytes.length - realOffset;
        final StringRepresentation rep = new StringRepresentation(new String(bytes, realOffset, realLength));
        
        RestletTools.setResponseHeader(getResponse(), "X-total-bytes", realLength);
        
        return rep;
    }
    
}

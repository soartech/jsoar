/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens.resources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.memory.Wme;
import org.restlet.resource.ResourceException;

/**
 * @author ray
 */
public class WmesResource extends BaseAgentResource
{
    private String id;
    
    

    /* (non-Javadoc)
     * @see org.jsoar.legilimens.BaseAgentResource#doInit()
     */
    @Override
    protected void doInit() throws ResourceException
    {
        super.doInit();
        
        id = getQuery().getFirstValue("id");
    }
/*
    @Get("html")
    public Representation toHtml()
    {
        return html("wmes");
    }
*/
    /* (non-Javadoc)
     * @see org.jsoar.legilimens.resources.BaseAgentResource#setTemplateAttributes(java.util.Map)
     */
    @Override
    public void setTemplateAttributes(Map<String, Object> attrs)
    {
        super.setTemplateAttributes(attrs);
        
        attrs.put("filterId", id);
        final List<Wme> wmes = new ArrayList<Wme>();
        for(Wme wme : agent.getAgent().getAllWmesInRete())
        {
            if(id == null || id.equals(wme.getIdentifier().toString()))
            {
                wmes.add(wme);
            }
        }
        attrs.put("wmes", wmes);
    }
    
    
}

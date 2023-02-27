/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens.resources;

import java.util.Map;

import org.jsoar.kernel.memory.Wmes;
import org.restlet.resource.ResourceException;

/**
 * @author ray
 */
public class WmesResource extends BaseAgentResource
{
    private String id;
    private String attr;
    private String value;
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.legilimens.BaseAgentResource#doInit()
     */
    @Override
    protected void doInit() throws ResourceException
    {
        super.doInit();
        
        id = getFilterParam("id");
        attr = getFilterParam("attr");
        value = getFilterParam("value");
    }
    
    private String getFilterParam(String name)
    {
        final String filter = getQuery().getFirstValue(name);
        
        return filter != null ? filter : "*";
    }
    
    /*
     * @Get("html")
     * public Representation toHtml()
     * {
     * return html("wmes");
     * }
     */
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.legilimens.resources.BaseAgentResource#setTemplateAttributes(java.util.Map)
     */
    @Override
    public void setTemplateAttributes(Map<String, Object> attrs)
    {
        super.setTemplateAttributes(attrs);
        
        attrs.put("filterId", id != null ? id : "*");
        attrs.put("filterAttr", attr != null ? attr : "*");
        attrs.put("filterValue", value != null ? value : "*");
        
        attrs.put("wmes", Wmes.search(agent.getAgent(), id, attr, value));
    }
    
}

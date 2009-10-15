/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens.resources;

import java.io.StringWriter;
import java.util.Map;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.tracing.Printer;
import org.restlet.resource.ResourceException;

/**
 * @author ray
 */
public class ProductionResource extends BaseAgentResource
{
    private String name;
    
    
    /* (non-Javadoc)
     * @see org.jsoar.legilimens.resources.BaseAgentResource#doInit()
     */
    @Override
    protected void doInit() throws ResourceException
    {
        super.doInit();
        
        name = getRequest().getAttributes().get("productionName").toString();
        
        setExisting(agent.getProductions().getProduction(name) != null);
    }

    /* (non-Javadoc)
     * @see org.jsoar.legilimens.resources.BaseAgentResource#setTemplateAttributes(java.util.Map)
     */
    @Override
    public void setTemplateAttributes(Map<String, Object> attrs)
    {
        super.setTemplateAttributes(attrs);
        attrs.put("production", new Wrapper(agent.getProductions().getProduction(name)));
    }
    
    public static class Wrapper
    {
        public final Production production;
        public final String name;
        public final String code;
        
        public Wrapper(Production rule)
        {
            this.production = rule;
            
            this.name = rule.getName().toString();
            final StringWriter writer = new StringWriter();
            final Printer printer = new Printer(writer, true);
            this.production.print(printer, false);
            printer.flush();
            this.code = writer.toString();
        }
    }
}

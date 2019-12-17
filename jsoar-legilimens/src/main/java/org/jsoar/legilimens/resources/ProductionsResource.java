/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens.resources;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.FileTools;
import org.restlet.data.Disposition;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ResourceException;

/**
 * @author ray
 */
public class ProductionsResource extends BaseAgentResource
{
    private boolean internal;
    
    
    /* (non-Javadoc)
     * @see org.jsoar.legilimens.resources.BaseAgentResource#doInit()
     */
    @Override
    protected void doInit() throws ResourceException
    {
        super.doInit();
        
        internal = Boolean.valueOf(getQuery().getFirstValue("internal"));
    }

    /* (non-Javadoc)
     * @see org.jsoar.legilimens.resources.BaseAgentResource#setTemplateAttributes(java.util.Map)
     */
    @Override
    public void setTemplateAttributes(Map<String, Object> attrs)
    {
        super.setTemplateAttributes(attrs);

        final List<Production> rules = new ArrayList<Production>();
        for(Production p : agent.getProductions().getProductions(null))
        {
            rules.add(p);
        }
        Collections.sort(rules, new Comparator<Production>()
        {
            @Override
            public int compare(Production o1, Production o2)
            {
                return o1.getName().toString().compareToIgnoreCase(o2.getName().toString());
            }
        });
        attrs.put("productions", rules);
    }
 
    @Get("txt")
    public Representation getTextRepresentation()
    {
        final Callable<String> callable = new Callable<String>()
        {
            @Override
            public String call() throws Exception
            {
                final StringWriter writer = new StringWriter();
                final Printer printer = new Printer(writer);
                printer.print("# Generated from JSoar agent at %s\n", new Date());
                printer.print("# internal = %s\n", internal);
                for(Production p : agent.getProductions().getProductions(null))
                {
                    p.print(printer, internal);
                    printer.startNewLine();
                }
                printer.flush();
                return writer.toString();
            }
        };
        
        final String result = executeCallable(callable);
        if(result == null)
        {
            return null;
        }
        final StringRepresentation rep = new StringRepresentation(result, MediaType.TEXT_PLAIN);
        Disposition disposition = new Disposition(Disposition.TYPE_ATTACHMENT);
        disposition.setFilename(FileTools.replaceIllegalCharacters(agent.getName(), "_") + ".soar");
        rep.setDisposition(disposition);
        return rep;
    }
}

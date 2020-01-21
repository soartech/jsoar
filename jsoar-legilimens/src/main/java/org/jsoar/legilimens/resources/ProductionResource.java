/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens.resources;

import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.Callable;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.tracing.Printer;
import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.resource.Delete;
import org.restlet.resource.Post;
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
        
        name = getPathAttribute("productionName");
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
    
    @Post()
    public Representation postTextRepresentation(Representation entity)
    {
        final Form form = new Form(entity);
        final String production = form.getFirstValue("production");
        
        final Callable<String> callable = new Callable<String>()
        {
            @Override
            public String call() throws Exception
            {
                //agent.getPrinter().startNewLine().print(agent.getName() + "> " + command + "\n");
                return agent.getInterpreter().eval(production);
            }
        };
        executeCallable(callable);
        
        return getHtmlRepresentation();
    }
    
    @Delete()
    public void deleteProduction()
    {
        final Callable<Void> callable = new Callable<Void>()
        {

            @Override
            public Void call() throws Exception
            {
                final Production p = agent.getProductions().getProduction(name);
                if(p != null)
                {
                    agent.getProductions().exciseProduction(p, true);
                }
                return null;
            }
        };
        executeCallable(callable);
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
            final Printer printer = new Printer(writer);
            this.production.print(printer, false);
            printer.flush();
            this.code = writer.toString();
        }

        /**
         * @return the production
         */
        public Production getProduction()
        {
            return production;
        }

        /**
         * @return the name
         */
        public String getName()
        {
            return name;
        }

        /**
         * @return the code
         */
        public String getCode()
        {
            return code;
        }
        
        
    }
}

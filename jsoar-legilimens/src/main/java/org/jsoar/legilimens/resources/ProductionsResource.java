/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 9, 2009
 */
package org.jsoar.legilimens.resources;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.tracing.Printer;

/**
 * @author ray
 */
public class ProductionsResource extends BaseAgentResource
{
    private String id;
    
        /* (non-Javadoc)
     * @see org.jsoar.legilimens.resources.BaseAgentResource#setTemplateAttributes(java.util.Map)
     */
    @Override
    public void setTemplateAttributes(Map<String, Object> attrs)
    {
        super.setTemplateAttributes(attrs);

        final List<Wrapper> rules = new ArrayList<Wrapper>();
        for(Production p : agent.getProductions().getProductions(null))
        {
            rules.add(new Wrapper(p));
        }
        attrs.put("productions", rules);
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

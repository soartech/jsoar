/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 18, 2009
 */
package org.jsoar.legilimens.templates;

import java.util.List;
import java.util.Map;

import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.properties.PropertyKey;

import freemarker.ext.beans.BeanModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * @author ray
 */
public class TemplateMethods
{
    private static class IsIdentifierMethod implements TemplateMethodModelEx
    {
        /* (non-Javadoc)
         * @see freemarker.template.TemplateMethodModel#exec(java.util.List)
         */
        @Override
        public Object exec(List args) throws TemplateModelException
        {
            final Symbol symbol = (Symbol) ((BeanModel) args.get(0)).getWrappedObject(); 
            return symbol.asIdentifier() != null;
        }
    }
    
    private static class GetPropertyMethod implements TemplateMethodModelEx
    {
        /* (non-Javadoc)
         * @see freemarker.template.TemplateMethodModel#exec(java.util.List)
         */
        @Override
        public Object exec(List args) throws TemplateModelException
        {
            final ThreadedAgent agent = (ThreadedAgent) ((BeanModel) args.get(0)).getWrappedObject();
            final String name = args.get(1).toString();
            
            for(PropertyKey<?> key : agent.getProperties().getKeys())
            {
                if(name.equals(key.getName()))
                {
                    return agent.getProperties().get(key);
                }
            }
            return null;
        }
    }
    
    
    public static void installMethods(Map<String, Object> map)
    {
        map.put("isIdentifier", new IsIdentifierMethod());
        map.put("agent_property", new GetPropertyMethod());
    }
}

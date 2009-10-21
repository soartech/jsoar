/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 18, 2009
 */
package org.jsoar.legilimens.templates;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.symbols.Symbol;

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
    
    private static class CollectionSizeMethod implements TemplateMethodModelEx
    {
        /* (non-Javadoc)
         * @see freemarker.template.TemplateMethodModel#exec(java.util.List)
         */
        @Override
        public Object exec(List args) throws TemplateModelException
        {
            final Collection<?> c = (Collection<?>) ((BeanModel) args.get(0)).getWrappedObject(); 
            return c.size();
        }
    }
    
    
    
    public static void installMethods(Map<String, Object> map)
    {
        map.put("isIdentifier", new IsIdentifierMethod());
        map.put("collectionSize", new CollectionSizeMethod());
    }
}

/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 19, 2009
 */
package org.jsoar.kernel.io.beans;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.IDN;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtils;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbols;

/**
 * @author ray
 */
public class WorkingMemoryToBean
{
    private BeanUtilsBean util = new BeanUtilsBean();
    
    private static class PropertyInfo
    {
        final Class<?> klass;

        public PropertyInfo(Class<?> klass)
        {
            this.klass = klass;
        }
    }
    
    private final Map<Class<?>, Map<String, PropertyInfo>> infos = new HashMap<Class<?>, Map<String,PropertyInfo>>();
    
    public <T> T read(Identifier id, Class<T> klass) throws SoarBeanException
    {
        try
        {
            final T bean = klass.newInstance();
            final Iterator<Wme> it = id.getWmes();
            final Map<Object, Object> map = new HashMap<Object, Object>();
            while(it.hasNext())
            {
                final Wme wme = it.next();
                final String prop = getPropertyName(wme);
                final Object value = getPropertyValue(wme, bean, prop);
                
                putProperty(bean, prop, value, map);
            }
            util.populate(bean, map);
            return bean;
        }
        catch (InstantiationException e)
        {
            throw new SoarBeanException(e);
        }
        catch (IllegalAccessException e)
        {
            throw new SoarBeanException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new SoarBeanException(e);
        }
    }
    
    private Class<?> getPropertyType(Object bean, String name) throws SoarBeanException
    {
        try
        {
            final PropertyDescriptor desc = util.getPropertyUtils().getPropertyDescriptor(bean, name);
            return desc != null ? desc.getPropertyType() : null;
        }
        catch (IllegalAccessException e)
        {
            throw new SoarBeanException(e);
        }
        catch (InvocationTargetException e)
        {
            throw new SoarBeanException(e);
        }
        catch (NoSuchMethodException e)
        {
            throw new SoarBeanException(e);
        }
        
    }
    private Object getPropertyValue(Wme wme, Object bean, String name) throws SoarBeanException
    {
        Object value = Symbols.valueOf(wme.getValue());
        if(value instanceof Identifier)
        {
            final Identifier id = (Identifier) value;
            final Class<?> type = getPropertyType(bean, name);
            return read(id, type);
        }
        else
        {
            return value;
        }
        
    }
    private String getPropertyName(Wme wme)
    {
        return wme.getAttribute().toString();
    }
    
    private <T> void putProperty(T bean, String name, Object value, Map<Object, Object> map)
    {
        map.put(name, value);
    }
    
}

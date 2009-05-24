/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 19, 2009
 */
package org.jsoar.kernel.io.beans;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbols;

import com.google.common.collect.Iterators;

/**
 * Implements conversion from Soar working memory (e.g. an output command
 * structure) to Java beans.
 * 
 * <p>A single instance of this class can and should be used for multiple
 * deserializations. This will allow for effective property caching
 * across calls.
 * 
 * <p>Suppose we are given the following Java classes:
 * 
 * <pre>{@code
 * class Person
 * {
 *    public String name;
 *    public int age;
 *    public Address address;
 * }
 * 
 * class Address
 * {
 *    public String street;
 *    public String city;
 *    public String state;
 *    public int zip;
 * }
 * }</pre>
 * 
 * and the following Soar production that creates an output command:
 *
 * <pre>
 * {@code
 * sp {testBean
 *    state &lt;s> ^superstate nil ^io.output-link &lt;ol>)
 * -->
 *    (&lt;ol> ^person &lt;p>)
 *    (&lt;p> ^name |Don Lockwood| ^age 41 ^address &lt;a>)
 *    (&lt;a> ^street |123 Main| ^city |Hollywood| ^state |CA| ^zip 90221)
 * }
 * </pre>
 * 
 * then, supposing you caught this output command and had the identifier {@code <p>}:
 * 
 * <pre>{@code
 *    final SoarBeanReader reader = new SoarBeanReader();
 *    
 *    final Identifier p = ...;
 *    final Person person = reader.read(p, Person.class);
 *    
 *    // now use person as normal...
 * }
 * </pre>
 *  
 *  
 * <p>Limitations:
 * <ul>
 * <li>No support for Java collections
 * </ul>
 * 
 * @author ray
 */
public class SoarBeanReader
{
    private BeanUtilsBean util = new BeanUtilsBean();
    
    /**
     * Read the working memory structure under the given identifier into
     * a JavaBean (mostly) of the given type.
     * 
     * @param <T> The type of object
     * @param id The root identifier to read from
     * @param klass The class of the object to return
     * @return An object initialized from working memory
     * @throws SoarBeanException
     */
    public <T> T read(Identifier id, Class<T> klass) throws SoarBeanException
    {
        try
        {
            final T bean = klass.newInstance();
            for(final Iterator<Wme> it = id.getWmes(); it.hasNext();)
            {
                final Wme wme = it.next();
                final String name = getPropertyName(wme);
                setProperty(bean, name, Symbols.valueOf(wme.getValue()));
            }
            return bean;
        }
        catch (InstantiationException e)
        {
            throw makeException(e);
        }
        catch (IllegalAccessException e)
        {
            throw makeException(e);
        }
        catch (InvocationTargetException e)
        {
            throw makeException(e);
        }
        catch (NoSuchMethodException e)
        {
            throw makeException(e);
        }
        catch (SecurityException e)
        {
            throw makeException(e);
        }
        catch (IllegalArgumentException e)
        {
            throw makeException(e);
        }
    }

    private SoarBeanException makeException(Exception cause)
    {
        return new SoarBeanException(cause.getMessage(), cause);
    }
    
    private String getPropertyName(Wme wme)
    {
        return wme.getAttribute().toString();
    }
        
    private <T> void setProperty(T bean, String name, Object value) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SoarBeanException, SecurityException, IllegalArgumentException
    {
        final PropertyDescriptor desc = util.getPropertyUtils().getPropertyDescriptor(bean, name);
        if(desc == null)
        {
            setField(bean, name, value);
            return;
        }
        
        final Method writer = desc.getWriteMethod();
        if(writer == null)
        {
            setField(bean, name, value);
            return;
        }
        
        final Class<?> type = getTargetType(desc.getPropertyType());
        final Object convertedValue = convert(value, type);
        
        writer.invoke(bean, convertedValue);
    }
    
    private <T> void setField(T bean, String name, Object value) throws SecurityException, SoarBeanException, IllegalArgumentException, IllegalAccessException
    {
        final Class<?> beanClass = bean.getClass();
        try
        {
            final Field field = beanClass.getField(name);
            
            final Class<?> type = getTargetType(field.getType());
            final Object convertedValue = convert(value, type);
            field.set(bean, convertedValue);
        }
        catch (NoSuchFieldException e)
        {
            return;
        }
    }
    
    private Class<?> getTargetType(Class<?> tempType)
    {
        return tempType;
    }
    
    private Object convert(Object value, Class<?> targetType) throws SoarBeanException
    {
        if(value instanceof Identifier)
        {
            final Identifier id = (Identifier) value;
            if(!targetType.isArray())
            {
                return read(id, targetType);
            }
            else
            {
                return convertArray(id, targetType);
            }
        }
        else
        {
            return util.getConvertUtils().convert(value, targetType);
        }
    }
    
    private Object convertArray(Identifier id, Class<?> arrayType) throws SoarBeanException
    {
        final Class<?> targetType = arrayType.getComponentType();
        final int count = Iterators.size(id.getWmes());
        final Object array = Array.newInstance(targetType, count);
        
        int i = 0;
        for(final Iterator<Wme> it = id.getWmes(); it.hasNext(); i++)
        {
            final Wme kid = it.next();
            final Object value = convert(Symbols.valueOf(kid.getValue()), targetType);
            Array.set(array, i, value);
        }
        
        return array;
    }
}

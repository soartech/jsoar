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
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.DoubleSymbol;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.JavaSymbol;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Symbol;
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
 *    state <s> ^superstate nil ^io.output-link &lt;ol>)
 * -->
 *    (<ol> ^person &lt;p>)
 *    (<p> ^name |Don Lockwood| ^age 41 ^address &lt;a>)
 *    (<a> ^street |123 Main| ^city |Hollywood| ^state |CA| ^zip 90221)
 * }
 * }</pre>
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
 * <p>Other features:
 * <ul>
 * <li>Soar-style attributes, i.e. those with hyphens or stars, are automatically 
 * converted to Java naming conventions. For example, {@code first-name} and {@code first*name}
 * both become {@code firstName}
 * <li>A property whose type is a sub-type of {@link Symbol} will just get the raw
 * {@link Symbol} value of a WME with no conversions. So Soar {@link Identifier}s can be
 * captured in beans.
 * </ul>
 *  
 * <p>Limitations:
 * <ul>
 * <li>No support for Java collections
 * </ul>
 * 
 * @author ray
 * @see SoarBeanOutputManager
 */
public class SoarBeanReader
{
    private static final Logger logger = LoggerFactory.getLogger(SoarBeanReader.class);
    
    private final BeanUtilsBean util = new BeanUtilsBean();
    private final Map<Identifier, Object> beanMap = new HashMap<Identifier, Object>();
    
    // these are only for debugging purposes
    private Identifier debugId = null;
    private Class<?> debugSoarBeanClass = null;
    
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
        debugSoarBeanClass = klass;
        debugId = id;
        
        try
        {
            return readInternal(id, klass);
        }
        finally
        {
            beanMap.clear();
        }
    }
    
    private <T> T readInternal(Identifier id, Class<T> klass) throws SoarBeanException
    {
        // First check whether we've seen this id before...
        final Object cached = beanMap.get(id);
        if(cached != null)
        {
            return klass.cast(cached);
        }
        
        try
        {
            final T bean = klass.newInstance();
            beanMap.put(id, bean); // remember id to avoid cycles
            for(final Iterator<Wme> it = id.getWmes(); it.hasNext();)
            {
                final Wme wme = it.next();
                final String name = getPropertyName(wme);
                setProperty(bean, name, wme.getValue());
            }
            return bean;
        }
        catch (InstantiationException e)
        {
            throw makeException(e, debugSoarBeanClass, debugId);
        }
        catch (IllegalAccessException e)
        {
            throw makeException(e, debugSoarBeanClass, debugId);
        }
        catch (InvocationTargetException e)
        {
            throw makeException(e, debugSoarBeanClass, debugId);
        }
        catch (NoSuchMethodException e)
        {
            throw makeException(e, debugSoarBeanClass, debugId);
        }
        catch (SecurityException e)
        {
            throw makeException(e, debugSoarBeanClass, debugId);
        }
        catch (IllegalArgumentException e)
        {
            throw makeException(e, debugSoarBeanClass, debugId);
        }
    }

    private <T> SoarBeanException makeException(Exception cause, Class<?> klass, Identifier id)
    {
        return new SoarBeanException(cause.getMessage(), cause, klass, id);
    }
    
    private String getPropertyName(Wme wme)
    {
        final String name = wme.getAttribute().toString();
        int hyphen = name.indexOf('-');
        int star = name.indexOf('*');
        if(hyphen == -1 && star == -1)
        {
            return name;
        }
        final StringBuilder result = new StringBuilder();
        boolean skipped = false;
        for(int i = 0; i < name.length(); i++)
        {
            char c = name.charAt(i);
            if(c == '-' || c == '*')
            {
                skipped = true;
            }
            else if(skipped)
            {
                result.append(result.length() != 0 ? Character.toUpperCase(c) : c);
                skipped = false;
            }
            else
            {
                result.append(c);
            }
        }
        return result.toString();
    }
        
    private <T> void setProperty(T bean, String name, Symbol value) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, SoarBeanException, SecurityException, IllegalArgumentException
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
    
    private <T> void setField(T bean, String name, Symbol value) throws SecurityException, SoarBeanException, IllegalArgumentException, IllegalAccessException
    {
        final Class<?> beanClass = bean.getClass();
        try
        {
            final Field field = beanClass.getField(name);
            final int modifiers = field.getModifiers();
            if(Modifier.isStatic(modifiers))
            {
                logger.warn("SoarBean field " + beanClass.getCanonicalName() + "." + name + " is static. Ignoring.");
                return;
            }
            else if(Modifier.isPrivate(modifiers))
            {
                logger.warn("SoarBean field " + beanClass.getCanonicalName() + "." + name + " is private. Ignoring.");
                return;
            }
            final Class<?> type = getTargetType(field.getType());
            final Object convertedValue = convert(value, type);
            field.set(bean, convertedValue);
        }
        catch (NoSuchFieldException e)
        {
            logger.warn("Unknown property " + beanClass.getCanonicalName() + "." + name + ". Ignoring.");
            return;
        }
    }
    
    private Class<?> getTargetType(Class<?> tempType)
    {
        return tempType;
    }

    private <T> T trySymbolConversion(Symbol initialValue, T convertedValue, Class<T> symbolType) throws SoarBeanException
    {
        if(convertedValue == null)
        {
            throw new SoarBeanException("Can't convert '" + initialValue + "' to " + symbolType.getCanonicalName(), debugSoarBeanClass, debugId);
        }
        return convertedValue;
    }
    
    private Object convert(Symbol value, Class<?> targetType) throws SoarBeanException
    {
        if(targetType.isAssignableFrom(Symbol.class))
        {
            return value;
        }
        else if(targetType.isAssignableFrom(Identifier.class))
        {
            return trySymbolConversion(value, value.asIdentifier(), Identifier.class);
        }
        else if(targetType.isAssignableFrom(StringSymbol.class))
        {
            return trySymbolConversion(value, value.asString(), StringSymbol.class);
        }
        else if(targetType.isAssignableFrom(DoubleSymbol.class))
        {
            return trySymbolConversion(value, value.asDouble(), DoubleSymbol.class);
        }
        else if(targetType.isAssignableFrom(IntegerSymbol.class))
        {
            return trySymbolConversion(value, value.asInteger(), IntegerSymbol.class);
        }
        else if(targetType.isAssignableFrom(JavaSymbol.class))
        {
            final JavaSymbol s = value.asJava();
            if(s == null)
            {
                throw new SoarBeanException("Can't convert '" + value + "' to JavaSymbol", debugSoarBeanClass, debugId);
            }
            return util.getConvertUtils().convert(s.getValue(), targetType);
        }
        else if(value instanceof Identifier)
        {
            final Identifier id = (Identifier) value;
            if(!targetType.isArray())
            {
                return readInternal(id, targetType);
            }
            else
            {
                return convertArray(id, targetType);
            }
        }
        else
        {
            return util.getConvertUtils().convert(Symbols.valueOf(value), targetType);
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
            final Object value = convert(kid.getValue(), targetType);
            Array.set(array, i, value);
        }
        
        return array;
    }
}

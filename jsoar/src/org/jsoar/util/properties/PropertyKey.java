/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 18, 2008
 */
package org.jsoar.util.properties;


/**
 * A PropertyKey is a unique identifier for a property. It includes 
 * the name of the property, the type of its value and a default value.
 * A PropertyKey is immutable.
 * 
 * @author ray
 */
public class PropertyKey<T>
{
    private final String name;
    private final Class<T> type;
    private final T defValue;
    
    /**
     * Create a new property key.
     * 
     * @param <T> The value type
     * @param name The name of the property
     * @param type The class of the value type
     * @param defValue The default value of the property
     * @return new PropertyKey
     */
    public static <T> PropertyKey<T> create(String name, Class<T> type, T defValue)
    {
        return new PropertyKey<T>(name, type, defValue);
    }
    
    /**
     * @param name
     * @param type
     */
    private PropertyKey(String name, Class<T> type, T defValue)
    {
        this.name = name;
        this.type = type;
        this.defValue = defValue;
    }

    /**
     * @return the name of the property
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return the type of the property's value
     */
    public Class<T> getType()
    {
        return type;
    }

    /**
     * @return the default value of the property
     */
    public T getDefaultValue()
    {
        return defValue;
    }
}

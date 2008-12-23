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
    private final boolean boundable;
    
    public static class Builder<T>
    {
        private final String name;
        private final Class<T> type;
        private T defValue;
        private boolean boundable;
        
        private Builder(String name, Class<T> type)
        {
            this.name = name;
            this.type = type;
        }

        public T defaultValue() { return defValue; }
        public Builder<T> defaultValue(T defValue) { this.defValue = defValue; return this; }
        public boolean boundable() { return boundable; }
        public Builder<T> boundable(boolean boundable) { this.boundable = boundable; return this; }
        
        public PropertyKey<T> build()
        {
            return new PropertyKey<T>(this);
        }
    }
    
    public static <T> Builder<T> builder(String name, Class<T> type)
    {
        return new Builder<T>(name, type);
    }
    
   
    private PropertyKey(Builder<T> builder)
    {
        this.name = builder.name;
        this.type = builder.type;
        this.defValue = builder.defValue;
        this.boundable = builder.boundable;
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

    /**
     * Returns true if this property will fire events when its value changed.
     * For certain high-frequency events, change events are not practical. In
     * these cases, the property will return false for this method.
     * 
     * @return true if this property fires change events when its value changes
     */
    public boolean isBoundable()
    {
        return boundable;
    }
}

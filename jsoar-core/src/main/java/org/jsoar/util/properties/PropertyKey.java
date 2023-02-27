/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 18, 2008
 */
package org.jsoar.util.properties;

import java.util.Comparator;

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
    private final boolean readonly;
    
    /**
     * A comparator for sorting property keys by name
     */
    public static Comparator<PropertyKey<?>> NAME_COMPARATOR = new Comparator<>()
    {
        
        @Override
        public int compare(PropertyKey<?> o1, PropertyKey<?> o2)
        {
            return o1.name.compareTo(o2.name);
        }
    };
    
    public static class Builder<T>
    {
        private final String name;
        private final Class<T> type;
        private T defValue;
        private boolean boundable;
        private boolean readonly;
        
        private Builder(String name, Class<T> type)
        {
            this.name = name;
            this.type = type;
        }
        
        public T defaultValue()
        {
            return defValue;
        }
        
        public Builder<T> defaultValue(T defValue)
        {
            this.defValue = defValue;
            return this;
        }
        
        public boolean boundable()
        {
            return boundable;
        }
        
        public Builder<T> boundable(boolean boundable)
        {
            this.boundable = boundable;
            return this;
        }
        
        public boolean readonly()
        {
            return readonly;
        }
        
        public Builder<T> readonly(boolean readonly)
        {
            this.readonly = readonly;
            return this;
        }
        
        public PropertyKey<T> build()
        {
            return new PropertyKey<>(this);
        }
    }
    
    public static <T> Builder<T> builder(String name, Class<T> type)
    {
        return new Builder<>(name, type);
    }
    
    private PropertyKey(Builder<T> builder)
    {
        this.name = builder.name;
        this.type = builder.type;
        this.defValue = builder.defValue;
        this.boundable = builder.boundable;
        this.readonly = builder.readonly;
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
    
    /**
     * Returns true if this property is readonly. An exception will be thrown if
     * client code attempts to modify a readonly property. Note that the value of
     * the property may still change, just not externally.
     * 
     * @return true if the property is read only.
     */
    public boolean isReadonly()
    {
        return readonly;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return name;
    }
    
}

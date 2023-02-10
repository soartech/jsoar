/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 18, 2008
 */
package org.jsoar.kernel;

import org.jsoar.kernel.tracing.Trace.Category;

/**
 * Enumeration of various production types.
 * 
 * <p>gsysparam.h:43:_PRODUCTION_TYPE
 * 
 * @author ray
 */
public enum ProductionType
{
    /**
     * gsysparam.h:43:USER_PRODUCTION_TYPE
     */
    USER(Category.FIRINGS_OF_USER_PRODS),
    
    /**
     * gsysparam.h:44:DEFAULT_PRODUCTION_TYPE
     */
    DEFAULT(Category.FIRINGS_OF_DEFAULT_PRODS),
    
    /**
     * gsysparam.h:45:CHUNK_PRODUCTION_TYPE
     */
    CHUNK(Category.FIRINGS_OF_CHUNKS),
    
    /**
     * gsysparam.h:46:JUSTIFICATION_PRODUCTION_TYPE
     */
    JUSTIFICATION(Category.FIRINGS_OF_JUSTIFICATIONS),
    
    /**
     * gsysparam.h:47:TEMPLATE_PRODUCTION_TYPE
     */
    TEMPLATE(Category.FIRINGS_OF_TEMPLATES);
    
    // NUM_PRODUCTION_TYPES 5
    // Soar-RL assumes that the production types start at 0 and go to (NUM_PRODUCTION_TYPES-1) sequentially
    
    private final Category category;
    private final String displayString;
    
    private ProductionType(Category category)
    {
        this.category = category;
        
        this.displayString = name().charAt(0) + name().substring(1).toLowerCase();
    }
    
    /**
     * @return The trace category of this type of production
     */
    public Category getTraceCategory()
    {
        return category;
    }
    
    /**
     * @return A display string in title case
     */
    public String getDisplayString()
    {
        return displayString;
    }
}

/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 18, 2008
 */
package org.jsoar.kernel;

import org.jsoar.kernel.tracing.Trace.Category;

/**
 * @author ray
 */
public enum ProductionType
{
    USER_PRODUCTION_TYPE(Category.TRACE_FIRINGS_OF_USER_PRODS_SYSPARAM),
    DEFAULT_PRODUCTION_TYPE(Category.TRACE_FIRINGS_OF_DEFAULT_PRODS_SYSPARAM),
    CHUNK_PRODUCTION_TYPE(Category.TRACE_FIRINGS_OF_CHUNKS_SYSPARAM),
    JUSTIFICATION_PRODUCTION_TYPE(Category.TRACE_FIRINGS_OF_JUSTIFICATIONS_SYSPARAM),
    TEMPLATE_PRODUCTION_TYPE(Category.TRACE_FIRINGS_OF_TEMPLATES_SYSPARAM);

    //NUM_PRODUCTION_TYPES 5
    // Soar-RL assumes that the production types start at 0 and go to (NUM_PRODUCTION_TYPES-1) sequentially

    private final Category category;
    
    private ProductionType(Category category)
    {
        this.category = category;
    }
    
    public Category getTraceCategory()
    {
        return category;
    }
    
}

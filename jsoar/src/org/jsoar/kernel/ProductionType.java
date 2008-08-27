/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 18, 2008
 */
package org.jsoar.kernel;

/**
 * @author ray
 */
public enum ProductionType
{
    USER_PRODUCTION_TYPE,
    DEFAULT_PRODUCTION_TYPE,
    CHUNK_PRODUCTION_TYPE,
    JUSTIFICATION_PRODUCTION_TYPE,
    TEMPLATE_PRODUCTION_TYPE,

    //NUM_PRODUCTION_TYPES 5
    // Soar-RL assumes that the production types start at 0 and go to (NUM_PRODUCTION_TYPES-1) sequentially

}

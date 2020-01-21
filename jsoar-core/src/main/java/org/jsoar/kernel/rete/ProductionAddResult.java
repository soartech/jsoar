/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 1, 2008
 */
package org.jsoar.kernel.rete;

/**
 * @author ray
 */
public enum ProductionAddResult
{
    
    NO_REFRACTED_INST,              /* no refracted inst. was given */
    REFRACTED_INST_MATCHED,         /* there was a match for the inst. */
    REFRACTED_INST_DID_NOT_MATCH,   /* there was no match for it */
    DUPLICATE_PRODUCTION           /* the prod. was a duplicate */
    
}

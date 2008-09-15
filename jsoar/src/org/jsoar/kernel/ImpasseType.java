/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 7, 2008
 */
package org.jsoar.kernel;

/**
 * gdatastructs.h:234
 * 
 * @author ray
 */
public enum ImpasseType
{
    NONE_IMPASSE_TYPE,                   /* no impasse */
    CONSTRAINT_FAILURE_IMPASSE_TYPE,
    CONFLICT_IMPASSE_TYPE,
    TIE_IMPASSE_TYPE,
    NO_CHANGE_IMPASSE_TYPE,
    
    // more specific forms of no change impasse types
    // made negative to never conflict with impasse constants
    // reinforcement_learning.h:53
    STATE_NO_CHANGE_IMPASSE_TYPE, // -1
    OP_NO_CHANGE_IMPASSE_TYPE,    // -2

}

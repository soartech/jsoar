/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 7, 2008
 */
package org.jsoar.kernel;

/**
 * <p>gdatastructs.h:234
 * 
 * @author ray
 */
public enum ImpasseType
{
    /**
     * gdatastructs.h:234:NONE_IMPASSE_TYPE
     */
    NONE,                   /* no impasse */
    /**
     * gdatastructs.h:234:CONSTRAINT_FAILURE_IMPASSE_TYPE
     */
    CONSTRAINT_FAILURE,
    /**
     * gdatastructs.h:234:CONFLICT_IMPASSE_TYPE
     */
    CONFLICT,
    /**
     * gdatastructs.h:234:TIE_IMPASSE_TYPE
     */
    TIE,
    /**
     * gdatastructs.h:234:NO_CHANGE_IMPASSE_TYPE
     */
    NO_CHANGE,
    
    // more specific forms of no change impasse types
    // made negative to never conflict with impasse constants
    // reinforcement_learning.h:53
    /**
     * gdatastructs.h:234:STATE_NO_CHANGE_IMPASSE_TYPE
     */
    STATE_NO_CHANGE, // -1
    /**
     * gdatastructs.h:234:OP_NO_CHANGE_IMPASSE_TYPE
     */
    OP_NO_CHANGE,    // -2

}

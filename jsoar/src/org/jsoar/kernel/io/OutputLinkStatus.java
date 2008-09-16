/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 15, 2008
 */
package org.jsoar.kernel.io;

/**
 * io.cpp:387
 * 
 * @author ray
 */
public enum OutputLinkStatus
{
    NEW_OL_STATUS,                     /* just created it */
    UNCHANGED_OL_STATUS,             /* normal status */
    MODIFIED_BUT_SAME_TC_OL_STATUS, /* some value in its TC has been
                                                  modified, but the ids in its TC
                                                  are the same */
    MODIFIED_OL_STATUS,               /* the set of ids in its TC has
                                                  changed */
    REMOVED_OL_STATUS,                /* link has just been removed */

}

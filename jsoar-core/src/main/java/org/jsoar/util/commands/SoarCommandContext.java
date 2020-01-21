/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2010
 */
package org.jsoar.util.commands;

import org.jsoar.util.SourceLocation;

/**
 * Interface for a context object that is constructed and passed to commands
 * as they are executed. This allows the command to access information about
 * where it's executing to assist in error reporting, working directory
 * managements, etc.
 * 
 * @author ray
 */
public interface SoarCommandContext
{
    /**
     * @return the source location of the currently executing command
     */
    SourceLocation getSourceLocation();
}

/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2010
 */
package org.jsoar.util.commands;

import org.jsoar.util.DefaultSourceLocation;
import org.jsoar.util.SourceLocation;

/**
 * Default implementation of {@link SoarCommandContext}
 * @author ray
 */
public class DefaultSoarCommandContext implements SoarCommandContext
{
    private final SourceLocation sourceLocation;
    
    public static SoarCommandContext empty()
    {
        return new DefaultSoarCommandContext(DefaultSourceLocation.UNKNOWN);
    }

    public DefaultSoarCommandContext(SourceLocation sourceLocation)
    {
        this.sourceLocation = sourceLocation;
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommandContext#getSourceLocation()
     */
    @Override
    public SourceLocation getSourceLocation()
    {
        return sourceLocation;
    }
}

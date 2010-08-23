/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

/**
 * @author ray
 */
public final class OSupportModeCommand implements SoarCommand
{
    public OSupportModeCommand()
    {
    }

    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        throw new SoarException( 
                "o-support-mode command is not supported. " +
                "jsoar's default behavior is equivalent to o-support mode 4 in" +
                "the standard Soar 9.0.0 release.");
    }
}
/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

/**
 * Implementation of the "soar8" command.
 * 
 * @author ray
 */
public final class Soar8Command implements SoarCommand
{
    public Soar8Command()
    {
    }

    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        throw new SoarException(
                "soar8 command is not supported by jsoar. " +
                "jsoar does not support Soar 7 mode.");
    }
}
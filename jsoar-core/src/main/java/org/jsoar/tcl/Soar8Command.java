/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.tcl;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;

/**
 * @author ray
 */
final class Soar8Command implements SoarCommand
{
    Soar8Command()
    {
    }

    @Override
    public String execute(String[] args) throws SoarException
    {
        throw new SoarException(
                "soar8 command is not supported by jsoar. " +
                "jsoar does not support Soar 7 mode.");
    }
}
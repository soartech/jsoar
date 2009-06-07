/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 */
package org.jsoar.tcl;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;

/**
 * @author ray
 */
final class OSupportModeCommand implements SoarCommand
{
    OSupportModeCommand()
    {
    }

    @Override
    public String execute(String[] args) throws SoarException
    {
        throw new SoarException( 
                "o-support-mode command is not supported. " +
                "jsoar's default behavior is equivalent to o-support mode 4 in" +
                "the standard Soar 9.0.0 release.");
    }
}
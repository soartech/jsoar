/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 23, 2010
 */
package org.jsoar.soarunit;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import org.jsoar.kernel.SoarException;
import org.jsoar.util.commands.SoarCommand;

/**
 * @author ray
 */
public class SoarUnitCommand implements SoarCommand
{
    /* (non-Javadoc)
     * @see org.jsoar.util.commands.SoarCommand#execute(java.lang.String[])
     */
    @Override
    public String execute(String[] args) throws SoarException
    {
        final StringWriter writer = new StringWriter();
        final PrintWriter pw = new PrintWriter(writer);
        try
        {
            new SoarUnit(pw).run(Arrays.copyOfRange(args, 1, args.length));
        }
        catch (InterruptedException e)
        {
            throw new SoarException(e.getMessage(), e);
        }
        catch (IOException e)
        {
            throw new SoarException(e.getMessage(), e);
        }
        pw.flush();
        return writer.toString();
    }

}

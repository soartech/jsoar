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
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.commands.SoarCommandProvider;

/**
 * @author ray
 */
public class SoarUnitCommand implements SoarCommand
{
    public static class Provider implements SoarCommandProvider
    {

        /* (non-Javadoc)
         * @see org.jsoar.util.commands.SoarCommandProvider#registerCommands(org.jsoar.util.commands.SoarCommandInterpreter)
         */
        @Override
        public void registerCommands(SoarCommandInterpreter interp, Adaptable context)
        {
            interp.addCommand("soar-unit", new SoarUnitCommand());
        }
        
    }
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

/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 7, 2008
 */
package org.jsoar.kernel.commands;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.parser.Parser;
import org.jsoar.util.commands.SoarCommand;
import org.jsoar.util.commands.SoarCommandContext;

/**
 * Implementation of the "set-parser" command.
 * 
 * @author ray
 */
public class SetParserCommand implements SoarCommand
{
    private final Agent agent;
    
    public SetParserCommand(Agent agent)
    {
        this.agent = agent;
    }

    /* (non-Javadoc)
     * @see tcl.lang.Command#cmdProc(tcl.lang.Interp, tcl.lang.TclObject[])
     */
    @Override
    public String execute(SoarCommandContext commandContext, String[] args) throws SoarException
    {
        if(args.length != 2)
        {
            // TODO illegal arguments
            throw new SoarException(String.format("%s <class>", args[0]));
        }
        try
        {
            Class<?> klass = Class.forName(args[1].toString());
            Parser parser = (Parser) klass.newInstance();
            agent.getProductions().setParser(parser);
            return "";
        }
        catch (ClassNotFoundException e)
        {
            throw new SoarException(e.getMessage());
        }
        catch (InstantiationException e)
        {
            throw new SoarException(e.getMessage());
        }
        catch (IllegalAccessException e)
        {
            throw new SoarException(e.getMessage());
        }
        catch (ClassCastException e)
        {
            throw new SoarException(e.getMessage());
        }
    }

}

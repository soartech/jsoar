/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 7, 2008
 */
package org.jsoar.tcl;

import org.jsoar.kernel.parser.Parser;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
public class SetParserCommand implements Command
{
    private final SoarTclInterface ifc;
    
    SetParserCommand(SoarTclInterface ifc)
    {
        this.ifc = ifc;
    }

    /* (non-Javadoc)
     * @see tcl.lang.Command#cmdProc(tcl.lang.Interp, tcl.lang.TclObject[])
     */
    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        if(args.length != 2)
        {
            throw new TclNumArgsException(interp, 0, args, "set-parser <class>");
        }
        try
        {
            Class<?> klass = Class.forName(args[1].toString());
            Parser parser = (Parser) klass.newInstance();
            ifc.getAgent().getProductions().setParser(parser);
        }
        catch (ClassNotFoundException e)
        {
            throw new TclException(interp, e.getMessage());
        }
        catch (InstantiationException e)
        {
            throw new TclException(interp, e.getMessage());
        }
        catch (IllegalAccessException e)
        {
            throw new TclException(interp, e.getMessage());
        }
        catch (ClassCastException e)
        {
            throw new TclException(interp, e.getMessage());
        }
    }

}

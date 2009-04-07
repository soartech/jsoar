package org.jsoar.tcl;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.jsoar.util.TeeWriter;

import tcl.lang.Command;
import tcl.lang.Interp;
import tcl.lang.TclException;
import tcl.lang.TclNumArgsException;
import tcl.lang.TclObject;

/**
 * @author ray
 */
final class CLogCommand implements Command
{
    private static final List<String> offOptions = Arrays.asList("-c", "--close", "-o", "--off", "-d", "--disable");
    private static final List<String> queryOptions = Arrays.asList("-q", "--query");
    
    /**
     * 
     */
    private final SoarTclInterface ifc;
    private LinkedList<Writer> writerStack = new LinkedList<Writer>();

    /**
     * @param soarTclInterface
     */
    CLogCommand(SoarTclInterface soarTclInterface)
    {
        ifc = soarTclInterface;
    }

    @Override
    public void cmdProc(Interp interp, TclObject[] args) throws TclException
    {
        if(args.length == 2)
        {
            String arg = args[1].toString();
            if(offOptions.contains(arg))
            {
                if(writerStack.isEmpty())
                {
                    throw new TclException(interp, "Log stack is empty");
                }
                Writer w = writerStack.pop();
                ifc.getAgent().getPrinter().popWriter();
                if(w != null)
                {
                    try
                    {
                        w.close();
                    }
                    catch (IOException e)
                    {
                        throw new TclException(interp, "While closing writer: " + e.getMessage());
                    }
                }
                return;
            }
            else if(queryOptions.contains(arg))
            {
                interp.setResult(writerStack.isEmpty() ? "closed" : String.format("open (%d writers)", writerStack.size()));
                return;
            }
            else if(arg.equals("stdout"))
            {
                Writer w = new OutputStreamWriter(System.out);
                writerStack.push(null);
                ifc.getAgent().getPrinter().pushWriter(new TeeWriter(ifc.getAgent().getPrinter().getWriter(), w), true);
                return;
            }
            else if(arg.equals("stderr"))
            {
                Writer w = new OutputStreamWriter(System.err);
                writerStack.push(null);
                ifc.getAgent().getPrinter().pushWriter(new TeeWriter(ifc.getAgent().getPrinter().getWriter(), w), true);
                return;
            }
            else
            {
                try
                {
                    Writer w = new FileWriter(arg);
                    writerStack.push(w);
                    ifc.getAgent().getPrinter().pushWriter(new TeeWriter(ifc.getAgent().getPrinter().getWriter(), w), true);
                    return;
                }
                catch (IOException e)
                {
                    throw new TclException(interp, "Failed to open file '" + arg + "': " + e.getMessage());
                }
            }
        }
        // TODO: Implement -a, --add, -A, --append, -e, --existing
        
        throw new TclNumArgsException(interp, 0, args, "");
    }
}
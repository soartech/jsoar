/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 9, 2009
 */
package org.jsoar.debugger;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.rhs.functions.RhsFunctions;
import org.jsoar.kernel.symbols.Symbol;

/**
 * A right hand side function that asks the user for input and returns the
 * result as a string.
 * 
 * @author ray
 */
public class AcceptRhsFunction extends AbstractRhsFunctionHandler
{
    public AcceptRhsFunction()
    {
        super("accept", 0, 1);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
            throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);

        final String message = arguments.isEmpty() ? "Input requested by agent:" : arguments.get(0).toString();
        
        try
        {
            final String result = askUserForInput(context, message);
            return context.getSymbols().createString(result != null ? result : "");
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new RhsFunctionException("Interrupted waiting for user input");
        }
        catch (InvocationTargetException e)
        {
            throw new RhsFunctionException("Errot while waiting for user input: " + e.getMessage(), e);
        }
    }

    private String askUserForInput(final RhsFunctionContext context, final String message) throws InterruptedException, InvocationTargetException
    {
        if(SwingUtilities.isEventDispatchThread())
        {
            return askUserForInputFromEventThread(context, message);
        }
        else
        {
            final AtomicReference<String> result = new AtomicReference<String>();
            SwingUtilities.invokeAndWait(() -> result.set(askUserForInputFromEventThread(context, message)));
            return result.get();
        }
    }

    private String askUserForInputFromEventThread(RhsFunctionContext context, String message)
    {
        return JOptionPane.showInputDialog(null, message, 
                "JSoar - firing rule '" + context.getProductionBeingFired().getName() + "'",
                JOptionPane.QUESTION_MESSAGE);
    }
}

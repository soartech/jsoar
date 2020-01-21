/*
 * Copyright (c) 2010  Dave Ray <daveray@gmail.com>
 *
 * Created on June 5, 2010
 */
package org.jsoar.debugger.actions;

import java.awt.event.ActionEvent;
import java.util.concurrent.Callable;

import javax.swing.KeyStroke;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.commands.SoarCommandInterpreter;

/**
 * @author ray
 */
public class ReloadAction extends AbstractDebuggerAction
{
    private static final long serialVersionUID = -7639843952865259437L;
    
    private final boolean excise;
    
    public ReloadAction(ActionManager manager, boolean excise)
    {
        super(manager, excise ? "Excise All and Reload" : "Reload");
        
        this.excise = excise;
        
        setAcceleratorKey(excise ? KeyStroke.getKeyStroke("ctrl shift R") : KeyStroke.getKeyStroke("ctrl R"));
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.actions.AbstractDebuggerAction#update()
     */
    @Override
    public void update()
    {
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        final SoarCommandInterpreter interp = getApplication().getAgent().getInterpreter();
        getApplication().getAgent().execute(new Callable<Void>() {

            @Override
            public Void call()
            {
                final Printer p = getApplication().getAgent().getPrinter();
                try
                {
                    p.startNewLine();
                    if(excise)
                    {
                        interp.eval("excise --all");
                    }
                    final String result = interp.eval("source --reload");
                    p.startNewLine().print(result).flush();
                }
                catch (SoarException e)
                {
                    // TODO this is a little smelly.
                    p.error(e.getMessage());
                }
                
                return null;
            }}, getApplication().newUpdateCompleter(false));
        
    }

}

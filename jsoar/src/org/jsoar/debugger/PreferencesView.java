/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.flexdock.docking.DockingConstants;
import org.jsoar.debugger.selection.SelectionListener;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.commands.PrintPreferencesCommand;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.util.adaptables.Adaptables;

/**
 * Simple view that shows preferences for selected id or wme.
 * 
 * @author ray
 */
public class PreferencesView extends AbstractAdaptableView implements SelectionListener
{
    private static final long serialVersionUID = -5150761314645770374L;

    private final LittleDebugger debugger;
    private final SelectionManager selectionManager;
    private JTextArea textArea = new JTextArea();
    
    public PreferencesView(LittleDebugger debugger)
    {
        super("preferences", "Preferences");
        
        this.debugger = debugger;
        this.selectionManager = debugger.getSelectionManager();
        
        addAction(DockingConstants.PIN_ACTION);
        
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(textArea), BorderLayout.CENTER);
        
        setContentPane(p);

        this.selectionManager.addListener(this);
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.selection.SelectionListener#selectionChanged(org.jsoar.debugger.selection.SelectionManager)
     */
    @Override
    public void selectionChanged(SelectionManager manager)
    {
        textArea.setText(getPreferencesOutput(manager.getSelection()));
        textArea.setCaretPosition(0);
    }
    
    private String getPreferencesOutput(final List<Object> selection)
    {
        Callable<String> matchCall = new Callable<String>() {

            @Override
            public String call() throws Exception
            {
                return safeGetPreferencesOutput(selection);
            }};
        return debugger.getAgentProxy().execute(matchCall);
    }
        
    private String safeGetPreferencesOutput(List<Object> selection)
    {
        final Agent agent = debugger.getAgentProxy().getAgent();
        final StringWriter writer = new StringWriter();
        final Printer printer = new Printer(writer, true);
        for(Object o : selection)
        {
            Wme w = Adaptables.adapt(o, Wme.class);
            Identifier id = Adaptables.adapt(o, Identifier.class);
            PrintPreferencesCommand command = new PrintPreferencesCommand();
            command.setPrintProduction(true);
            command.setWmeTraceType(WmeTraceType.FULL);
            if(w != null)
            {
                command.setId(w.getIdentifier());
                command.setAttr(w.getAttribute());
            }
            else if(id != null)
            {
                command.setId(id);
                command.setObject(true);
            }
            else
            {
                continue;
            }
            
            try
            {
                command.print(agent, printer);
            }
            catch (IOException e)
            {
                printer.error(e.getMessage());
                e.printStackTrace();
            }
            printer.print("\n");
            break;
        }
        
//        printer.print("*** preferences\n");
//        agent.soarReteListener.print_match_set(printer, 
//                WmeTraceType.FULL, 
//                EnumSet.of(MatchSetTraceType.MS_ASSERT, MatchSetTraceType.MS_RETRACT));
        
        return writer.toString();
    }
}

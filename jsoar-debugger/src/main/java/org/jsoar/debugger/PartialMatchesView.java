/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import org.jsoar.debugger.selection.SelectionListener;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionManager;
import org.jsoar.kernel.rete.PartialMatches;
import org.jsoar.kernel.rete.PartialMatches.Entry;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.SwingCompletionHandler;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.adaptables.Adaptables;

/**
 * @author ray
 */
public class PartialMatchesView extends AbstractAdaptableView implements SelectionListener, Refreshable
{
    private static final long serialVersionUID = -5150761314645770374L;

    private final ThreadedAgent agent;
    private final SelectionManager selectionManager;
    private JTextPane textArea = new JTextPane();
    
    public PartialMatchesView(JSoarDebugger debugger)
    {
        super("partialmatches", "Partial Matches");
        
        this.agent = debugger.getAgent();
        this.selectionManager = debugger.getSelectionManager();
        
        JPanel p = new JPanel(new BorderLayout());
        this.textArea.setEditable(false);
        this.textArea.setContentType("text/html");

        p.add(new JScrollPane(textArea), BorderLayout.CENTER);
        
        setContentPane(p);

        this.selectionManager.addListener(this);
        selectionChanged(selectionManager);
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.selection.SelectionListener#selectionChanged(org.jsoar.debugger.selection.SelectionManager)
     */
    @Override
    public void selectionChanged(SelectionManager manager)
    {
        getMatchOutput(new ArrayList<Object>(manager.getSelection()));
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.debugger.Refreshable#refresh(boolean)
     */
    @Override
    public void refresh(boolean afterInitSoar)
    {
        getMatchOutput(new ArrayList<Object>(selectionManager.getSelection()));
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.AbstractAdaptableView#getShortcutKey()
     */
    @Override
    public String getShortcutKey()
    {
        return "ctrl M";
    }

    private void getMatchOutput(final List<Object> selection)
    {
        Callable<String> matchCall = new Callable<String>() {

            @Override
            public String call() throws Exception
            {
                return safeGetMatchOutput(selection);
            }};
        CompletionHandler<String> finish = new CompletionHandler<String>() {
            @Override
            public void finish(String result)
            {
                if(result.length() != 0)
                {
                    textArea.setText(result);
                    textArea.setCaretPosition(0);
                }
            }
            
        };
        agent.execute(matchCall, SwingCompletionHandler.newInstance(finish));
    }
    
    private Production getProduction(ProductionManager pm, Object o)
    {
        Production p = Adaptables.adapt(o, Production.class);
        if(p != null)
        {
            return p;
        }
        
        return pm.getProduction(o.toString());
    }
    
    private String safeGetMatchOutput(List<Object> selection)
    {
        final StringBuilder b = new StringBuilder();
        b.append("<html>");
        for(Object o : selection)
        {
            final Production p = getProduction(agent.getProductions(), o);
            if(p != null)
            {
                b.append("<h3>" + escape(p.getName()) + "</h3>");
                final PartialMatches pm = p.getPartialMatches();
                final List<Entry> entries = pm.getEntries();
                if(entries.size() > 0)
                {
                    formatEntries(b, entries, 0);
                    b.append("<br>");
                    final Entry lastEntry = entries.get(entries.size() - 1);
                    final int total = lastEntry.matches;
                    b.append(String.format("<b><font color='%s'>%d complete match%s.</font><b>", 
                            total > 0 ? "green" : "red",
                            total,
                            total != 1 ? "es" : ""));
                }
                else
                {
                    b.append("No match info available<br>");
                }
            }
            b.append("<br>");
        }
        
        b.append("</html>");
        return b.toString();
    }

    private String escape(String s)
    {
        return s.replace("&", "&amp;").replace("<", "&lt;");
    }
    
    private void spaces(StringBuilder b, int level)
    {
        for(int i = 0; i < level; i++)
        {
            b.append("&nbsp;&nbsp;");
        }
    }
    
    private void formatPositiveEntry(StringBuilder b, Entry e)
    {
        b.append("<font color='" + (e.matches > 0 ? "green" : "red") + "'>");
        b.append("<b>" + e.matches + "</b> ");
        b.append(escape(String.format("%s", e.condition)));
        b.append("</font>");
    }

    private void formatNegativeEntry(final StringBuilder b, Entry e, int level)
    {
        // how about &not; ??
        b.append("-{<br>");
        formatEntries(b, e.negatedSubConditions, level+1);
        spaces(b, level);
        b.append(String.format("} <b><font color='%s'>%d</font></b>", e.matches > 0 ? "green" : "red", e.matches));
    }
    
    private void formatEntries(final StringBuilder b, final List<Entry> entries, int level)
    {
        b.append("<font face='monospace'>");
        for(Entry e : entries)
        {
            spaces(b, level);
            if(e.negatedSubConditions == null)
            {
                formatPositiveEntry(b, e);
            }
            else
            {
                formatNegativeEntry(b, e, level);
            }
            b.append("<br>");
        }
        b.append("</font>");
    }
}

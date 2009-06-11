/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import org.flexdock.docking.DockingConstants;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jsoar.debugger.actions.AbstractDebuggerAction;
import org.jsoar.debugger.selection.SelectionProvider;
import org.jsoar.debugger.selection.TableSelectionProvider;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.SwingCompletionHandler;
import org.jsoar.util.StringTools;
import org.jsoar.util.SwingTools;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

/**
 * 
 * 
 * @author ray
 */
public class WmeSearchView extends AbstractAdaptableView implements Refreshable
{
    private static final long serialVersionUID = -2680823027837157856L;
    
    private final JSoarDebugger debugger;
    private final JLabel description = new JLabel(" Enter glob pattern below and click Search");
    private final JXTable wmeTable = new JXTable(new DefaultWmeTableModel());
    private final JTextField idField = new JTextField("*", 3);
    private final JTextField attrField = new JTextField("*", 6);
    private final JTextField valueField = new JTextField("*", 6);
    private final TableSelectionProvider selectionProvider = new TableSelectionProvider(wmeTable) {

        /* (non-Javadoc)
         * @see org.jsoar.debugger.selection.TableSelectionProvider#getValueAt(int)
         */
        @Override
        protected Object getValueAt(int row)
        {
            row = wmeTable.convertRowIndexToModel(row);
            return ((DefaultWmeTableModel) wmeTable.getModel()).getWmes().get(row);
        }};
    private JToggleButton synch = new JToggleButton(Images.SYNCH, false);
    {
        synch.setToolTipText("Re-run search when run ends");
    }
    
    public WmeSearchView(JSoarDebugger debuggerIn)
    {
        super("wmeSearch", "WME Search");
        this.debugger = debuggerIn;
        
        addAction(DockingConstants.PIN_ACTION);
                
        final JPanel barPanel = new JPanel(new BorderLayout());
        barPanel.add(description, BorderLayout.WEST);
        final JToolBar bar = createToolbar();
        
        barPanel.add(bar, BorderLayout.EAST);
        
        final JPanel p = new JPanel(new BorderLayout());
        p.add(barPanel, BorderLayout.NORTH);
        
        this.wmeTable.setHighlighters(HighlighterFactory.createAlternateStriping());
        this.wmeTable.setShowGrid(false);
        this.wmeTable.setDefaultRenderer(Identifier.class, new DefaultWmeTableCellRenderer());
        
        p.add(new JScrollPane(wmeTable), BorderLayout.CENTER);
        
        final ActionListener action = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                doSearch();
            }
        };
        
        final JPanel searchPanel = new JPanel();
        searchPanel.add(new JLabel("Pattern: ("));
        searchPanel.add(idField);
        idField.addActionListener(action);
        SwingTools.addSelectAllOnFocus(idField);

        searchPanel.add(new JLabel("^"));
        searchPanel.add(attrField);
        attrField.addActionListener(action);
        SwingTools.addSelectAllOnFocus(attrField);
        
        searchPanel.add(new JLabel(" "));
        searchPanel.add(valueField);
        valueField.addActionListener(action);
        SwingTools.addSelectAllOnFocus(valueField);
        searchPanel.add(new JLabel(")"));
        
        searchPanel.add(new JButton(new AbstractAction("Search") {

            private static final long serialVersionUID = -98843167878839240L;

            @Override
            public void actionPerformed(ActionEvent e)
            {
                doSearch();
            }}));
        
        p.add(searchPanel, BorderLayout.SOUTH);
        
        setContentPane(p);
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.AbstractAdaptableView#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(Class<?> klass)
    {
        if(SelectionProvider.class.equals(klass))
        {
            return selectionProvider;
        }
        return super.getAdapter(klass);
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.Refreshable#refresh(boolean)
     */
    @Override
    public void refresh(boolean afterInitSoar)
    {
        if(synch.isSelected())
        {
            doSearch();
        }
    }

    private JToolBar createToolbar()
    {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        
        bar.add(synch);
        
        bar.add(new AbstractDebuggerAction("Print to trace", Images.COPY) {
            private static final long serialVersionUID = -3614573079885324027L;

            {
                setToolTip("Print wmes to trace");
            }
            @Override
            public void update()
            {
            }

            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                print();
            }});
        return bar;
    }

    private void print()
    {
        final Printer printer = debugger.getAgent().getPrinter();
        final DefaultWmeTableModel model = (DefaultWmeTableModel) wmeTable.getModel();
        printer.startNewLine();
        for(Wme w : model.getWmes())
        {
            printer.print("%s", w); // TODO: WmeImpl.formatTo adds a newline :(
        }
        printer.startNewLine();
    }
    
    private void doSearch()
    {
        final String id = idField.getText().trim();
        final String attr = attrField.getText().trim();
        final String value = valueField.getText().trim();
        
        final Callable<List<Wme>> call = new Callable<List<Wme>>() {

            @Override
            public List<Wme> call() throws Exception
            {
                return getWmes(id, attr, value);
            }
        };
        
        final CompletionHandler<List<Wme>> done = new CompletionHandler<List<Wme>>() {

            @Override
            public void finish(List<Wme> result)
            {
                wmeTable.setModel(new DefaultWmeTableModel(result));
                description.setText(String.format(
                   "<html>&nbsp;WMEs matching pattern <b><code>(%s ^%s %s)</code></b>", id, attr, value));
            }
        };
        
        debugger.getAgent().execute(call, SwingCompletionHandler.newInstance(done));
    }
    
    private List<Wme> getWmes(String id, String attr, String value)
    {
        final Agent agent = debugger.getAgent().getAgent();
        final Pattern idPattern = Pattern.compile(StringTools.createRegexFromGlob(id).toUpperCase());
        final Pattern attrPattern = Pattern.compile(StringTools.createRegexFromGlob(attr));
        final Pattern valuePattern = Pattern.compile(StringTools.createRegexFromGlob(value));
        
        final Predicate<Wme> predicate = new Predicate<Wme>() {

            @Override
            public boolean apply(Wme w)
            {
                return idPattern.matcher(w.getIdentifier().toString()).matches() &&
                       attrPattern.matcher(w.getAttribute().toString()).matches() &&
                       valuePattern.matcher(w.getValue().toString()).matches();
            }
            
        };
        return new ArrayList<Wme>(Collections2.filter(agent.getAllWmesInRete(), predicate));
    }
}

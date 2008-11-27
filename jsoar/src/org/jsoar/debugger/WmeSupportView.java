/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.concurrent.Callable;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.flexdock.docking.DockingConstants;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jsoar.debugger.actions.AbstractDebuggerAction;
import org.jsoar.debugger.selection.SelectionListener;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeSupportInfo;
import org.jsoar.util.adaptables.Adaptables;

/**
 * Simple view that shows preferences for selected id or wme.
 * 
 * @author ray
 */
public class WmeSupportView extends AbstractAdaptableView implements SelectionListener
{
    private static final long serialVersionUID = -5150761314645770374L;

    private final LittleDebugger debugger;
    private final SelectionManager selectionManager;
    private final JEditorPane source = new JEditorPane("text/html", "");
    private final JXTable sourceWmeTable = new JXTable();
    private WmeSupportInfo sourceInfo;
    
    public WmeSupportView(LittleDebugger debuggerIn)
    {
        super("wmeSupport", "WME Support");
        this.debugger = debuggerIn;
        this.selectionManager = debugger.getSelectionManager();
        
        addAction(DockingConstants.PIN_ACTION);
                
        JPanel barPanel = new JPanel(new BorderLayout());
        source.setEditable(false);
        source.setOpaque(false);
        source.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent hle) {
                if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
                    productionClicked();
                }
            }
        });
        
        barPanel.add(source, BorderLayout.WEST);
        JToolBar bar = createToolbar();
        
        barPanel.add(bar, BorderLayout.EAST);
        
        JPanel p = new JPanel(new BorderLayout());
        p.add(barPanel, BorderLayout.NORTH);
        
        JPanel wmesPanel = new JPanel(new BorderLayout());
        wmesPanel.setBorder(BorderFactory.createTitledBorder("Supporting WMEs"));
        
        this.sourceWmeTable.setShowGrid(false);
        this.sourceWmeTable.setHighlighters(HighlighterFactory.createAlternateStriping());
        this.sourceWmeTable.setColumnControlVisible(true);
        
        wmesPanel.add(new JScrollPane(sourceWmeTable), BorderLayout.CENTER);
        
        p.add(wmesPanel, BorderLayout.CENTER);
        
        setContentPane(p);

        this.selectionManager.addListener(this);
    }

    /**
     * 
     */
    private void productionClicked()
    {
        ProductionListView prodView = Adaptables.adapt(debugger, ProductionListView.class);
        if(prodView != null)
        {
            prodView.selectProduction(sourceInfo.getSource());
        }
    }

    /**
     * @return
     */
    private JToolBar createToolbar()
    {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        
        bar.add(new AbstractDebuggerAction("Print to trace", Images.COPY) {
            private static final long serialVersionUID = -3614573079885324027L;

            {
                setToolTip("Print preferences to trace");
            }
            @Override
            public void update()
            {
            }

            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                if(sourceInfo != null)
                {
                    debugger.getAgentProxy().getAgent().getPrinter().startNewLine().print(sourceInfo.toString()).flush();
                }
            }});
        return bar;
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.selection.SelectionListener#selectionChanged(org.jsoar.debugger.selection.SelectionManager)
     */
    @Override
    public void selectionChanged(SelectionManager manager)
    {
        final Object selection = manager.getSelectedObject();
        Wme w = Adaptables.adapt(selection, Wme.class);
        if(w != null)
        {
            sourceInfo = getSourceInfo(w);
            if(sourceInfo != null)
            {
                final Production prod = sourceInfo.getSource();
                source.setText(String.format("<html><b><code>%#s</code></b> is %s-supported by <a href=''>%s</a></html>", w, 
                                             sourceInfo.isOSupported() ? "O" : "I",
                                             prod != null ? prod.getName() : "[dummy production]"));
                sourceWmeTable.setModel(new DefaultWmeTableModel(sourceInfo.getSourceWmes()));
                sourceWmeTable.packAll();
            }
            else
            {
                source.setText(String.format("<html><b><code>%#s</code></b> is an architecture or I/O WME</html>", w));
                sourceWmeTable.setModel(new DefaultWmeTableModel(new ArrayList<Wme>()));
                sourceWmeTable.packAll();
            }
        }
    }
    
    private WmeSupportInfo getSourceInfo(final Wme w)
    {
        Callable<WmeSupportInfo> callable = new Callable<WmeSupportInfo>() {

            @Override
            public WmeSupportInfo call() throws Exception
            {
                final Agent agent = debugger.getAgentProxy().getAgent();
                return WmeSupportInfo.get(agent, w);
            }};
        return debugger.getAgentProxy().execute(callable);
    }
}

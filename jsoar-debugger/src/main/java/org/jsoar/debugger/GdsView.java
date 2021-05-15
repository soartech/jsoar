/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 30, 2010
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.jsoar.debugger.DefaultWmeTableModel.Columns;
import org.jsoar.debugger.selection.SelectionProvider;
import org.jsoar.debugger.selection.TableSelectionProvider;
import org.jsoar.kernel.Goal;
import org.jsoar.kernel.GoalDependencySet;
import org.jsoar.kernel.events.GdsGoalRemovedEvent;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

/**
 * @author ray
 */
public class GdsView extends AbstractAdaptableView implements Refreshable, Disposable
{
    private static final int MAX_REMOVAL_HISTORY = 5;
    
    private final JSoarDebugger debugger;
    private final JLabel label = new JLabel("No goals");
    private final DefaultWmeTableModel wmeModel = new DefaultWmeTableModel();
    private final JXTable wmeTable = new JXTable(wmeModel);
    private final TableSelectionProvider selectionProvider = new TableSelectionProvider(wmeTable) {

        @Override
        protected Object getValueAt(int row)
        {
            row = wmeTable.convertRowIndexToModel(row);
            return ((DefaultWmeTableModel) wmeTable.getModel()).getWmes().get(row);
        }};
    private final List<String> recentRemovals = new ArrayList<String>();
    private final JLabel recentSummary = new JLabel("Recent removals: None");
    private final SoarEventListener removalListener = new SoarEventListener(){
        @Override
        public void onEvent(SoarEvent event)
        {
            updateRemovalSummary((GdsGoalRemovedEvent) event);
        }
    };
    
    public GdsView(JSoarDebugger debugger)
    {
        super("gds", "GDS");
        
        this.debugger = debugger;
        
        final JPanel p = new JPanel(new BorderLayout());
        p.add(label, BorderLayout.NORTH);
        
        this.wmeTable.setHighlighters(HighlighterFactory.createAlternateStriping());
        this.wmeTable.setShowGrid(false);
        this.wmeTable.setDefaultRenderer(Identifier.class, new DefaultWmeTableCellRenderer());
        this.wmeTable.getColumnExt(Columns.Acceptable.ordinal()).setVisible(false);
        
        p.add(new JScrollPane(wmeTable), BorderLayout.CENTER);
        
        p.add(recentSummary, BorderLayout.SOUTH);
        
        getContentPane().add(p);
        
        this.debugger.getAgent().getEvents().addListener(GdsGoalRemovedEvent.class, removalListener);
    }

    
    /* (non-Javadoc)
     * @see org.jsoar.debugger.AbstractAdaptableView#getShortcutKey()
     */
    @Override
    public String getShortcutKey()
    {
        return "ctrl shift G";
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.Disposable#dispose()
     */
    @Override
    public void dispose()
    {
        this.debugger.getAgent().getEvents().removeListener(GdsGoalRemovedEvent.class, removalListener);
    }


    /* (non-Javadoc)
     * @see org.jsoar.debugger.Refreshable#refresh(boolean)
     */
    @Override
    public void refresh(boolean afterInitSoar)
    {
        final Callable<Model> start = new Callable<Model>() {
            @Override
            public Model call() throws Exception
            {
                final List<Goal> stack = debugger.getAgent().getAgent().getGoalStack();
                return new Model(!stack.isEmpty() ? stack.get(stack.size() - 1) : null);
            }
        };
        final CompletionHandler<Model> finish = new CompletionHandler<Model>()
        {
            @Override
            public void finish(Model result)
            {
                if(result.goal != null)
                {
                    if(result.wmes.isEmpty())
                    {
                        label.setText(String.format("<html><b>&nbsp;<code>%s</code> has no GDS</b></html>", result.goal.getIdentifier()));
                    }
                    else
                    {
                        label.setText(String.format("<html><b>&nbsp;GDS for <code>%s</code>.</b> (%d wmes)</html>", 
                                     result.goal.getIdentifier(), result.wmes.size()));
                    }
                }
                else
                {
                    label.setText("<html><b>&nbsp;No goals</b></html>");
                }
                wmeModel.setWmes(result.wmes);
            }
        };
        
        debugger.getAgent().execute(start, SwingCompletionHandler.newInstance(finish));
        
        if(afterInitSoar)
        {
            recentRemovals.clear();
            recentSummary.setText("Recent removals: None");
            setTitleText("GDS");
        }
    }

    private void updateRemovalSummary(final GdsGoalRemovedEvent event)
    {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run()
            {
                recentRemovals.add(0, String.format("%s %#s", event.getGoal(), event.getCause()));
                if(recentRemovals.size() > MAX_REMOVAL_HISTORY)
                {
                    recentRemovals.remove(recentRemovals.size() - 1);
                }
                recentSummary.setText("Recent removals: " + Joiner.on(", ").join(recentRemovals));
                
                setTitleText(String.format("GDS (%s!)", event.getGoal()));
            }
        });
    }
    
    private static List<Wme> getGdsWmes(Goal goal)
    {
        final GoalDependencySet gds = Adaptables.adapt(goal, GoalDependencySet.class);
        
        return gds != null ? Lists.newArrayList(gds.getWmes()) : new ArrayList<Wme>();
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

    private static class Model
    {
        final Goal goal;
        final List<Wme> wmes;
        
        public Model(Goal goal)
        {
            this.goal = goal;
            this.wmes = getGdsWmes(this.goal);
        }
    }
}

/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 21, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.flexdock.docking.Dockable;
import org.flexdock.docking.DockingConstants;
import org.flexdock.docking.DockingManager;
import org.flexdock.docking.activation.ActiveDockableTracker;
import org.flexdock.util.SwingUtility;
import org.flexdock.view.View;
import org.flexdock.view.Viewport;
import org.jsoar.debugger.actions.ActionManager;
import org.jsoar.debugger.actions.ExitAction;
import org.jsoar.debugger.actions.InitSoarAction;
import org.jsoar.debugger.actions.RunAction;
import org.jsoar.debugger.actions.SourceFileAction;
import org.jsoar.debugger.actions.StopAction;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.debugger.selection.SelectionProvider;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.events.AfterInitSoarEvent;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.runtime.ThreadedAgentProxy;
import org.jsoar.tcl.SoarTclException;
import org.jsoar.tcl.SoarTclInterface;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;

/**
 * @author ray
 */
public class LittleDebugger extends JPanel
{
    private static final long serialVersionUID = 7997119112479665988L;

    private final SelectionManager selectionManager = new SelectionManager();
    private final ActionManager actionManager = new ActionManager(this);
    private final RunControlModel runControlModel = new RunControlModel();
        
    private Agent agent = new Agent();
    private SoarTclInterface ifc = new SoarTclInterface(agent);
    private ThreadedAgentProxy proxy = new ThreadedAgentProxy(agent);
    
    private JFrame frame;
    private Viewport viewport = new Viewport();
    private final TraceView traceView;
    private final ProductionListView prodListView;
    private final SelectionInfoView textView;
    private final WorkingMemoryGraphView wmGraphView;
    private final MatchesView matchesView;
    
    private DefaultListModel wmeListModel = new DefaultListModel();
    private JList wmeList = new JList(wmeListModel);

    
    public LittleDebugger(JFrame frame)
    {
        super(new BorderLayout());
        
        this.frame = frame;
        
        this.add(viewport, BorderLayout.CENTER);
        
        initActions();
        
        traceView = new TraceView(this);
        proxy.initialize();
        
        viewport.dock(traceView);
        prodListView = new ProductionListView(this);
        traceView.dock(prodListView, DockingConstants.EAST_REGION, 0.75f);
        //prodListView.dock(createWorkingMemoryView(), DockingConstants.SOUTH_REGION);
        
        matchesView = new MatchesView(this);
        prodListView.dock(matchesView, DockingConstants.SOUTH_REGION);
        
        textView = new SelectionInfoView(this);
        matchesView.dock(textView, DockingConstants.SOUTH_REGION);
        
        wmGraphView = new WorkingMemoryGraphView(this);
        traceView.dock(wmGraphView, DockingConstants.SOUTH_REGION);
        
        
        initMenuBar();
        initToolbar();
        
        ActiveDockableTracker.getTracker(frame).addPropertyChangeListener(new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                Dockable newDockable = (Dockable) evt.getNewValue();
                SelectionProvider provider = Adaptables.adapt(newDockable, SelectionProvider.class);
                if(provider != null)
                {
                    selectionManager.setSelectionProvider(provider);
                }
            }});
        
        agent.getEventManager().addListener(AfterInitSoarEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                update(true);
            }});
        update(false);
    }

    
    private View createWorkingMemoryView()
    {
        View view = new View("wm", "Working Memory");
        view.addAction(DockingConstants.PIN_ACTION);
        
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(wmeList), BorderLayout.CENTER);
        
        view.setContentPane(p);
        
        return view;
    }
    
    public ThreadedAgentProxy getAgentProxy()
    {
        return proxy;
    }
    
    public SoarTclInterface getTcl()
    {
        return ifc;
    }
    
    public SelectionManager getSelectionManager()
    {
        return selectionManager;
    }
    
    public RunControlModel getRunControlModel()
    {
        return runControlModel;
    }
    
    public ActionManager getActionManager()
    {
        return actionManager;
    }
    
    public void exit()
    {
        proxy.shutdown();
        System.exit(0);
    }
    
    private void initActions()
    {
        new ExitAction(actionManager);
        new RunAction(actionManager);
        new StopAction(actionManager);
        new InitSoarAction(actionManager);
        new SourceFileAction(actionManager);
    }
    
    private void initMenuBar()
    {
        JMenuBar bar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(actionManager.getAction(SourceFileAction.class));
        fileMenu.addSeparator();
        fileMenu.add(actionManager.getAction(ExitAction.class));
        
        bar.add(fileMenu);
        bar.add(new TraceMenu(agent.trace));
        
        JMenu runMenu = new JMenu("Run");
        runMenu.add(actionManager.getAction(RunAction.class));
        runMenu.add(actionManager.getAction(StopAction.class));
        runMenu.addSeparator();
        runMenu.add(actionManager.getAction(InitSoarAction.class));
        bar.add(runMenu);
        
        frame.setJMenuBar(bar);
    }
    
    private void initToolbar()
    {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        
        
        add(bar, BorderLayout.NORTH);
    }
    
    public void update(final boolean afterInitSoar)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable() {

                @Override
                public void run()
                {
                    update(afterInitSoar);
                }});
            return;
        }
        
        actionManager.updateActions();
        
        updateWmes();
        prodListView.refresh();
        wmGraphView.refresh(afterInitSoar);
    }
    
    private void updateWmes()
    {
        wmeListModel.clear();
        
        Callable<List<String>> callable = new Callable<List<String>>() {

            @Override
            public List<String> call() throws Exception
            {
                final List<String> wmes = new ArrayList<String>();
                for(WmeImpl wme : agent.rete.getAllWmes())
                {
                    wmes.add(String.format("%s", wme));
                }
                return wmes;
            }};
        
        List<String> wmes = proxy.execute(callable);
        Collections.sort(wmes);
        for(String s : wmes)
        {
            wmeListModel.addElement(s);
        }
    }
        
    private static void initializeLookAndFeel()
    {
        try
        {
            // Use the look and feel of the system we're running on rather
            // than Java. If an error occurs, we proceed normally using
            // whatever L&F we get.
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (UnsupportedLookAndFeelException e) { }
        catch (ClassNotFoundException e) { }
        catch (InstantiationException e) { }
        catch (IllegalAccessException e) { }
    }
    
    public static void main(final String[] args)
    {
        initializeLookAndFeel();
        
        SwingUtilities.invokeLater(new Runnable() {
            
            public void run() { initialize(args); }
        });
    }

    /**
     * @param args
     */
    private static void initialize(final String[] args)
    {
        DockingManager.setFloatingEnabled(true);
        
        JFrame frame = new JFrame("Little JSoar Debugger");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        final LittleDebugger littleDebugger = new LittleDebugger(frame);
        frame.setContentPane(littleDebugger);
        frame.setSize(1200, 1024);
        SwingUtility.centerOnScreen(frame);
        frame.setVisible(true);
        
        littleDebugger.proxy.execute(new Runnable() {
            public void run() 
            {
                for(String arg : args)
                {
                    try
                    {
                        littleDebugger.ifc.sourceFile(arg);
                    }
                    catch (SoarTclException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                littleDebugger.update(false);
            } } );
    }
    
}

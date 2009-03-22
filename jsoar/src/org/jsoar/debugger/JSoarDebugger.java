/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 21, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.flexdock.docking.Dockable;
import org.flexdock.docking.DockingConstants;
import org.flexdock.docking.DockingManager;
import org.flexdock.docking.activation.ActiveDockableTracker;
import org.flexdock.util.SwingUtility;
import org.flexdock.view.Viewport;
import org.jsoar.debugger.actions.AboutAction;
import org.jsoar.debugger.actions.ActionManager;
import org.jsoar.debugger.actions.EditProductionAction;
import org.jsoar.debugger.actions.ExciseProductionAction;
import org.jsoar.debugger.actions.ExitAction;
import org.jsoar.debugger.actions.InitSoarAction;
import org.jsoar.debugger.actions.RunAction;
import org.jsoar.debugger.actions.SourceFileAction;
import org.jsoar.debugger.actions.StopAction;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.debugger.selection.SelectionProvider;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.events.AfterInitSoarEvent;
import org.jsoar.kernel.events.StopEvent;
import org.jsoar.runtime.Completer;
import org.jsoar.runtime.SwingCompletion;
import org.jsoar.runtime.ThreadedAgentProxy;
import org.jsoar.tcl.SoarTclException;
import org.jsoar.tcl.SoarTclInterface;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;

/**
 * @author ray
 */
public class JSoarDebugger extends JPanel implements Adaptable
{
    private static final long serialVersionUID = 7997119112479665988L;

    private final SelectionManager selectionManager = new SelectionManager();
    private final ActionManager actionManager = new ActionManager(this);
    private final RunControlModel runControlModel = new RunControlModel();
        
    private Agent agent;
    private SoarTclInterface ifc;
    private ThreadedAgentProxy proxy;
    private LoadPluginCommand loadPluginCommand = new LoadPluginCommand(this);
    private List<JSoarDebuggerPlugin> plugins = new CopyOnWriteArrayList<JSoarDebuggerPlugin>();
    
    private JFrame frame;
    private Viewport viewport = new Viewport();
    private StatusBar status = new StatusBar(this);
    
    private final List<AbstractAdaptableView> views = new ArrayList<AbstractAdaptableView>();
    
    /**
     * Construct a new debugger. Add to a JFrame and call initialize().
     */
    private JSoarDebugger()
    {
        super(new BorderLayout());
        
    }
    
    /**
     * Initialize the debugger
     * 
     * @param parentFrame The parent frame of the debugger
     * @param proxy A non-null, <b>initialized</b> agent proxy
     */
    private void initialize(JFrame parentFrame, ThreadedAgentProxy proxy)
    {
        this.frame = parentFrame;
        this.agent = proxy.getAgent();
        this.proxy = proxy;
        this.ifc = SoarTclInterface.findOrCreate(agent);
        
        this.ifc.getInterpreter().createCommand("load-plugin", loadPluginCommand);
        
        this.add(viewport, BorderLayout.CENTER);
        
        initActions();
        
        this.add(status, BorderLayout.SOUTH);
        
        initViews();
        initMenuBar();
        initToolbar();
        
        // Track selection to active view
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
        
        // Update after init-soar
        agent.getEventManager().addListener(AfterInitSoarEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                newUpdateCompleter(true).finish(null);
            }});

        // Update when the agent stops running
        agent.getEventManager().addListener(StopEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                newUpdateCompleter(false).finish(null);
            }});
        
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e)
            {
                exit();
            }});
        
        Adaptables.adapt(this, TraceView.class).setVisible(true);
        
        update(false);
        
    }

    private void initViews()
    {
        final TraceView traceView = new TraceView(this); 
        views.add(traceView);
        viewport.dock(traceView);
        
        final ProductionListView prodListView = new ProductionListView(this);
        views.add(prodListView);
        traceView.dock(prodListView, DockingConstants.EAST_REGION, 0.75f);
        
        final MatchesView matchesView = new MatchesView(this);
        views.add(matchesView);
        prodListView.dock(matchesView, DockingConstants.SOUTH_REGION);
        
        final ProductionEditView prodEditView = new ProductionEditView(this);
        views.add(prodEditView);
        traceView.dock(prodEditView);
        
        final WorkingMemoryTreeView wmTreeView = new WorkingMemoryTreeView(this);
        views.add(wmTreeView);
        traceView.dock(wmTreeView, DockingConstants.SOUTH_REGION);
        
        final PreferencesView preferencesView = new PreferencesView(this);
        views.add(preferencesView);
        wmTreeView.dock(preferencesView, DockingConstants.EAST_REGION, 0.6f);
        
        final WmeSupportView wmeSupportView = new WmeSupportView(this);
        views.add(wmeSupportView);
        preferencesView.dock(wmeSupportView, DockingConstants.SOUTH_REGION);
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

    public void updateActionsAndStatus()
    {
        if(SwingUtilities.isEventDispatchThread())
        {
            actionManager.updateActions();
            status.refresh(false);
        }
        else
        {
            SwingUtilities.invokeLater(new Runnable() { public void run() {
                updateActionsAndStatus();
            } });
        }
    }
    
    void addPlugin(JSoarDebuggerPlugin plugin)
    {
        plugins.add(plugin);
    }
    
    public void exit()
    {
        for(JSoarDebuggerPlugin plugin : plugins)
        {
            plugin.shutdown();
        }
        plugins.clear();
        
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
        new ExciseProductionAction(actionManager);
        new AboutAction(actionManager);
        new EditProductionAction(actionManager);
    }
    
    private void initMenuBar()
    {
        JMenuBar bar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(actionManager.getAction(SourceFileAction.class));
        fileMenu.addSeparator();
        fileMenu.add(actionManager.getAction(ExitAction.class));
        
        bar.add(fileMenu);
        
        JMenu runMenu = new JMenu("Run");
        runMenu.add(actionManager.getAction(RunAction.class));
        runMenu.add(actionManager.getAction(StopAction.class));
        runMenu.addSeparator();
        runMenu.add(actionManager.getAction(InitSoarAction.class));
        bar.add(runMenu);
        
        JMenu helpMenu = new JMenu("Help");
        helpMenu.add(actionManager.getAction(AboutAction.class));
        bar.add(helpMenu);
        
        frame.setJMenuBar(bar);
    }
    
    private void initToolbar()
    {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        
        bar.add(new RunControlPanel(this));
        
        add(bar, BorderLayout.NORTH);
    }
    
    private void update(final boolean afterInitSoar)
    {
        updateActionsAndStatus();
        
        List<Refreshable> refreshables = Adaptables.adaptCollection(views, Refreshable.class);
        for(Refreshable r : refreshables)
        {
            r.refresh(afterInitSoar);
        }
    }
          
    public Completer<Void> newUpdateCompleter(final boolean afterInitSoar)
    {
        return SwingCompletion.newInstance( new Completer<Void>() {

            @Override
            public void finish(Void result)
            {
                update(afterInitSoar);
            }
        });
    }
    /**
     * Initialize the UI look and feel to the system look and feel. 
     */
    public static void initializeLookAndFeel()
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
     * Attach a new debugger window to the given threaded agent. This function
     * <b>must<b/> be called from the Swing event thread!
     * 
     * @param proxy an <b>initialized</b> threaded agent to attach to
     * @return the debugger
     */
    public static JSoarDebugger attach(ThreadedAgentProxy proxy)
    {
        DockingManager.setFloatingEnabled(true);
        
        final JSoarDebugger debugger = new JSoarDebugger();
        
        JFrame frame = new JFrame("JSoar Debugger - " + proxy.getAgent().getName());
        
        frame.setContentPane(debugger);
        frame.setSize(1200, 1024);
        
        debugger.initialize(frame, proxy);

        SwingUtility.centerOnScreen(frame);
        frame.setVisible(true);
        return debugger;
    }

    /**
     * Detach debugger from agent
     */
    public void detach()
    {
        // TODO clean up dock property listener
        // TODO clean up soar event listener
        // TODO close debugger window
    }
    
    /**
     * This is identical to {@link #main(String[])}, but it must be called from
     * the Swing event thread. Also the look and feel must be initialized prior
     * to calling this.
     * 
     * @param args command-line arguments
     */
    public static JSoarDebugger initialize(final String[] args)
    {
        final JSoarDebugger debugger = attach(new ThreadedAgentProxy(new Agent()).initialize());
        
        debugger.proxy.execute(new Callable<Void>() {
            public Void call() 
            {
                for(String arg : args)
                {
                    try
                    {
                        debugger.ifc.sourceFile(arg);
                    }
                    catch (SoarTclException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                return null;
            } }, debugger.newUpdateCompleter(false));
        return debugger;
    }

    /* (non-Javadoc)
     * @see org.jsoar.util.adaptables.Adaptable#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(Class<?> klass)
    {
        if(klass.equals(SoarTclInterface.class))
        {
            return ifc;
        }
        if(klass.equals(Agent.class))
        {
            return agent;
        }
        if(klass.equals(ThreadedAgentProxy.class))
        {
            return proxy;
        }
        if(klass.equals(SelectionManager.class))
        {
            return selectionManager;
        }
        return Adaptables.findAdapter(views, klass);
    }
    
}

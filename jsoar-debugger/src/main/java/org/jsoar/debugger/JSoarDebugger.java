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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.jsoar.debugger.actions.RestoreLayoutAction;
import org.jsoar.debugger.actions.RunAction;
import org.jsoar.debugger.actions.SourceFileAction;
import org.jsoar.debugger.actions.StopAction;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.debugger.selection.SelectionProvider;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.DebuggerProvider;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.commands.RunCommand;
import org.jsoar.kernel.commands.StopCommand;
import org.jsoar.kernel.events.AfterInitSoarEvent;
import org.jsoar.kernel.events.StopEvent;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.SwingCompletionHandler;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.SwingTools;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.jsoar.util.commands.SoarCommands;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.jsoar.util.properties.PropertyListener;
import org.jsoar.util.properties.PropertyListenerHandle;

/**
 * @author ray
 */
public class JSoarDebugger extends JPanel implements Adaptable
{
    private static final long serialVersionUID = 7997119112479665988L;
    private static final Log logger = LogFactory.getLog(JSoarDebugger.class);

    private static final Map<ThreadedAgent, JSoarDebugger> debuggers = Collections.synchronizedMap(new HashMap<ThreadedAgent, JSoarDebugger>());
    
    private final SelectionManager selectionManager = new SelectionManager();
    private final ActionManager actionManager = new ActionManager(this);
    private final RunControlModel runControlModel = new RunControlModel();
        
    private JSoarDebuggerConfiguration configuration = new DefaultDebuggerConfiguration();
    
    //private Agent agent;
    private ThreadedAgent proxy;
    private LoadPluginCommand loadPluginCommand = new LoadPluginCommand(this);
    private List<JSoarDebuggerPlugin> plugins = new CopyOnWriteArrayList<JSoarDebuggerPlugin>();
    
    private JFrame frame;
    private Viewport viewport = new Viewport();
    private StatusBar status;
    
    private final List<AbstractAdaptableView> views = new ArrayList<AbstractAdaptableView>();
    
    private PropertyChangeListener dockTrackerListener = null;
    private final List<SoarEventListener> soarEventListeners = new ArrayList<SoarEventListener>();
    private final List<PropertyListenerHandle<?>> propertyListeners = new ArrayList<PropertyListenerHandle<?>>();
    
    
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
    private void initialize(JFrame parentFrame, ThreadedAgent proxy)
    {
        logger.info("Initializing debugger for agent '" + proxy + "'");
        
        this.frame = parentFrame;
        this.frame.setTitle("JSoar Debugger - " + proxy.getName());

        this.proxy = proxy;
        final SoarCommandInterpreter ifc = proxy.getInterpreter();
        ifc.addCommand("run", new RunCommand(proxy));
        ifc.addCommand("stop", new StopCommand(proxy));
        ifc.addCommand("stop-soar", new StopCommand(proxy));
        ifc.addCommand("load-plugin", loadPluginCommand);
        
        this.add(viewport, BorderLayout.CENTER);
        
        initActions();
        
        this.add(status = new StatusBar(proxy), BorderLayout.SOUTH);
        
        initViews();
        initMenuBar();
        initToolbar();
        
        // Track selection to active view
        ActiveDockableTracker.getTracker(frame).addPropertyChangeListener(
                dockTrackerListener = new PropertyChangeListener() {

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
        
        // Track the agent name in the title bar
        saveListener(proxy.getProperties().addListener(SoarProperties.NAME, new PropertyListener<String>() {

            @Override
            public void propertyChanged(
                    org.jsoar.util.properties.PropertyChangeEvent<String> event)
            {
                frame.setTitle("JSoar Debugger - " + event.getNewValue());
            }}));
        
        // Track agent's running state
        saveListener(proxy.getProperties().addListener(SoarProperties.IS_RUNNING, new PropertyListener<Boolean>() {

            @Override
            public void propertyChanged(
                    org.jsoar.util.properties.PropertyChangeEvent<Boolean> event)
            {
                updateActionsAndStatus();
            }}));

        // Update after init-soar
        proxy.getEvents().addListener(AfterInitSoarEvent.class, saveListener(new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                newUpdateCompleter(true).finish(null);
            }}));

        // Update when the agent stops running
        proxy.getEvents().addListener(StopEvent.class, saveListener(new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                newUpdateCompleter(false).finish(null);
            }}));
        
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

    private <T> void saveListener(PropertyListenerHandle<T> listener)
    {
        propertyListeners.add(listener);
    }
    
    private SoarEventListener saveListener(SoarEventListener listener)
    {
        soarEventListeners.add(listener);
        return listener;
    }
    
    private void initViews()
    {
        final TraceView traceView = addView(new TraceView(this)); 
        viewport.dock(traceView);
        
        final ProductionListView prodListView = addView(new ProductionListView(this));
        traceView.dock(prodListView, DockingConstants.EAST_REGION, 0.75f);
        
        final PartialMatchesView matchesView = addView(new PartialMatchesView(this));
        prodListView.dock(matchesView, DockingConstants.SOUTH_REGION);
        
        final MatchSetView matchSetView = addView(new MatchSetView(this));
        matchesView.dock(matchSetView);
        
        final ProductionEditView prodEditView = addView(new ProductionEditView(this));
        traceView.dock(prodEditView);
        
        final WorkingMemoryTreeView wmTreeView = addView(new WorkingMemoryTreeView(this));
        traceView.dock(wmTreeView, DockingConstants.SOUTH_REGION);
        
        final PreferencesView preferencesView = addView(new PreferencesView(this));
        wmTreeView.dock(preferencesView, DockingConstants.EAST_REGION, 0.6f);
        
        final WmeSupportView wmeSupportView = addView(new WmeSupportView(this));
        preferencesView.dock(wmeSupportView);
    }
    
    private <T extends AbstractAdaptableView> T addView(T view)
    {
        views.add(view);
        return view;
    }
    
    public ThreadedAgent getAgent()
    {
        return proxy;
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
        detach();
        configuration.exit();
    }
    
    public void restoreLayout()
    {
        // TODO: Implement layout storage in a way that doesn't suck.
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
        new RestoreLayoutAction(actionManager);
    }
    
    private void initMenuBar()
    {
        JMenuBar bar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(actionManager.getAction(SourceFileAction.class));
        fileMenu.addSeparator();
        fileMenu.add(actionManager.getAction(ExitAction.class));
        
        bar.add(fileMenu);
        
        final JMenu viewMenu = new JMenu("View");
        viewMenu.add(actionManager.getAction(RestoreLayoutAction.class));
        bar.add(viewMenu);
        
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
          
    public CompletionHandler<Void> newUpdateCompleter(final boolean afterInitSoar)
    {
        return SwingCompletionHandler.newInstance( new CompletionHandler<Void>() {

            @Override
            public void finish(Void result)
            {
                update(afterInitSoar);
            }
        });
    }
    
    public void setConfiguration(JSoarDebuggerConfiguration config)
    {
        this.configuration = config;
    }
    
    public static void main(final String[] args)
    {
        SwingTools.initializeLookAndFeel();
        
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
    public static JSoarDebugger attach(ThreadedAgent proxy)
    {
        synchronized(debuggers)
        {
            JSoarDebugger debugger = debuggers.get(proxy);
            if(debugger == null)
            {
                DockingManager.setFloatingEnabled(true);
        
                debugger = new JSoarDebugger();
                
                final JFrame frame = new JFrame();
                
                frame.setContentPane(debugger);
                frame.setSize(1200, 1024);
                
                debugger.initialize(frame, proxy);
        
                SwingUtility.centerOnScreen(frame);
                
                frame.setVisible(true);
                
                debuggers.put(proxy, debugger);
            }
            else
            {
                debugger.frame.setVisible(true);
                debugger.frame.requestFocus();
            }
            return debugger;
        }
    }

    /**
     * Detach debugger from agent
     */
    public void detach()
    {
        synchronized(debuggers)
        {
            logger.info(String.format("Detaching from agent '" + proxy + "'"));
            
            // clean up dock property listener
            ActiveDockableTracker.getTracker(frame).removePropertyChangeListener(dockTrackerListener);
            dockTrackerListener = null;
            
            // clean up soar prop listeners
            for(PropertyListenerHandle<?> listener : propertyListeners)
            {
                listener.removeListener();
            }
            
            // clean up soar event listener
            for(SoarEventListener listener : soarEventListeners)
            {
                proxy.getEvents().removeListener(null, listener);
            }
            soarEventListeners.clear();
            
            // clean up disposable views
            for(Disposable d : Adaptables.adaptCollection(views, Disposable.class))
            {
                d.dispose();
            }
            views.clear();
            
            if(frame.isVisible())
            {
                frame.setVisible(false);
            }
            
            for(JSoarDebuggerPlugin plugin : plugins)
            {
                plugin.shutdown();
            }
            plugins.clear();
            
            debuggers.remove(proxy);
        }
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
        final JSoarDebugger debugger = attach(ThreadedAgent.create());
        
        debugger.proxy.execute(new Callable<Void>() {
            public Void call() 
            {
                for(String arg : args)
                {
                    try
                    {
                        SoarCommands.source(debugger.getAgent().getInterpreter(), arg);
                    }
                    catch (SoarException e)
                    {
                        logger.error("Error sourcing file '" + arg + "'", e);
                    }
                }
                return null;
            } }, debugger.newUpdateCompleter(false));
        return debugger;
    }

    /**
     * Creates and returns a {@link DebuggerProvider} that will open this
     * debugger when an agent calls the debug RHS function
     * 
     * @return
     */
    public static DebuggerProvider newDebuggerProvider()
    {
        return newDebuggerProvider(null);
    }
    
    public static DebuggerProvider newDebuggerProvider(JSoarDebuggerConfiguration config)
    {
        return new DefaultDebuggerProvider(config);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.util.adaptables.Adaptable#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(Class<?> klass)
    {
        if(klass.equals(SoarCommandInterpreter.class))
        {
            return getAgent().getInterpreter();
        }
        if(klass.equals(Agent.class))
        {
            return proxy.getAgent();
        }
        if(klass.equals(ThreadedAgent.class))
        {
            return proxy;
        }
        if(klass.equals(SelectionManager.class))
        {
            return selectionManager;
        }
        if(klass.equals(RunControlModel.class))
        {
            return runControlModel;
        }
        if(klass.equals(ActionManager.class))
        {
            return actionManager;
        }
        Object o = Adaptables.findAdapter(views, klass);
        if(o != null)
        {
            return o;
        }
        return Adaptables.findAdapter(plugins, klass);
    }
    
}

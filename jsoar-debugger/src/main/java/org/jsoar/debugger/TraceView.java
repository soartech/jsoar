/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

import org.jsoar.debugger.ParseSelectedText.SelectedObject;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.debugger.selection.SelectionProvider;
import org.jsoar.debugger.syntax.Highlighter;
import org.jsoar.debugger.syntax.SyntaxSettings;
import org.jsoar.debugger.syntax.ui.SyntaxConfigurator;
import org.jsoar.debugger.util.IncrementalSearchPanel;
import org.jsoar.kernel.JSoarVersion;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.runtime.CompletionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bibliothek.gui.dock.common.action.CButton;

/**
 * @author ray
 */
public class TraceView extends AbstractAdaptableView implements Disposable
{
    private static final String HIGHLIGHT_IMMEDIATELY = "highlightImmediately";
    private static final String ENABLE_HIGHLIGHTING = "enableHighlighting";
    private static final String SCROLL_LOCK = "scrollLock";
    private static final String LIMIT = "limit";
    private static final String SEARCH = "search";
    
    private static final Logger LOG = LoggerFactory.getLogger(TraceView.class);
    
    private final JSoarDebugger debugger;
    private final CommandEntryPanel commandPanel;
    private final Provider selectionProvider = new Provider();
    
    private final IncrementalSearchPanel searchPanel;
    
    // Variables related to the triming limit along with a lock object for syncing between threads
    private Object limitLock = new Object();
    private int limit = -1;
    private int limitTolerance = 0;
    
    private boolean scrollLock = true;
    
    private final JTextPane outputWindow = new JTextPane()
    {
        private static final long serialVersionUID = 5161494134278464101L;
        
        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.text.JTextComponent#paste()
         */
        public void paste()
        {
            executePastedInput();
        }
        
        // ensure that we always wrap the trace text so it fits horizontally within the visible trace window
        public boolean getScrollableTracksViewportWidth()
        {
            return true;
        }
    };
    
    private Highlighter highlighter;
    private final BatchStyledDocument styledDocument;
    private final Writer outputWriter = new Writer()
    {
        private StringBuilder buffer = new StringBuilder();
        
        @Override
        public void close() throws IOException
        {
        }
        
        @Override
        synchronized public void flush() throws IOException
        {
            synchronized (outputWriter) // synchronized on outer.this like the flush() method
            {
                String input = buffer.toString();
                if(highlightImmediately.get())
                {
                    styledDocument.takeBatchUpdate(input, enableHighlighting.get());
                }
                else
                {
                    styledDocument.takeDelayedBatchUpdate(input, enableHighlighting.get());
                }
                buffer.setLength(0);
                
                synchronized (limitLock)
                {
                    styledDocument.trim(limit, limitTolerance);
                }
                
                // Handling scroll lock setting
                Runnable runnable = () -> {
                    if(scrollLock)
                    {
                        // Scroll to the end
                        int length = outputWindow.getDocument().getLength();
                        if(length > 0)
                        {
                            // The design of the StyledDocument synchronization is weird
                            // they provide the Position class which should be safe to use
                            // from other threads but then all the interface method's take
                            // bare integers which sounds like you are just asking for trouble
                            try
                            {
                                styledDocument.documentWriteLock();
                                outputWindow.setCaretPosition(styledDocument.getLength());
                            }
                            finally
                            {
                                styledDocument.documentWriteUnlock();
                            }
                        }
                    }
                };
                
                // On close, this gets purged from the EDT which will cause a deadlock. -ACN
                if(SwingUtilities.isEventDispatchThread())
                {
                    runnable.run();
                }
                else
                {
                    try
                    {
                        SwingUtilities.invokeAndWait(runnable);
                    }
                    catch(InvocationTargetException e)
                    {
                        e.printStackTrace();
                    }
                    catch(InterruptedException e)
                    {
                        // Probably means we're shutting down
                        LOG.info("Interrupted while attempting to scroll to the end for text |{}| which may happen on shutdown", input);
                        Thread.currentThread().interrupt();
                    }
                }
                
            }
        }
        
        @Override
        synchronized public void write(char[] cbuf, int off, int len) throws IOException
        {
            buffer.append(cbuf, off, len);
        }
    };
    
    // Flags for syntax highlighting
    private AtomicBoolean enableHighlighting = new AtomicBoolean(true);
    private AtomicBoolean highlightImmediately = new AtomicBoolean(true);
    
    public TraceView(JSoarDebugger debuggerIn)
    {
        super("trace", "Trace");
        this.debugger = debuggerIn;
        
        highlighter = Highlighter.getInstance(debugger);
        styledDocument = new BatchStyledDocument(highlighter, debugger);
        
        outputWindow.setFont(new Font("Monospaced", Font.PLAIN, (int) (12 * JSoarDebugger.getFontScale())));
        setLimit(getPreferences().getInt(LIMIT, -1));
        enableHighlighting = new AtomicBoolean(getPreferences().getBoolean(ENABLE_HIGHLIGHTING, true));
        highlightImmediately = new AtomicBoolean(getPreferences().getBoolean(HIGHLIGHT_IMMEDIATELY, true));
        scrollLock = getPreferences().getBoolean(SCROLL_LOCK, true);
        setDefaultTextStyle();
        
        reloadSyntax();
        
        outputWindow.addMouseListener(new MouseAdapter()
        {
            
            public void mousePressed(MouseEvent e)
            {
                if(e.isPopupTrigger())
                {
                    showPopupMenu(e);
                }
            }
            
            public void mouseReleased(MouseEvent e)
            {
                if(e.isPopupTrigger())
                {
                    updateSelectionFromMouseEvent(e);
                    showPopupMenu(e);
                }
                else if(SwingUtilities.isLeftMouseButton(e))
                {
                    if(e.getClickCount() == 1)
                    {
                        updateSelectionFromMouseEvent(e);
                    }
                    else if(e.getClickCount() == 2)
                    {
                        handleObjectAction(e);
                    }
                }
            }
        });
        outputWindow.setEditable(false);
        outputWindow.setDocument(styledDocument);
        
        final JSoarVersion version = JSoarVersion.getInstance();
        outputWindow.setText("JSoar " + version + "\n" +
                "https://github.com/soartech/jsoar\n" +
                "Current command interpreter is '" + debugger.getAgent().getInterpreter().getName() + "'\n" +
                "\n" +
                "Right-click for trace options (or use watch command)\n" +
                "Double-click identifiers, wmes, and rule names to drill down\n" +
                "You can paste code (ctrl+v) directly into this window.\n");
        
        debugger.getAgent().getPrinter().pushWriter(outputWriter);
        
        final Trace trace = debugger.getAgent().getTrace();
        trace.disableAll();
        trace.setEnabled(Category.LOADING, true);
        trace.setWatchLevel(1);
        
        final JPanel p = new JPanel(new BorderLayout());
        
        JScrollPane scrollPane = new JScrollPane(outputWindow);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        p.add(scrollPane, BorderLayout.CENTER);
        
        final JPanel bottom = new JPanel(new BorderLayout());
        
        commandPanel = new CommandEntryPanel(debugger);
        bottom.add(commandPanel, BorderLayout.CENTER);
        
        searchPanel = new IncrementalSearchPanel(outputWindow, debugger);
        searchPanel.setSearchText(getPreferences().get(SEARCH, ""));
        
        bottom.add(searchPanel, BorderLayout.EAST);
        p.add(bottom, BorderLayout.SOUTH);
        
        addAction(new CButton("Clear", Images.CLEAR)
        {
            @Override
            protected void action()
            {
                clear();
            }
        });
        
        getContentPane().add(p);
        
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.debugger.Disposable#dispose()
     */
    public void dispose()
    {
        commandPanel.dispose();
        
        final Printer printer = debugger.getAgent().getPrinter();
        printer.removePersistentWriter(outputWriter);
        
        // todo - reimplement line wrap
        // getPreferences().putBoolean("wrap", outputWindow.getLineWrap());
        getPreferences().put(SEARCH, searchPanel.getSearchText());
        getPreferences().putInt(LIMIT, limit);
        getPreferences().putBoolean(SCROLL_LOCK, scrollLock);
        getPreferences().putBoolean(ENABLE_HIGHLIGHTING, enableHighlighting.get());
        getPreferences().putBoolean(HIGHLIGHT_IMMEDIATELY, highlightImmediately.get());
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.debugger.AbstractAdaptableView#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(Class<?> klass)
    {
        if(klass.equals(SelectionProvider.class))
        {
            return selectionProvider;
        }
        return super.getAdapter(klass);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.debugger.AbstractAdaptableView#activate()
     */
    @Override
    public void activate()
    {
        commandPanel.giveFocus();
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.debugger.AbstractAdaptableView#getShortcutKey()
     */
    @Override
    public String getShortcutKey()
    {
        return "ctrl T";
    }
    
    /**
     * Clear the trace window. This method may be called from any thread.
     */
    public void clear()
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(this::clear);
        }
        else
        {
            outputWindow.setText("");
        }
    }
    
    /**
     * Set the limit on the number of characters saved in the trace. When
     * {@code limit +20%} is reached, the beginning 20% of the trace buffer
     * will be removed.
     *
     * @param limit the limit, in number of characters, or {@code -1} for no limit
     */
    public void setLimit(int limit)
    {
        // output limit code above is synchronized on the outputWriter
        synchronized (limitLock)
        {
            this.limit = limit;
            if(this.limit > 0)
            {
                this.limitTolerance = (int) (this.limit * 0.2);
            }
        }
    }
    
    private SelectedObject getObjectAtPoint(Point p)
    {
        int offset = outputWindow.viewToModel2D(p);
        if(offset < 0)
        {
            return null;
        }
        final ParseSelectedText pst = new ParseSelectedText(outputWindow.getText(), offset);
        final SelectedObject object = pst.getParsedObject(debugger);
        
        return object;
    }
    
    private void updateSelectionFromMouseEvent(MouseEvent e)
    {
        final SelectedObject object = getObjectAtPoint(e.getPoint());
        if(object == null)
        {
            return;
        }
        
        final Callable<Object> call = () -> {
            return object.retrieveSelection(debugger);
        };
        
        final CompletionHandler<Object> finish = result -> {
            selectionProvider.setSelection(result);
        };
        
        debugger.getAgent().execute(call, SwingCompletionHandler.newInstance(finish));
    }
    
    private void handleObjectAction(MouseEvent e)
    {
        final SelectedObject object = getObjectAtPoint(e.getPoint());
        if(object == null)
        {
            return;
        }
        
        final Callable<Void> call = () -> {
            
            final Object o = object.retrieveSelection(debugger);
            final String command;
            if(o instanceof Identifier)
            {
                command = "print " + o;
            }
            else if(o instanceof Production)
            {
                command = "print \"" + ((Production) o).getName() + "\"";
            }
            else if(o instanceof Wme)
            {
                final Wme w = (Wme) o;
                if(w.getValue().asIdentifier() != null)
                {
                    command = "print " + w.getValue();
                }
                else
                {
                    command = null;
                }
            }
            else
            {
                command = null;
            }
            
            if(command != null)
            {
                debugger.getAgent().getPrinter().startNewLine();
                debugger.getAgent().getInterpreter().eval(command);
            }
            return null;
        };
        
        debugger.getAgent().execute(call, null);
    }
    
    @SuppressWarnings("serial")
    private void showPopupMenu(MouseEvent e)
    {
        final TraceMenu menu = new TraceMenu(debugger.getAgent().getTrace());
        
        menu.populateMenu();
        
        menu.insertSeparator(0);
        
        // Add trace limit action
        menu.insert(new AbstractAction("Limit trace output ...")
        {
            
            private static final long serialVersionUID = 3871607368064705900L;
            
            @Override
            public void actionPerformed(ActionEvent e)
            {
                askForTraceLimit();
            }
        }, 0);
        
        final JCheckBoxMenuItem scrollLockItem = new JCheckBoxMenuItem("Scroll lock", scrollLock);
        scrollLockItem.addActionListener(e1 -> scrollLock = !scrollLock);
        
        menu.insert(scrollLockItem, 0);
        
        // Adding various coloring options to a sub menu
        final JMenu highlightingMenu = new JMenu("Syntax Highlighting");
        
        ButtonGroup buttonGroup = new ButtonGroup();
        JRadioButtonMenuItem noHLItem = new JRadioButtonMenuItem("No Highlighting");
        noHLItem.addActionListener(e1 -> {
            enableHighlighting.set(false);
            noHLItem.setSelected(true);
        });
        JRadioButtonMenuItem immediateHLItem = new JRadioButtonMenuItem("Highlight Immediately");
        immediateHLItem.addActionListener(e1 -> {
            enableHighlighting.set(true);
            highlightImmediately.set(true);
            immediateHLItem.setSelected(true);
        });
        JRadioButtonMenuItem delayedHLItem = new JRadioButtonMenuItem("Highlight In Separate Thread");
        delayedHLItem.addActionListener(e1 -> {
            enableHighlighting.set(true);
            highlightImmediately.set(false);
            delayedHLItem.setSelected(true);
        });
        
        highlightingMenu.add(noHLItem);
        highlightingMenu.add(immediateHLItem);
        highlightingMenu.add(delayedHLItem);
        buttonGroup.add(noHLItem);
        buttonGroup.add(immediateHLItem);
        buttonGroup.add(delayedHLItem);
        
        // Setting highlight control state initially
        if(enableHighlighting.get())
        {
            if(highlightImmediately.get())
            {
                immediateHLItem.setSelected(true);
            }
            else
            {
                delayedHLItem.setSelected(true);
            }
        }
        else
        {
            noHLItem.setSelected(true);
        }
        
        menu.insert(highlightingMenu, 0);
        
        menu.insert(new AbstractAction("Edit Syntax Highlighting")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                new SyntaxConfigurator(highlighter.getPatterns(), TraceView.this, debugger).go();
            }
        }, 0);
        
        // Add clear action
        menu.insert(new AbstractAction("Clear")
        {
            
            private static final long serialVersionUID = 3871607368064705900L;
            
            @Override
            public void actionPerformed(ActionEvent e)
            {
                clear();
            }
        }, 0);
        
        final int offset = outputWindow.viewToModel2D(e.getPoint());
        if(offset >= 0)
        {
            final ParseSelectedText pst = new ParseSelectedText(outputWindow.getText(), offset);
            final SelectedObject object = pst.getParsedObject(debugger);
            if(object != null)
            {
                menu.addSeparator();
                object.fillMenu(debugger, menu);
            }
        }
        
        // Show the menu
        menu.getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
    }
    
    private void askForTraceLimit()
    {
        final String result = JOptionPane.showInputDialog(outputWindow, "Trace limit in characters (-1 for no limit)", limit);
        if(result != null)
        {
            try
            {
                final int newLimit = Integer.valueOf(result);
                setLimit(newLimit >= 0 ? newLimit : -1);
            }
            catch(NumberFormatException e)
            {
                // Do nothing
            }
        }
    }
    
    private void executePastedInput()
    {
        final Clipboard cb = outputWindow.getToolkit().getSystemClipboard();
        final Transferable t = cb.getContents(null);
        if(t.isDataFlavorSupported(DataFlavor.stringFlavor))
        {
            try
            {
                final String text = (String) t.getTransferData(DataFlavor.stringFlavor);
                debugger.getAgent().execute(new CommandLineRunnable(debugger, text), null);
            }
            catch(UnsupportedFlavorException | IOException ignored)
            {
                // Do nothing
            }
        }
    }
    
    public void saveSyntax()
    {
        highlighter.save();
        reformatText();
    }
    
    public void reformatText()
    {
        this.styledDocument.reformatText();
    }
    
    public void setDefaultTextStyle()
    {
        highlighter = Highlighter.getInstance(debugger);
        highlighter.setDefaultTextStyle(outputWindow);
    }
    
    public SyntaxSettings reloadSyntaxDefaults()
    {
        SyntaxSettings patterns = highlighter.reloadSyntaxDefaults();
        
        setDefaultTextStyle();
        return patterns;
    }
    
    public void reloadSyntax()
    {
        highlighter.reloadSyntax();
    }
    
    private class Provider implements SelectionProvider
    {
        SelectionManager manager;
        List<Object> selection = new ArrayList<Object>();
        
        public void setSelection(Object o)
        {
            selection.clear();
            if(o != null)
            {
                selection.add(o);
            }
            if(manager != null)
            {
                manager.fireSelectionChanged();
            }
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see org.jsoar.debugger.selection.SelectionProvider#activate(org.jsoar.debugger.selection.SelectionManager)
         */
        @Override
        public void activate(SelectionManager manager)
        {
            this.manager = manager;
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see org.jsoar.debugger.selection.SelectionProvider#deactivate()
         */
        @Override
        public void deactivate()
        {
            this.manager = null;
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see org.jsoar.debugger.selection.SelectionProvider#getSelectedObject()
         */
        @Override
        public Object getSelectedObject()
        {
            return !selection.isEmpty() ? selection.get(0) : null;
        }
        
        /*
         * (non-Javadoc)
         * 
         * @see org.jsoar.debugger.selection.SelectionProvider#getSelection()
         */
        @Override
        public List<Object> getSelection()
        {
            return selection;
        }
        
    }
}

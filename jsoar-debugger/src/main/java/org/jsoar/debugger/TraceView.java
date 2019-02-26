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
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import javax.swing.text.Document;
import javax.swing.text.Position;

import org.jsoar.debugger.ParseSelectedText.SelectedObject;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.debugger.selection.SelectionProvider;
import org.jsoar.debugger.syntax.Highlighter;
import org.jsoar.debugger.syntax.StyleOffset;
import org.jsoar.debugger.syntax.SyntaxSettings;
import org.jsoar.debugger.syntax.ui.SyntaxConfigurator;
import org.jsoar.kernel.JSoarVersion;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.SwingCompletionHandler;
import org.jsoar.util.IncrementalSearchPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bibliothek.gui.dock.common.action.CButton;

/**
 * @author ray
 */
public class TraceView extends AbstractAdaptableView implements Disposable
{
    private static final Logger logger = LoggerFactory.getLogger(TraceView.class);
	
    private final JSoarDebugger debugger;
    private final CommandEntryPanel commandPanel;
    private final Provider selectionProvider = new Provider();

    private final IncrementalSearchPanel searchPanel;

    private int limit = -1;
    private int limitTolerance = 0;
    private boolean scrollLock = true;

    private final JTextPane outputWindow = new JTextPane() {
        private static final long serialVersionUID = 5161494134278464101L;

        /* (non-Javadoc)
         * @see javax.swing.text.JTextComponent#paste()
         */
        public void paste()
        {
            executePastedInput();
        }
    };

    private Highlighter highlighter;
    private final BatchStyledDocument styledDocument;
    private static final char[] EOL_ARRAY = { '\n' };
    private final Writer outputWriter = new Writer()
    {
        private long lastFlush;
        private StringBuilder buffer = new StringBuilder();
//        private volatile boolean flushing = false;
        //@SuppressWarnings("unused")
        //private volatile boolean printing = false;

        @Override
        public void close() throws IOException
        {
        }
        
        private final ConcurrentLinkedQueue<String> inputs = new ConcurrentLinkedQueue<>();
        
        private final Semaphore uiThreadLock = new Semaphore(1);
        @Override
        synchronized public void flush() throws IOException
        {
            if (colorImmediately.get()) {
            	synchronized (outputWriter) // synchronized on outer.this like the flush() method
                {
            		String input = buffer.toString();
//            		for(String splitInput: input.split("[\\r\\n]+")) {
//            			inputs.offer(splitInput);
            			styledDocument.takeDelayedBatchUpdate(buffer.toString(), outputWindow, scrollLock);
//            		}
            		buffer.setLength(0);
                }
            } else {
            	while(true) {
	            	if(uiThreadLock.tryAcquire()) {
						break;
					}else {
						return;
					}
            	}
            	SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        synchronized (outputWriter) // synchronized on outer.this like the flush() method
                        {
							try {
								Position endPosition = outputWindow.getDocument().getEndPosition();

								try {
									String str = buffer.toString();
									outputWindow.getDocument().insertString(endPosition.getOffset() - 1, str,
											highlighter.getDefaultAttributes());
									// outputWindow.getDocument().insertString(endPosition.getOffset()-1, "\r\n",
									// defaultAttributes);
								} catch (BadLocationException e) {
									e.printStackTrace();
								}
								buffer.setLength(0);

								trimOutput(outputWindow.getDocument());

								if (scrollLock) {
									// Scroll to the end
									outputWindow.setCaretPosition(outputWindow.getDocument().getLength());
								}
							} finally {
								uiThreadLock.release();
							}
                            
                        }
                    }
                });

                //delayed syntax highlighting
				lastFlush = System.currentTimeMillis();
				Thread lateHighlightThread = new Thread() {
					public void run() {
						final long pauseInterval = 200;
						try {
							Thread.sleep(pauseInterval);
						} catch (InterruptedException ignored) {
						}
						if (!colorImmediately.get() && System.currentTimeMillis() - lastFlush >= pauseInterval) {
							synchronized (colorLock) {
								reformatText();
							}
						}
					}
				};
				lateHighlightThread.setDaemon(true);
				lateHighlightThread.start();
            }

        }

        @Override
        synchronized public void write(char[] cbuf, int off, int len) throws IOException
        {
            buffer.append(cbuf, off, len);
        }
    };
    private AtomicBoolean colorImmediately = new AtomicBoolean(true);
    private final Object colorLock = new Object(); 

    private void trimOutput(Document document) {
        if (limit > 0) {
            final int length = document.getLength();
            if (length > limit + limitTolerance) {
                try {
                    // Trim the trace back down to limit
                    document.remove(0, length - limit);
                } catch (BadLocationException e) {
                }
            }
        }
    }


    public TraceView(JSoarDebugger debuggerIn)
    {
        super("trace", "Trace");
        this.debugger = debuggerIn;

        highlighter = Highlighter.getInstance(debugger);
        styledDocument = new BatchStyledDocument(highlighter, debugger, colorImmediately, colorLock);
        
        outputWindow.setFont(new Font("Monospaced", Font.PLAIN, (int) (12 * JSoarDebugger.getFontScale())));
        setLimit(getPreferences().getInt("limit", -1));
        colorImmediately = new AtomicBoolean(getPreferences().getBoolean("coloredOutput", true));
        scrollLock = getPreferences().getBoolean("scrollLock", true);
        setDefaultTextStyle();



        reloadSyntax();

        //todo - re-implement word wrap
        //        outputWindow.setLineWrap(getPreferences().getBoolean("wrap", true));
        outputWindow.addMouseListener(new MouseAdapter() {

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
            }});
        outputWindow.setEditable(false);
        outputWindow.setDocument(styledDocument);

        final JSoarVersion version = JSoarVersion.getInstance();
        outputWindow.setText("JSoar " + version + "\n" +
                             "http://jsoar.googlecode.com\n" +
                             "Current command interpreter is '" + debugger.getAgent().getInterpreter().getName() + "'\n" +
                             "\n" +
                             "Right-click for trace options (or use watch command)\n" +
                             "Double-click identifiers, wmes, and rule names to drill down\n" +
                             "You can paste code (ctrl+v) directly into this window.\n");


        debugger.getAgent().getPrinter().addPersistentWriter(outputWriter);


        final Trace trace = debugger.getAgent().getTrace();
        trace.disableAll();
        trace.setEnabled(Category.LOADING, true);
        trace.setWatchLevel(1);

        final JPanel p = new JPanel(new BorderLayout());

        p.add(new JScrollPane(outputWindow), BorderLayout.CENTER);

        final JPanel bottom = new JPanel(new BorderLayout());

        commandPanel = new CommandEntryPanel(debugger);
        bottom.add(commandPanel, BorderLayout.CENTER);

        searchPanel = new IncrementalSearchPanel(outputWindow, debugger);
        searchPanel.setSearchText(getPreferences().get("search", ""));

        bottom.add(searchPanel, BorderLayout.EAST);
        p.add(bottom, BorderLayout.SOUTH);

        addAction(new CButton("Clear", Images.CLEAR) {
            @Override
            protected void action()
            {
                clear();
            }
        });


        getContentPane().add(p);

    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.Disposable#dispose()
     */
    public void dispose()
    {
        commandPanel.dispose();

        final Printer printer = debugger.getAgent().getPrinter();
        printer.removePersistentWriter(outputWriter);

        //todo - reimplement line wrap
//        getPreferences().putBoolean("wrap", outputWindow.getLineWrap());
        getPreferences().put("search", searchPanel.getSearchText());
        getPreferences().putInt("limit", limit);
        getPreferences().putBoolean("scrollLock", scrollLock);
        getPreferences().putBoolean("coloredOutput", colorImmediately.get());
    }

    /* (non-Javadoc)
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

    /* (non-Javadoc)
     * @see org.jsoar.debugger.AbstractAdaptableView#activate()
     */
    @Override
    public void activate()
    {
        commandPanel.giveFocus();
    }

    /* (non-Javadoc)
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
            SwingUtilities.invokeLater(new Runnable() { public void run() { clear(); } });
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
        synchronized(outputWriter)
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
        int offset = outputWindow.viewToModel(p);
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
        final Callable<Object> call = new Callable<Object>() {

            @Override
            public Object call() throws Exception
            {
                return object.retrieveSelection(debugger);
            }};
        final CompletionHandler<Object> finish = new CompletionHandler<Object>() {

            @Override
            public void finish(Object result)
            {
                selectionProvider.setSelection(result);
            }};
        debugger.getAgent().execute(call, SwingCompletionHandler.newInstance(finish));
    }

    private void handleObjectAction(MouseEvent e)
    {
        final SelectedObject object = getObjectAtPoint(e.getPoint());
        if(object == null)
        {
            return;
        }
        final Callable<Void> call = new Callable<Void>() {

            @Override
            public Void call() throws Exception
            {
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
                    debugger.getAgent().getInterpreter().eval(command);
                }
                return null;
            }};
        debugger.getAgent().execute(call, null);
    }

    @SuppressWarnings("serial")
    private void showPopupMenu(MouseEvent e)
    {
        final TraceMenu menu = new TraceMenu(debugger.getAgent().getTrace());

        menu.populateMenu();

        menu.insertSeparator(0);

        // Add trace limit action
        menu.insert(new AbstractAction("Limit trace output ...") {

            private static final long serialVersionUID = 3871607368064705900L;

            @Override
            public void actionPerformed(ActionEvent e)
            {
                askForTraceLimit();
            }}, 0);

        // Add Wrap text action
        final JCheckBoxMenuItem scrollLockItem = new JCheckBoxMenuItem("Scroll lock", scrollLock);
        scrollLockItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                scrollLock = !scrollLock;
            }});
        menu.insert(scrollLockItem, 0);

        final JCheckBoxMenuItem colorItem = new JCheckBoxMenuItem("Color Output Immediately (slow)", colorImmediately.get());
        colorItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	//Technically this isn't atomic, but if it's changing faster that this we have bigger problems
                colorImmediately.set(!colorImmediately.get());
            }
        });
        menu.insert(colorItem,0);

        menu.insert(new AbstractAction("Edit Syntax Highlighting") {
            @Override
            public void actionPerformed(ActionEvent e) {
                new SyntaxConfigurator(highlighter.getPatterns(),TraceView.this, debugger).go();
            }
        },0);

        // Add Wrap text action
        //todo - reimplement line wrap
        /*final JCheckBoxMenuItem wrapTextItem = new JCheckBoxMenuItem("Wrap text", outputWindow.getLineWrap());
        wrapTextItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                outputWindow.setLineWrap(!outputWindow.getLineWrap());
            }});
        menu.insert(wrapTextItem, 0);*/

        // Add clear action
        menu.insert(new AbstractAction("Clear") {

            private static final long serialVersionUID = 3871607368064705900L;

            @Override
            public void actionPerformed(ActionEvent e)
            {
                clear();
            }}, 0);



        final int offset = outputWindow.viewToModel(e.getPoint());
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
            catch (UnsupportedFlavorException | IOException ignored)
            {
                // Do nothing
            }
        }
    }

    public void saveSyntax() {
        highlighter.save();
        reformatText();
    }

    public void reformatText() {
        highlighter.formatText(outputWindow);
    }

    public void setDefaultTextStyle()
    {
        highlighter = Highlighter.getInstance(debugger);
        highlighter.setDefaultTextStyle(outputWindow);
    }

    public SyntaxSettings reloadSyntaxDefaults(){
        SyntaxSettings patterns = highlighter.reloadSyntaxDefaults();

        setDefaultTextStyle();
        return patterns;
    }

    public void reloadSyntax() {
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

        /* (non-Javadoc)
         * @see org.jsoar.debugger.selection.SelectionProvider#activate(org.jsoar.debugger.selection.SelectionManager)
         */
        @Override
        public void activate(SelectionManager manager)
        {
            this.manager = manager;
        }

        /* (non-Javadoc)
         * @see org.jsoar.debugger.selection.SelectionProvider#deactivate()
         */
        @Override
        public void deactivate()
        {
            this.manager = null;
        }

        /* (non-Javadoc)
         * @see org.jsoar.debugger.selection.SelectionProvider#getSelectedObject()
         */
        @Override
        public Object getSelectedObject()
        {
            return !selection.isEmpty() ? selection.get(0) : null;
        }

        /* (non-Javadoc)
         * @see org.jsoar.debugger.selection.SelectionProvider#getSelection()
         */
        @Override
        public List<Object> getSelection()
        {
            return selection;
        }

    }
}

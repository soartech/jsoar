/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.jsoar.debugger.ParseSelectedText.SelectedObject;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.debugger.selection.SelectionProvider;
import org.jsoar.kernel.JSoarVersion;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.SwingCompletionHandler;
import org.jsoar.util.IncrementalSearchPanel;

/**
 * @author ray
 */
public class TraceView extends AbstractAdaptableView implements Disposable
{
    private static final long serialVersionUID = -358416409712991384L;

    private final JSoarDebugger debugger;
    private final CommandEntryPanel commandPanel;
    private final Provider selectionProvider = new Provider();

    private final IncrementalSearchPanel searchPanel;

    private final JTextArea outputWindow = new JTextArea();
    private final Writer outputWriter = new Writer()
    {
        private StringBuilder buffer = new StringBuilder();
        private boolean flushing = false;
        
        @Override
        public void close() throws IOException
        {
        }

        @Override
        synchronized public void flush() throws IOException
        {
            // If there's already a runnable headed for the UI thread, don't send another
            if(flushing) { return; }
            
            // Send a runnable over to the UI thread to take the current buffer contents
            // and put them in the trace window.
            flushing = true;
            
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    synchronized(outputWriter) // synchronized on outer.this like the flush() method
                    {
                        outputWindow.append(buffer.toString());
                        outputWindow.setCaretPosition(outputWindow.getDocument().getLength());
                        buffer.setLength(0);
                        flushing = false;
                    }
                }
            });
        }

        @Override
        synchronized public void write(char[] cbuf, int off, int len) throws IOException
        {
            buffer.append(cbuf, off, len);
        }
    };

    public TraceView(JSoarDebugger debuggerIn)
    {
        super("trace", "Trace");
        this.debugger = debuggerIn;
        
        outputWindow.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputWindow.setLineWrap(getPreferences().getBoolean("wrap", true));
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
                    showPopupMenu(e);
                }
                else if(SwingUtilities.isLeftMouseButton(e))
                {
                    updateSelectionOnLeftClick(e);
                }
            }});
        outputWindow.setEditable(false);
        
        final JSoarVersion version = JSoarVersion.getInstance();
        outputWindow.setText("JSoar " + version + "\nhttp://jsoar.googlecode.com\n\n" +  
                             "Right-click for trace options\n");
        
        debugger.getAgent().getPrinter().pushWriter(outputWriter);
        
        final Trace trace = debugger.getAgent().getTrace();
        trace.disableAll();
        trace.setEnabled(Category.LOADING, true);
        trace.setWatchLevel(1);
        
        final JPanel p = new JPanel(new BorderLayout());
        p.add(new JScrollPane(outputWindow), BorderLayout.CENTER);
        
        final JPanel bottom = new JPanel(new BorderLayout());
        
        commandPanel = new CommandEntryPanel(debugger);
        bottom.add(commandPanel, BorderLayout.CENTER);
        
        searchPanel = new IncrementalSearchPanel(outputWindow);
        searchPanel.setSearchText(getPreferences().get("search", ""));
        
        bottom.add(searchPanel, BorderLayout.EAST);
        p.add(bottom, BorderLayout.SOUTH);
        
        this.setContentPane(p);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.debugger.Disposable#dispose()
     */
    public void dispose()
    {
        commandPanel.dispose();
        
        final Printer printer = debugger.getAgent().getPrinter();
        while(outputWriter != printer.popWriter())
        {
        }
        
        getPreferences().putBoolean("wrap", outputWindow.getLineWrap());
        getPreferences().put("search", searchPanel.getSearchText());
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

    private void updateSelectionOnLeftClick(MouseEvent e)
    {
        int offset = outputWindow.viewToModel(e.getPoint());
        if(offset < 0)
        {
            return;
        }
        final ParseSelectedText pst = new ParseSelectedText(outputWindow.getText(), offset);
        final SelectedObject object = pst.getParsedObject(debugger);
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
    
    private void showPopupMenu(MouseEvent e)
    {
        final TraceMenu menu = new TraceMenu(debugger.getAgent().getTrace());
        
        menu.populateMenu();
        
        menu.insertSeparator(0);
        
        // Add Wrap text action
        final JCheckBoxMenuItem wrapTextItem = new JCheckBoxMenuItem("Wrap text", outputWindow.getLineWrap());
        wrapTextItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                outputWindow.setLineWrap(!outputWindow.getLineWrap());
            }});
        menu.insert(wrapTextItem, 0);
        
        // Add clear action
        menu.insert(new AbstractAction("Clear") {

            private static final long serialVersionUID = 3871607368064705900L;

            @Override
            public void actionPerformed(ActionEvent e)
            {
                outputWindow.setText("");
            }}, 0);
        
        final int offset = outputWindow.viewToModel(e.getPoint());
        SelectedObject object = null;
        if(offset >= 0)
        {
            final ParseSelectedText pst = new ParseSelectedText(outputWindow.getText(), offset);
            object = pst.getParsedObject(debugger);
            if(object != null)
            {
                menu.addSeparator();
                object.fillMenu(debugger, menu);
            }
        }
        
        // Show the menu
        menu.getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
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

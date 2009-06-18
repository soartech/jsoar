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

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.flexdock.docking.DockingConstants;
import org.jsoar.kernel.JSoarVersion;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.Category;

/**
 * @author ray
 */
public class TraceView extends AbstractAdaptableView implements Disposable
{
    private static final long serialVersionUID = -358416409712991384L;

    private final JSoarDebugger debugger;
    private final CommandEntryPanel commandPanel;
    
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

            public void mousePressed(MouseEvent e) { mouseReleased(e); }

            public void mouseReleased(MouseEvent e)
            {
                if(e.isPopupTrigger())
                {
                    showPopupMenu(e);
                }
            }});
        outputWindow.setEditable(false);
        
        final JSoarVersion version = JSoarVersion.getInstance();
        outputWindow.setText("JSoar " + version + "\nhttp://jsoar.googlecode.com\n\n" +  
                             "Right-click for trace options\n");
        
        debugger.getAgent().getPrinter().pushWriter(outputWriter, true);
        
        final Trace trace = debugger.getAgent().getTrace();
        trace.disableAll();
        trace.setEnabled(Category.LOADING, true);
        trace.setWatchLevel(1);
        
        this.addAction(DockingConstants.PIN_ACTION);

        final JPanel p = new JPanel(new BorderLayout());
        //p.add(new RunControlPanel(debugger), BorderLayout.NORTH);
        p.add(new JScrollPane(outputWindow), BorderLayout.CENTER);
        
        commandPanel = new CommandEntryPanel(debugger);
        p.add(commandPanel, BorderLayout.SOUTH);
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
        
        // Show the menu
        menu.getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
    }
}

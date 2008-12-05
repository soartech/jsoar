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

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.flexdock.docking.DockingConstants;

/**
 * @author ray
 */
public class TraceView extends AbstractAdaptableView
{
    private static final long serialVersionUID = -358416409712991384L;

    private final LittleDebugger debugger;
    
    private JTextArea outputWindow = new JTextArea();
    private Writer outputWriter = new Writer()
    {
        private StringBuilder buffer = new StringBuilder();
        
        @Override
        public void close() throws IOException
        {
        }

        @Override
        synchronized public void flush() throws IOException
        {
            final String output = buffer.toString();
            buffer = new StringBuilder();

            if(output.length() > 0)
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        outputWindow.append(output);
                        outputWindow.setCaretPosition(outputWindow.getDocument().getLength());
                    }
                });
            }
        }

        @Override
        synchronized public void write(char[] cbuf, int off, int len) throws IOException
        {
            buffer.append(cbuf, off, len);
        }
    };
    private JTextField commandField = new JTextField();

    public TraceView(LittleDebugger debuggerIn)
    {
        super("trace", "Trace");
        this.debugger = debuggerIn;
        
        outputWindow.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        outputWindow.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) { mouseReleased(e); }

            public void mouseReleased(MouseEvent e)
            {
                if(e.isPopupTrigger())
                {
                    TraceMenu menu = new TraceMenu(debugger.getAgentProxy().getAgent().getTrace());
                    menu.populateMenu();
                    menu.getPopupMenu().show(e.getComponent(), e.getX(), e.getY());
                }
            }});
        debugger.getAgentProxy().getAgent().getPrinter().pushWriter(outputWriter, true);
        debugger.getAgentProxy().getAgent().trace.enableAll();
        
        this.addAction(DockingConstants.PIN_ACTION);

        JPanel p = new JPanel(new BorderLayout());
        //p.add(new RunControlPanel(debugger), BorderLayout.NORTH);
        p.add(new JScrollPane(outputWindow), BorderLayout.CENTER);
        
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(commandField, BorderLayout.SOUTH);
        commandField.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                debugger.getAgentProxy().execute(new CommandLineRunnable(debugger, commandField.getText()));
            }});
        p.add(bottom, BorderLayout.SOUTH);
        this.setContentPane(p);
    }
}

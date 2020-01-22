/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 1, 2009
 */
package org.jsoar.runtime;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.JSoarVersion;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.events.AfterDecisionCycleEvent;
import org.jsoar.kernel.io.CycleCountInput;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;

/**
 * An applet with a Soar agent and a simple console.
 * 
 * @author ray
 */
public class TinyDebugger extends JApplet
{
    private static final long serialVersionUID = 3028131188835230802L;
    private ThreadedAgent agent;
    private int sleepCounter = 0;
    
    private final JPanel tracePanel = new JPanel(new BorderLayout());
    private final JTextArea trace = new JTextArea();
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private final DefaultComboBoxModel promptModel = new DefaultComboBoxModel(new Object[] { 
          "source http://darevay.com/jsoar/waterjugs.soar",
          "source http://darevay.com/jsoar/towers.soar",
          "run -d 1", "run", "stats", "p s1", "stop-soar", "init-soar", "waitsnc --on" });
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private final JComboBox prompt = new JComboBox(promptModel);
    
    private final JPanel productionPanel = new JPanel(new BorderLayout());
    private final JTextArea productionEditor = new JTextArea();
    
    private final JTabbedPane tabs = new JTabbedPane();
    
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
            final String output = buffer.toString();
            buffer.setLength(0);

            if(output.length() > 0)
            {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        trace.append(output);
                        final int length = trace.getDocument().getLength();
                        // Limit output to around 10000 chars (applets have little memory)
                        if(length > 12000)
                        {
                            try
                            {
                                trace.getDocument().remove(0, length - 10000);
                            }
                            catch (BadLocationException e) {}
                        }
                        trace.setCaretPosition(trace.getDocument().getLength());
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
    
    /* (non-Javadoc)
     * @see java.applet.Applet#init()
     */
    @Override
    public void init()
    {
        agent = ThreadedAgent.create();
        agent.getPrinter().pushWriter(outputWriter);
        new CycleCountInput(agent.getInputOutput());
        
        agent.getEvents().addListener(AfterDecisionCycleEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                // Put in a little sleep every 100 decisions. Otherwise, the agent thread 
                // kills the UI thread.
                sleepCounter = (sleepCounter + 1) % 100;
                if(sleepCounter == 0)
                {
                    try { Thread.sleep(25); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
            }});
        
        try
        {
            SwingUtilities.invokeAndWait(new Runnable() {

               @Override
               public void run()
               {
                  createUi();
               }});
        }
        catch(Exception e)
        {
           System.err.println("Failed to initialize: " + e);
        }
    }

    /* (non-Javadoc)
     * @see java.applet.Applet#destroy()
     */
    @Override
    public void destroy()
    {
        final Agent inner = agent.getAgent();
        agent.detach();
        inner.dispose();
    }

    /* (non-Javadoc)
     * @see java.applet.Applet#stop()
     */
    @Override
    public void stop()
    {
        agent.stop();
    }
    
    private void createUi()
    {
        // Set up trace panel
        trace.setFont(new Font("Monospaced", Font.PLAIN, 12));
        trace.setEditable(false);
        JSoarVersion version = JSoarVersion.getInstance();
        trace.setText("jsoar " + version + "\nhttps://github.com/soartech/jsoar\n\n");
        tracePanel.add(new JScrollPane(trace), BorderLayout.CENTER);
        
        final JPanel promptPanel = new JPanel(new BorderLayout());
        
        prompt.setEditable(true);
        prompt.getEditor().addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                executeCommand();
            }});
        promptPanel.add(prompt, BorderLayout.CENTER);
        promptPanel.add(new JButton(new AbstractAction("Stop!") {
            private static final long serialVersionUID = -8284340874152313700L;

            @Override
            public void actionPerformed(ActionEvent e)
            {
                agent.stop();
            }}), BorderLayout.EAST);
        
        tracePanel.add(promptPanel, BorderLayout.SOUTH);
        
        tabs.addTab("Trace", tracePanel);
        
        // Set up production panel
        productionEditor.setFont(new Font("Monospaced", Font.PLAIN, 12));
        productionEditor.setText("# Enter production(s) here then click 'Load'\n" +
        		"sp {hello*world\n" +
        		"  (state <s> ^superstate nil)\n" +
        		"-->\n" +
        		"  (write (crlf) |Hello, world|)\n" +
        		"}\n" +
        		"");
        productionPanel.add(new JScrollPane(productionEditor), BorderLayout.CENTER);
        productionPanel.add(new JButton(new AbstractAction("Load") {

            private static final long serialVersionUID = -8284340874152313700L;

            @Override
            public void actionPerformed(ActionEvent e)
            {
                loadProduction();
                
            }}), BorderLayout.SOUTH);
        
        tabs.addTab("Productions", productionPanel);
        
        setContentPane(tabs);
        
        agent.getPrinter().flush();
    }

    private void loadProduction()
    {
        final String command = productionEditor.getText();
        final Callable<Void> callable = new Callable<Void>() {

            @Override
            public Void call() throws Exception
            {
                try
                {
                    agent.getInterpreter().eval(command);
                }
                catch (SoarException e)
                {
                    agent.getPrinter().print(e.getMessage()).flush();
                }
                return null;
            }};
        
        agent.execute(callable, null);
        tabs.setSelectedComponent(tracePanel);
    }
    
    @SuppressWarnings("unchecked")
    private void executeCommand()
    {
        final String command = prompt.getEditor().getItem().toString().trim();
        if(command.length() == 0) { return; }
        
        final Callable<Void> callable = new Callable<Void>() {

            @Override
            public Void call() throws Exception
            {
                final Printer printer = agent.getPrinter();
                String result;
                try
                {
                    printer.startNewLine().print(command);
                    result = agent.getInterpreter().eval(command);
                }
                catch (SoarException e)
                {
                    result = "\n" + e.getMessage();
                }
                if(result != null && result.length() > 0)
                {
                    printer.print(result);
                }
                printer.flush();
                return null;
            }};
        
        agent.execute(callable, null);
        
        promptModel.removeElement(command);
        promptModel.insertElementAt(command, 0);
        prompt.setSelectedIndex(0);
        prompt.getEditor().selectAll();
    }
}

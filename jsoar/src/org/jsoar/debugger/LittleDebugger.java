/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 21, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.tcl.SoarTclException;
import org.jsoar.tcl.SoarTclInterface;

/**
 * @author ray
 */
public class LittleDebugger extends JPanel
{
    private enum StepType
    {
        phases,
        elaborations,
        decisions,
        output_mods
    }
    
    private Agent agent = new Agent();
    private SoarTclInterface ifc = new SoarTclInterface(agent);
    private ExecutorService pool = Executors.newSingleThreadExecutor();
    
    private JTextField countField = new JTextField("    1");
    private JComboBox stepTypeCombo = new JComboBox(StepType.values());
    {
        stepTypeCombo.setSelectedIndex(0);
    }
    
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
            outputWindow.append(buffer.toString());
            outputWindow.setCaretPosition(outputWindow.getText().length());
            buffer = new StringBuilder();
        }

        @Override
        synchronized public void write(char[] cbuf, int off, int len) throws IOException
        {
            buffer.append(cbuf, off, len);
        }
    };
    
    private class RunCommand implements Runnable
    {
        private StepType type;
        private int count;
        /**
         * @param type
         * @param count
         */
        public RunCommand(StepType type, int count)
        {
            this.type = type;
            this.count = count;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run()
        {
            switch(type)
            {
            case phases:
                agent.decisionCycle.run_for_n_phases(count);
                break;
            case elaborations:
                agent.decisionCycle.run_for_n_elaboration_cycles(count);
                break;
            case decisions:
                agent.decisionCycle.run_for_n_decision_cycles(count);
                break;
            case output_mods:
                agent.decisionCycle.run_for_n_modifications_of_output(count);
                break;
            }
            updateWmes();
        }
    }
    
    private DefaultListModel wmeListModel = new DefaultListModel();
    private JList wmeList = new JList(wmeListModel);
    
    public LittleDebugger()
    {
        super(new BorderLayout());
        
        outputWindow.setFont(new Font("Monospaced", Font.PLAIN, 12));
        agent.getPrinter().setWriter(outputWriter, true);
        agent.trace.enableAll();
        
        agent.initialize();
        
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
                                          new JScrollPane(outputWindow), 
                                          new JScrollPane(wmeList));
        split.setDividerLocation(600);
        add(split, BorderLayout.CENTER);
        
        initToolbar();
        
        updateWmes();
    }
    
    private void initToolbar()
    {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        
        bar.add(new JLabel("Step "));
        bar.add(countField);
        bar.add(stepTypeCombo);
        bar.add(new JButton(new AbstractAction("GO"){

            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                int count = Integer.valueOf(countField.getText().trim());
                StepType type = (StepType) stepTypeCombo.getSelectedItem();
                pool.execute(new RunCommand(type, count));
            }}));
        
        add(bar, BorderLayout.NORTH);
    }
    
    private void updateWmes()
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            try
            {
                SwingUtilities.invokeAndWait(new Runnable() {

                    @Override
                    public void run()
                    {
                        updateWmes();
                    }});
            }
            catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (InvocationTargetException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return;
        }
        wmeListModel.clear();
        
        List<String> wmes = new ArrayList<String>();
        for(Wme wme : agent.rete.getAllWmes())
        {
            wmes.add(String.format("%s", wme));
        }
        Collections.sort(wmes);
        for(String s : wmes)
        {
            wmeListModel.addElement(s);
        }
    }
    
    public static void main(String[] args)
    {
        JFrame frame = new JFrame("Little JSoar Debugger");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        final LittleDebugger littleDebugger = new LittleDebugger();
        frame.setContentPane(littleDebugger);
        frame.setSize(800, 600);
        frame.setVisible(true);
        
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
    }
    
}

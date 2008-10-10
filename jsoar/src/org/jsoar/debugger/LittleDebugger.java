/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 21, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.tcl.SoarTclException;
import org.jsoar.tcl.SoarTclInterface;

/**
 * @author ray
 */
public class LittleDebugger extends JPanel
{
    private static final long serialVersionUID = 7997119112479665988L;

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
            update();
        }
    }
    
    private JFrame frame;
    private DefaultListModel wmeListModel = new DefaultListModel();
    private JList wmeList = new JList(wmeListModel);
    private DefaultListModel prodListModel = new DefaultListModel();
    private JList prodList = new JList(prodListModel);
    
    public LittleDebugger(JFrame frame)
    {
        super(new BorderLayout());
        
        this.frame = frame;
        
        outputWindow.setFont(new Font("Monospaced", Font.PLAIN, 12));
        agent.getPrinter().setWriter(outputWriter, true);
        agent.trace.enableAll();
        
        agent.initialize();
        
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                               new JScrollPane(prodList),
                                               new JScrollPane(wmeList));
        
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
                                          new JScrollPane(outputWindow), 
                                          rightSplit);
        split.setDividerLocation(600);
        add(split, BorderLayout.CENTER);
        
        initMenuBar();
        initToolbar();
        
        update();
        
        prodList.addMouseListener(new MouseAdapter() {

            /* (non-Javadoc)
             * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
             */
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if(e.getClickCount() != 2)
                {
                    return;
                }
                Production p = (Production) prodList.getSelectedValue();
                if(p != null)
                {
                    p.print_production(agent.rete, agent.getPrinter(), SwingUtilities.isLeftMouseButton(e));
                }
            }});
    }
    
    private void exit()
    {
        System.exit(0);
    }
    
    private void initMenuBar()
    {
        JMenuBar bar = new JMenuBar();
        
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new AbstractAction("Exit") {
            private static final long serialVersionUID = -2043372835820538377L;

            @Override
            public void actionPerformed(ActionEvent e)
            {
                exit();
            }});
        
        bar.add(fileMenu);
        bar.add(new TraceMenu(agent.trace));
        
        frame.setJMenuBar(bar);
    }
    
    private void initToolbar()
    {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        
        bar.add(new JLabel("Step "));
        bar.add(countField);
        bar.add(stepTypeCombo);
        bar.add(new JButton(new AbstractAction("GO"){

            private static final long serialVersionUID = -6058718638062761141L;

            @Override
            public void actionPerformed(ActionEvent arg0)
            {
                int count = Integer.valueOf(countField.getText().trim());
                StepType type = (StepType) stepTypeCombo.getSelectedItem();
                pool.execute(new RunCommand(type, count));
            }}));
        
        add(bar, BorderLayout.NORTH);
    }
    private void update()
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            try
            {
                SwingUtilities.invokeAndWait(new Runnable() {

                    @Override
                    public void run()
                    {
                        update();
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
        
        updateWmes();
        updateProds();
    }
    private void updateWmes()
    {
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
    
    private void updateProds()
    {
        prodListModel.clear();
        List<Production> prods = agent.getProductions(null);
        for(Production p : prods)
        {
            prodListModel.addElement(p);
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
        catch (UnsupportedLookAndFeelException e)
        {
        }
        catch (ClassNotFoundException e)
        {
        }
        catch (InstantiationException e)
        {
        }
        catch (IllegalAccessException e)
        {
        }

    }
    public static void main(String[] args)
    {
        initializeLookAndFeel();
        
        JFrame frame = new JFrame("Little JSoar Debugger");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        final LittleDebugger littleDebugger = new LittleDebugger(frame);
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

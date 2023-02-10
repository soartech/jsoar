/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 26, 2010
 */
package org.jsoar.soarunit.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jsoar.soarunit.Test;
import org.jsoar.soarunit.TestAgentFactory;
import org.jsoar.soarunit.TestCase;
import org.jsoar.soarunit.TestCaseResult;
import org.jsoar.soarunit.TestResult;

/**
 * @author ray
 */
public class TestResultList extends JPanel
{
    private static final long serialVersionUID = -2037972910107529427L;
    
    private final TestAgentFactory agentFactory;
    private final DefaultListModel<TestResultProxy> model = new DefaultListModel<>();
    private final JList<TestResultProxy> list = new JList<>(model);
    private final JTextArea output = new JTextArea();
    
    public TestResultList(TestAgentFactory agentFactory)
    {
        super(new BorderLayout());
        
        this.agentFactory = agentFactory;
        
        this.list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.list.setCellRenderer(new Renderer());
        this.list.addMouseListener(new MouseAdapter()
        {
            /*
             * (non-Javadoc)
             * 
             * @see java.awt.event.MouseAdapter#mouseReleased(java.awt.event.MouseEvent)
             */
            @Override
            public void mouseReleased(MouseEvent e)
            {
                maybeShowContextMenu(e);
            }
            
            /*
             * (non-Javadoc)
             * 
             * @see java.awt.event.MouseAdapter#mousePressed(java.awt.event.MouseEvent)
             */
            @Override
            public void mousePressed(MouseEvent e)
            {
                maybeShowContextMenu(e);
            }
            
            /*
             * (non-Javadoc)
             * 
             * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
             */
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if(SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2)
                {
                    handleDoubleClick(e);
                }
            }
            
        });
        
        this.list.getSelectionModel().addListSelectionListener(new ListSelectionListener()
        {
            
            @Override
            public void valueChanged(ListSelectionEvent e)
            {
                handleSelectionChanged(e);
            }
        });
        
        final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        split.setLeftComponent(new JScrollPane(list));
        split.setRightComponent(new JScrollPane(output));
        split.setDividerSize(5);
        split.setResizeWeight(0.7);
        
        add(split, BorderLayout.CENTER);
    }
    
    protected void handleSelectionChanged(ListSelectionEvent e)
    {
        final TestResultProxy result = (TestResultProxy) list
                .getSelectedValue();
        if(result != null)
        {
            final String outputString = result.getResult().getOutput();
            output.setText(!outputString.isEmpty() ? outputString
                    : "No output produced by agent.");
        }
        else
        {
            output.setText("");
        }
    }
    
    public void reset()
    {
        model.clear();
    }
    
    public void addTestCase(TestCase testCase)
    {
        for(Test test : testCase.getTests())
        {
            model.addElement(new TestResultProxy(test));
        }
    }
    
    private int getProxyIndex(TestResult testResult)
    {
        for(int i = 0; i < model.getSize(); i++)
        {
            final TestResultProxy proxy = (TestResultProxy) model.get(i);
            if(proxy.getTest() == testResult.getTest())
            {
                return i;
            }
        }
        return -1;
    }
    
    public void addTestResults(TestCaseResult testCaseResult)
    {
        for(TestResult testResult : testCaseResult.getTestResults())
        {
            final int index = getProxyIndex(testResult);
            if(index >= 0)
            {
                ((TestResultProxy) model.get(index)).setResult(testResult);
            }
        }
        
        list.repaint();
        
        // Autoscroll
        /*
         * final int lastIndex = model.getSize() - 1;
         * if(lastIndex >= 0)
         * {
         * list.ensureIndexIsVisible(lastIndex);
         * }
         */
    }
    
    private void handleDoubleClick(MouseEvent e)
    {
        final TestResultProxy result = (TestResultProxy) list.getSelectedValue();
        if(result != null)
        {
            EditTestAction.editTest(result.getTest());
        }
    }
    
    private void maybeShowContextMenu(MouseEvent e)
    {
        if(!e.isPopupTrigger())
        {
            return;
        }
        
        final int index = list.locationToIndex(e.getPoint());
        if(index < 0)
        {
            return;
        }
        
        list.setSelectedIndex(index);
        final JPopupMenu menu = new JPopupMenu();
        final TestResultProxy result = (TestResultProxy) list.getSelectedValue();
        if(result != null)
        {
            menu.add(new EditTestAction(result.getTest()));
            menu.add(new DebugTestAction(agentFactory, result.getTest()));
            menu.add(new CopyDebugTestToClipboardAction(result.getTest()));
        }
        menu.show(e.getComponent(), e.getX(), e.getY());
    }
    
    private static class Renderer extends DefaultListCellRenderer
    {
        private static final long serialVersionUID = 771703914650867765L;
        
        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
         */
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus)
        {
            final JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                    cellHasFocus);
            
            final TestResultProxy r = (TestResultProxy) value;
            if(!isSelected)
            {
                if(r.getResult() != null)
                {
                    if(r.getResult().isPassed())
                    {
                        c.setBackground(Constants.PASS_COLOR);
                    }
                    else
                    {
                        c.setBackground(Constants.FAIL_COLOR);
                    }
                }
                else
                {
                    c.setBackground(Constants.RUNNING_COLOR);
                }
            }
            
            return c;
        }
    }
    
}

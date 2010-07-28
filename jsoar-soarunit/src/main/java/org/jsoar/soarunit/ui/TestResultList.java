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
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.jsoar.soarunit.TestAgentFactory;
import org.jsoar.soarunit.TestCaseResult;
import org.jsoar.soarunit.TestResult;

/**
 * @author ray
 */
public class TestResultList extends JPanel
{
    private static final long serialVersionUID = -2037972910107529427L;
    
    private final TestAgentFactory agentFactory;
    private final DefaultListModel model = new DefaultListModel();
    private final JList list = new JList(model);

    public TestResultList(TestAgentFactory agentFactory)
    {
        super(new BorderLayout());
        
        this.agentFactory = agentFactory;
        
        this.list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.list.setCellRenderer(new Renderer());
        this.list.addMouseListener(new MouseAdapter() {
            /* (non-Javadoc)
             * @see java.awt.event.MouseAdapter#mouseReleased(java.awt.event.MouseEvent)
             */
            @Override
            public void mouseReleased(MouseEvent e)
            {
                maybeShowContextMenu(e);
            }

            /* (non-Javadoc)
             * @see java.awt.event.MouseAdapter#mousePressed(java.awt.event.MouseEvent)
             */
            @Override
            public void mousePressed(MouseEvent e)
            {
                maybeShowContextMenu(e);
            }

            /* (non-Javadoc)
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
        
        add(new JScrollPane(list), BorderLayout.CENTER);        
    }
    
    public void reset()
    {
        model.clear();
    }
    
    public void addTestResults(TestCaseResult testCaseResult)
    {
        for(TestResult testResult : testCaseResult.getTestResults())
        {
            model.addElement(testResult);
        }
        
        // Autoscroll
        final int lastIndex = model.getSize() - 1;
        if(lastIndex >= 0)
        {
            list.ensureIndexIsVisible(lastIndex);
        }
    }
    
    private void handleDoubleClick(MouseEvent e)
    {
        final TestResult result = (TestResult) list.getSelectedValue();
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
        final TestResult result = (TestResult) list.getSelectedValue();
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

        /* (non-Javadoc)
         * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
         */
        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus)
        {
            // TODO Auto-generated method stub
            final JLabel c = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected,
                    cellHasFocus);
            
            final TestResult r = (TestResult) value;
            if(!isSelected)
            {
                if(!r.isPassed())
                {
                    c.setBackground(Constants.FAIL_COLOR);
                }
            }
            
            return c;
        }
        
    }

}

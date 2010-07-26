/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 24, 2010
 */
package org.jsoar.soarunit.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.jsoar.kernel.SoarException;
import org.jsoar.soarunit.TestResult;
import org.jsoar.soarunit.TestSuite;
import org.jsoar.soarunit.TestSuiteResult;

/**
 * @author ray
 */
public class TestPanel extends JPanel
{
    private static final long serialVersionUID = 4823211094468351324L;
    private static final Color FAIL_COLOR = new Color(242, 102, 96);
    private static final Color PASS_COLOR = new Color(102, 242, 96);
    
    private final List<TestSuite> allSuites;
    private final JLabel summary = new JLabel();
    private final TestProgressBar testProgress = new TestProgressBar();
    private final DefaultListModel model = new DefaultListModel();
    private final JList list = new JList(model);

    private final int total;
    private int passed;
    private int failed;
    
    public TestPanel(List<TestSuite> allSuites)
    {
        super(new BorderLayout());
        
        this.allSuites = allSuites;
        this.total = TestSuite.getTotalTests(allSuites);
        
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
        });
        
        final JPanel header = new JPanel(new GridLayout(2, 1));
        final JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(4, 4, 4, 4), 
                BorderFactory.createLoweredBevelBorder()));
        progressPanel.add(testProgress, BorderLayout.CENTER);
        
        summary.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        header.add(summary);
        header.add(progressPanel);
        
        add(header, BorderLayout.NORTH);
        add(new JScrollPane(list), BorderLayout.CENTER);
        
    }
    
    public void runTests()
    {
        model.clear();
        passed = 0;
        failed = 0;
        updateSummary();
        testProgress.update(total, passed, failed);
        new RunThread().start();
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
            menu.add(new DebugTestAction(result.getTest()));
            menu.add(new CopyDebugTestToClipboardAction(result.getTest()));
        }
        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void runTestsInternal() throws SoarException
    {
        int index = 0;
        for(TestSuite suite : allSuites)
        {
            final TestSuiteResult result = suite.run(index++, allSuites.size());
            addResult(result);
        }
    }
    
    private void addResult(final TestSuiteResult suiteResult)
    {
        SwingUtilities.invokeLater(new Runnable() {

            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run()
            {
                for(TestResult testResult : suiteResult.getTestResults())
                {
                    model.addElement(testResult);
                    if(testResult.isPassed())
                    {
                        passed++;
                    }
                    else
                    {
                        failed++;
                    }
                }
                
                // Autoscroll
                final int lastIndex = model.getSize() - 1;
                if(lastIndex >= 0)
                {
                    list.ensureIndexIsVisible(lastIndex);
                }
                
                testProgress.update(total, passed, failed);
                updateSummary();
            }});
    }
    
    private void updateSummary()
    {
        summary.setText(String.format("%d/%d tests run. %d passed, %d failed%n", 
                                        passed + failed, 
                                        total, passed, failed));
    }

    private class RunThread extends Thread
    {
        /* (non-Javadoc)
         * @see java.lang.Thread#run()
         */
        @Override
        public void run()
        {
            try
            {
                runTestsInternal();
            }
            catch (SoarException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
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
                    c.setBackground(FAIL_COLOR);
                }
            }
            
            return c;
        }
        
    }
}

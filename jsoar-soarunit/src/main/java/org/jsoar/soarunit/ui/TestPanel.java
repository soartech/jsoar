/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 24, 2010
 */
package org.jsoar.soarunit.ui;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.jsoar.kernel.SoarException;
import org.jsoar.soarunit.FiringCounts;
import org.jsoar.soarunit.TestCase;
import org.jsoar.soarunit.TestCaseResult;
import org.jsoar.soarunit.TestResult;

/**
 * @author ray
 */
public class TestPanel extends JPanel
{
    private static final long serialVersionUID = 4823211094468351324L;
    
    private final List<TestCase> allTestCases;
    private final TestSummaryPanel summary;
    private final TestResultList list;
    private final CoveragePanel coverage;
    
    public TestPanel(List<TestCase> allTestCases)
    {
        super(new BorderLayout());
        
        this.allTestCases = allTestCases;
        this.summary = new TestSummaryPanel(TestCase.getTotalTests(allTestCases));
        
        add(summary, BorderLayout.NORTH);

        final JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Tests", list = new TestResultList());
        tabs.addTab("Coverage", coverage = new CoveragePanel());
        
        add(tabs, BorderLayout.CENTER);
    }
    
    public void runTests()
    {
        list.reset();
        summary.reset();
        coverage.reset();
        new RunThread().start();
    }
    
    private void runTestsInternal() throws SoarException
    {
        final FiringCounts allCounts = new FiringCounts();
        int index = 0;
        for(TestCase testCase : allTestCases)
        {
            final TestCaseResult result = testCase.run(index++, allTestCases.size(), false);
            allCounts.merge(result.getFiringCounts());
            addResult(result);
        }
        
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                coverage.setFiringCounts(allCounts);
                summary.update(allCounts);
            }
        });
    }
    
    private void addResult(final TestCaseResult testCaseResult)
    {
        SwingUtilities.invokeLater(new Runnable() {

            /* (non-Javadoc)
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run()
            {
                for(TestResult testResult : testCaseResult.getTestResults())
                {
                    summary.addTestResult(testResult);
                }
                
                list.addTestResults(testCaseResult);
            }});
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
}

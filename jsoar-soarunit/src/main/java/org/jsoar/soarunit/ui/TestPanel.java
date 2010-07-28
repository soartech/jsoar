/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 24, 2010
 */
package org.jsoar.soarunit.ui;

import java.awt.BorderLayout;
import java.io.PrintWriter;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.jsoar.kernel.SoarException;
import org.jsoar.soarunit.FiringCounts;
import org.jsoar.soarunit.TestAgentFactory;
import org.jsoar.soarunit.TestCase;
import org.jsoar.soarunit.TestCaseResult;
import org.jsoar.soarunit.TestResult;
import org.jsoar.soarunit.TestRunner;
import org.jsoar.util.NullWriter;

/**
 * @author ray
 */
public class TestPanel extends JPanel
{
    private static final long serialVersionUID = 4823211094468351324L;
    
    private final TestAgentFactory agentFactory;
    private final List<TestCase> allTestCases;
    private final TestSummaryPanel summary;
    private final TestResultList list;
    private final CoveragePanel coverage;
    
    public TestPanel(TestAgentFactory agentFactory, List<TestCase> allTestCases)
    {
        super(new BorderLayout());
        
        this.agentFactory = agentFactory;
        this.allTestCases = allTestCases;
        this.summary = new TestSummaryPanel(TestCase.getTotalTests(allTestCases));
        
        add(summary, BorderLayout.NORTH);

        final JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Tests", list = new TestResultList(agentFactory));
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
        final TestRunner runner = new TestRunner(agentFactory, new PrintWriter(new NullWriter()));
        runner.setHaltOnFailure(false);
        runner.setTotal(allTestCases.size());
        
        for(TestCase testCase : allTestCases)
        {
            final TestCaseResult result = runner.run(testCase);
            addResult(result);
        }
        
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                final FiringCounts allCounts = runner.getFiringCounts();
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

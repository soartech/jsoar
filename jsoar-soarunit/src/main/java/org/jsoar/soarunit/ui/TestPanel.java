/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 24, 2010
 */
package org.jsoar.soarunit.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.jsoar.kernel.SoarException;
import org.jsoar.soarunit.FiringCounts;
import org.jsoar.soarunit.TestAgentFactory;
import org.jsoar.soarunit.TestCase;
import org.jsoar.soarunit.TestCaseCollector;
import org.jsoar.soarunit.TestCaseResult;
import org.jsoar.soarunit.TestCaseResultHandler;
import org.jsoar.soarunit.TestResult;
import org.jsoar.soarunit.TestRunner;
import org.jsoar.util.NullWriter;
import org.jsoar.util.StringTools;

/**
 * @author ray
 */
public class TestPanel extends JPanel
{
    private static final long serialVersionUID = 4823211094468351324L;
    
    private static final Color ERROR_BACKGROUND_COLOR = new Color(242, 102, 96).brighter();

	public static final String RUNNING_TESTS = "runningTests";
    
    private final TestAgentFactory agentFactory;
    private final TestCaseCollector collector;
    private final TestSummaryPanel summary;
    private final TestResultList list;
    private final CoveragePanel coverage;
    private final JTextArea errors = new JTextArea();
    private final Color defaultErrorBackground = errors.getBackground();
    private final JTabbedPane tabs = new JTabbedPane();
   
    public TestPanel(TestAgentFactory agentFactory, TestCaseCollector collector)
    {
        super(new BorderLayout());
        
        this.agentFactory = agentFactory;
        this.collector = collector;
        this.summary = new TestSummaryPanel();
        
        add(summary, BorderLayout.NORTH);

        tabs.addTab("Tests", list = new TestResultList(agentFactory));
        tabs.addTab("Coverage", coverage = new CoveragePanel());
        
        errors.setEditable(false);
        tabs.addTab("Errors", new JScrollPane(errors));
        
        add(tabs, BorderLayout.CENTER);
    }
    
    public void runTests()
    {
    	firePropertyChange(RUNNING_TESTS, false, true);
        list.reset();
        summary.reset();
        coverage.reset();
        
        errors.setText("No errors!");
        errors.setBackground(defaultErrorBackground);
        
        tabs.setSelectedIndex(0);
        new RunThread().start();
    }
    
    private void runTestsInternal() throws SoarException, IOException
    {
        final List<TestCase> allTestCases = collector.collect();
        final int totalTests = TestCase.getTotalTests(allTestCases);
        
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                summary.setTotal(totalTests);
            }
        });
        
        final TestRunner runner = new TestRunner(agentFactory, new PrintWriter(new NullWriter()));
        runner.setHaltOnFailure(false);
        runner.runAllTestCases(allTestCases, new TestCaseResultHandler() {
			
			@Override
			public void handleTestCaseResult(TestCaseResult result) {
				addResult(result);
			}
		});
        
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                handleTestRunFinished(runner);
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
    
    private void showError(final String message, final Throwable e)
    {
        if(SwingUtilities.isEventDispatchThread())
        {
            final StringBuilder text = new StringBuilder(message != null ? message : "no message");
            text.append("\n");
            text.append("____________________________________________________________________________\n\n");
            if(e != null)
            {
                text.append(StringTools.getStackTrace(e));
            }
            errors.setText(text.toString());
            errors.setBackground(ERROR_BACKGROUND_COLOR);
            tabs.setSelectedIndex(2);
            firePropertyChange(RUNNING_TESTS, true, false);
        }
        else
        {
            SwingUtilities.invokeLater(new Runnable() {
                
                @Override
                public void run()
                {
                    showError(message, e);
                }
            });
        }
    }
    
    private void handleTestRunFinished(final TestRunner runner) {
		final FiringCounts allCounts = runner.getFiringCounts();
		coverage.setFiringCounts(allCounts);
		summary.update(allCounts);
		
		firePropertyChange(RUNNING_TESTS, true, false);
	}

	private class RunThread extends Thread
    {
        @Override
        public void run()
        {
            try
            {
                runTestsInternal();
            }
            catch (Exception e)
            {
                showError(e.getMessage(), e);
            }
        }
    }
}

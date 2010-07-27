/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 26, 2010
 */
package org.jsoar.soarunit.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.jsoar.soarunit.FiringCounts;
import org.jsoar.soarunit.TestResult;

/**
 * @author ray
 */
public class TestSummaryPanel extends JPanel
{
    private static final long serialVersionUID = 6339961427705646782L;
    
    private final JLabel summary = new JLabel();
    private final TestProgressBar testProgress = new TestProgressBar();
    private final int total;
    private int passed;
    private int failed;
    private FiringCounts counts = new FiringCounts();
    
    public TestSummaryPanel(int total)
    {
        super(new BorderLayout());
        
        this.total = total;
        
        final JPanel header = new JPanel(new GridLayout(2, 1));
        final JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(4, 4, 4, 4), 
                BorderFactory.createLoweredBevelBorder()));
        progressPanel.add(testProgress, BorderLayout.CENTER);
        
        summary.setBorder(BorderFactory.createEmptyBorder(4, 4, 0, 4));
        header.add(summary);
        header.add(progressPanel);
        
        add(header, BorderLayout.CENTER);
    }
    
    public void reset()
    {
        this.counts = new FiringCounts();
        update(0, 0);
    }
    
    public void addTestResult(TestResult result)
    {
        update(result.isPassed() ? passed + 1 : passed,
               !result.isPassed() ? failed + 1 : failed);
    }

    public void update(FiringCounts counts)
    {
        this.counts = counts;
        updateSummary();
    }
    
    private void update(int passed, int failed)
    {
        this.passed = passed;
        this.failed = failed;
        testProgress.update(total, passed, failed);
        updateSummary();
    }
    
    private void updateSummary()
    {
        summary.setText(String.format("%d/%d tests run. %d passed, %d failed, %d%% coverage%n", 
                                        passed + failed, 
                                        total, passed, failed,
                                        (int) (this.counts.getCoverage() * 100)));
    }
}

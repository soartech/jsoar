/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 24, 2010
 */
package org.jsoar.soarunit.ui;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.jsoar.soarunit.TestSuite;

/**
 * @author ray
 */
public class MainFrame extends JFrame
{
    private static final long serialVersionUID = -701720884093453648L;

    private final JPanel contentPane = new JPanel(new BorderLayout());
    private final TestPanel testPanel;
    
    public MainFrame(List<TestSuite> allSuites)
    {
        super("SoarUnit");
        
        testPanel = new TestPanel(allSuites);
        
        setJMenuBar(initMenuBar());
        
        contentPane.add(initToolBar(), BorderLayout.NORTH);
        contentPane.add(testPanel, BorderLayout.CENTER);
        setContentPane(contentPane);
        
    }
    
    private JToolBar initToolBar()
    {
        final JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        bar.add(new RunTestsAction(testPanel));
        return bar;
    }

    private JMenuBar initMenuBar()
    {
        final JMenuBar bar = new JMenuBar();
        
        return bar;
    }

    public void runTests()
    {
        testPanel.runTests();
    }
}

/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 24, 2010
 */
package org.jsoar.soarunit.ui;

import java.awt.BorderLayout;
import java.util.concurrent.ExecutorService;

import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.UIManager.LookAndFeelInfo;

import org.jsoar.soarunit.TestAgentFactory;
import org.jsoar.soarunit.TestCaseCollector;

/**
 * @author ray
 */
public class MainFrame extends JFrame
{
    private static final long serialVersionUID = -701720884093453648L;

    private final JPanel contentPane = new JPanel(new BorderLayout());
    private final TestPanel testPanel;
    
    public MainFrame(TestAgentFactory agentFactory, TestCaseCollector collector, ExecutorService executor)
    {
        super("SoarUnit");
        
        testPanel = new TestPanel(agentFactory, collector, executor);
        
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
    
    /**
     * Initialize the UI look and feel to the system look and feel. 
     */
    public static void initializeLookAndFeel()
    {
        try
        {
            // First try Nimbus because it looks nice. Then fall back to
            // the system L&F
            try {
                for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        return;
                    }
                }
            } catch (RuntimeException e) {
                // If Nimbus is not available, you can set the GUI to another look and feel.
            }
            
            // Use the look and feel of the system we're running on rather
            // than Java. If an error occurs, we proceed normally using
            // whatever L&F we get.
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (UnsupportedLookAndFeelException e) { }
        catch (ClassNotFoundException e) { }
        catch (InstantiationException e) { }
        catch (IllegalAccessException e) { }
    }
    
}

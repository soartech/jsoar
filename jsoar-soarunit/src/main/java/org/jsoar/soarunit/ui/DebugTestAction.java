/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 24, 2010
 */
package org.jsoar.soarunit.ui;

import java.awt.event.ActionEvent;
import java.io.PrintWriter;

import javax.swing.AbstractAction;

import org.jsoar.kernel.SoarException;
import org.jsoar.soarunit.Test;
import org.jsoar.soarunit.TestAgentFactory;
import org.jsoar.soarunit.TestRunner;
import org.jsoar.util.NullWriter;

/**
 * @author ray
 */
public class DebugTestAction extends AbstractAction
{
    private static final long serialVersionUID = -3500496894588331412L;
    
    private final TestAgentFactory agentFactory;
    private final Test test;
    
    public DebugTestAction(TestAgentFactory agentFactory, Test test)
    {
        super("Debug");
        
        this.agentFactory = agentFactory;
        this.test = test;
    }


    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        try
        {
            final TestRunner runner = new TestRunner(agentFactory, new PrintWriter(new NullWriter()));
            runner.debugTest(test, false);
        }
        catch (SoarException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (InterruptedException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

}

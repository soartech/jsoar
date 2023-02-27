/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 24, 2010
 */
package org.jsoar.soarunit.ui;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.PrintWriter;

import javax.swing.AbstractAction;

import org.jsoar.kernel.SoarException;
import org.jsoar.soarunit.Test;
import org.jsoar.soarunit.TestAgentFactory;
import org.jsoar.soarunit.TestRunner;
import org.jsoar.util.NullWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ray
 */
public class DebugTestAction extends AbstractAction
{
    private static final Logger LOG = LoggerFactory.getLogger(DebugTestAction.class);
    
    private static final long serialVersionUID = -3500496894588331412L;
    
    private final TestAgentFactory agentFactory;
    private final Test test;
    
    public DebugTestAction(TestAgentFactory agentFactory, Test test)
    {
        super("Debug");
        
        this.agentFactory = agentFactory;
        this.test = test;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent event)
    {
        try(PrintWriter pw = new PrintWriter(new NullWriter()))
        {
            final TestRunner runner = new TestRunner(agentFactory, () -> pw, null);
            runner.debugTest(test.reload(), false);
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        catch(SoarException | IOException e)
        {
            LOG.error("Error handling action event", e);
        }
    }
    
}

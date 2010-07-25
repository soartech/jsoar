/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 24, 2010
 */
package org.jsoar.soarunit.ui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
 * @author ray
 */
public class RunTestsAction extends AbstractAction
{
    private static final long serialVersionUID = -4334282971542808502L;
    
    private final TestPanel tp;
    
    public RunTestsAction(TestPanel tp)
    {
        super("Run All");
        
        this.tp = tp;
    }
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        tp.runTests();
    }

}

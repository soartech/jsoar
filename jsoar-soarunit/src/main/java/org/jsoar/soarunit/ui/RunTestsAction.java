/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 24, 2010
 */
package org.jsoar.soarunit.ui;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;

/**
 * @author ray
 */
public class RunTestsAction extends AbstractAction implements PropertyChangeListener
{
    private static final long serialVersionUID = -4334282971542808502L;
    
    private final TestPanel tp;
    
    public RunTestsAction(TestPanel tp)
    {
        super("Re-run All Tests");
        
        this.tp = tp;
        this.tp.addPropertyChangeListener(TestPanel.RUNNING_TESTS, this);
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e)
    {
        tp.runTests();
    }
    
	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		setEnabled(!((Boolean) evt.getNewValue()).booleanValue());
	}

}

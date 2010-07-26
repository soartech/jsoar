/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 24, 2010
 */
package org.jsoar.soarunit.ui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jsoar.kernel.SoarException;
import org.jsoar.soarunit.Test;

/**
 * @author ray
 */
public class DebugTestAction extends AbstractAction
{
    private static final long serialVersionUID = -3500496894588331412L;
    
    private final Test test;
    
    public DebugTestAction(Test test)
    {
        super("Debug");
        
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
            test.getTestCase().debugTest(test);
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

/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;

import org.jsoar.debugger.actions.InitSoarAction;
import org.jsoar.debugger.actions.RunAction;
import org.jsoar.debugger.actions.StopAction;

/**
 * @author ray
 */
public class RunControlPanel extends JPanel
{
    private static final long serialVersionUID = 4339204720269679671L;

    private final LittleDebugger debugger;
    
    private JTextField countField;
    private JComboBox stepTypeCombo;
    
    public RunControlPanel(LittleDebugger debuggerIn)
    {
        super(new BorderLayout());
        
        this.debugger = debuggerIn;
        
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        
        bar.add(new JLabel("  Run "));
        countField = debugger.getRunControlModel().createCountField();
        countField.setHorizontalAlignment(JTextField.RIGHT);
        countField.setMaximumSize(new Dimension(50, 20));
        bar.add(countField);
        
        stepTypeCombo = debugger.getRunControlModel().createTypeCombo();
        stepTypeCombo.setSelectedIndex(0);
        stepTypeCombo.setMaximumSize(new Dimension(150, 20));
        bar.add(stepTypeCombo);
        
        bar.add(debugger.getActionManager().getAction(RunAction.class));
        bar.add(debugger.getActionManager().getAction(StopAction.class));
        bar.add(debugger.getActionManager().getAction(InitSoarAction.class));
        
        add(bar, BorderLayout.CENTER);
    }
}

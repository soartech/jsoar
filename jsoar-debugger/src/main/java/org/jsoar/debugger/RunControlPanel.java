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

import org.jsoar.debugger.actions.ActionManager;
import org.jsoar.debugger.actions.InitSoarAction;
import org.jsoar.debugger.actions.RunAction;
import org.jsoar.debugger.actions.StopAction;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;

/**
 * @author ray
 */
public class RunControlPanel extends JPanel
{
    private static final long serialVersionUID = 4339204720269679671L;

    private JTextField countField;
    private JComboBox stepTypeCombo;
    
    public RunControlPanel(Adaptable debuggerIn)
    {
        super(new BorderLayout());
        
        final JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        
        final RunControlModel rcm = Adaptables.adapt(debuggerIn, RunControlModel.class);
        bar.add(new JLabel("  Run "));
        countField = rcm.createCountField();
        countField.setHorizontalAlignment(JTextField.RIGHT);
        countField.setMaximumSize(new Dimension(50, 20));
        bar.add(countField);
        
        stepTypeCombo = rcm.createTypeCombo();
        stepTypeCombo.setSelectedIndex(0);
        stepTypeCombo.setMaximumSize(new Dimension(150, 20));
        bar.add(stepTypeCombo);
        
        final ActionManager am = Adaptables.adapt(debuggerIn, ActionManager.class);
        bar.add(am.getAction(RunAction.class));
        bar.add(am.getAction(StopAction.class));
        bar.add(am.getAction(InitSoarAction.class));
        
        add(bar, BorderLayout.CENTER);
    }
}

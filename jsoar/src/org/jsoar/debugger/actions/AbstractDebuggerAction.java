/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2008
 */
package org.jsoar.debugger.actions;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.KeyStroke;

import org.jsoar.debugger.JSoarDebugger;
import org.jsoar.debugger.selection.SelectionManager;

/**
 * @author ray
 */
public abstract class AbstractDebuggerAction extends AbstractAction
{
    private final ActionManager manager;
    
    public AbstractDebuggerAction(String label)
    {
        super(label);
        this.manager = null;
    }

    public AbstractDebuggerAction(String label, Icon icon)
    {
        super(label, icon);
        this.manager = null;
    }
    
    public AbstractDebuggerAction(ActionManager manager, String label)
    {
        super(label);
        this.manager = manager;
        this.manager.addAction(this);
    }

    public AbstractDebuggerAction(ActionManager manager, String label, Icon icon)
    {
        super(label, icon);
        this.manager = manager;
        this.manager.addAction(this);
    }
    
    public AbstractDebuggerAction(ActionManager manager, String label, Class<?> klass, boolean adapt)
    {
        super(label);
        this.manager = manager;
        manager.addObjectAction(this, klass, adapt);
    }

    public AbstractDebuggerAction(ActionManager manager, String label, Icon icon, Class<?> klass, boolean adapt)
    {
        super(label, icon);
        this.manager = manager;
        manager.addObjectAction(this, klass, adapt);
    }
    
    public void setToolTip(String tip)
    {
        this.putValue(SHORT_DESCRIPTION, tip);
    }
    
    public void setAcceleratorKey(KeyStroke key)
    {
        this.putValue(ACCELERATOR_KEY, key);
    }
    
    public void setLabel(String label)
    {
        this.putValue(NAME, label);
    }

    public abstract void update();
    
    public String getId()
    {
        return getClass().getCanonicalName();
    }
    
    public ActionManager getActions()
    {
        return manager;
    }
    
    public JSoarDebugger getApplication()
    {
        return manager != null ? manager.getApplication() : null;
    }
    
    public SelectionManager getSelectionManager()
    {
        return manager != null ? manager.getSelectionManager() : null;
    }
}

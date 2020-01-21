/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2008
 */
package org.jsoar.debugger.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import org.jsoar.debugger.Images;
import org.jsoar.kernel.Production;
import org.jsoar.util.adaptables.Adaptables;

/**
 * @author ray
 */
public class SetBreakpointAction extends AbstractDebuggerAction
{
    private static final long serialVersionUID = -1460902354871319429L;

    /**
     * @param manager the owning action manager
     */
    public SetBreakpointAction(ActionManager manager)
    {
        super(manager, "Add breakpoint", Images.PRODUCTION_BREAK, Production.class, true);
        
        setToolTip("Set breakpoint on production");
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.actions.AbstractDebuggerAction#update()
     */
    @Override
    public void update()
    {
        final List<Production> prods = Adaptables.adaptCollection(getSelectionManager().getSelection(), Production.class);
        setEnabled(prods.size() == 1);
        if(prods.size() == 1)
        {
            setLabel(prods.get(0).isBreakpointEnabled() ? "Remove breakpoint" : "Add breakpoint");
        }
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent arg0)
    {
        final List<Production> prods = Adaptables.adaptCollection(getSelectionManager().getSelection(), Production.class);
        if(prods.isEmpty())
        {
            return;
        }
        prods.get(0).setBreakpointEnabled(!prods.get(0).isBreakpointEnabled());
        update();
    }

}

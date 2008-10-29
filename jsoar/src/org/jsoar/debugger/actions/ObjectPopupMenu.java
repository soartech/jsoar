/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 28, 2008
 */
package org.jsoar.debugger.actions;

import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

/**
 * @author ray
 */
public class ObjectPopupMenu extends JPopupMenu
{
    private static final long serialVersionUID = 6072258090248211454L;
    
    public static void show(MouseEvent e, ActionManager am)
    {
        Object o = am.getApplication().getSelectionManager().getSelectedObject();
        if(o != null && e.isPopupTrigger())
        {
            ObjectPopupMenu menu = new ObjectPopupMenu(am, o);
            menu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    public ObjectPopupMenu(ActionManager am, Object object)
    {
        for(AbstractDebuggerAction a : am.getActionsForObject(object))
        {
            add(a);
        }
    }
}

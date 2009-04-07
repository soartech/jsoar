/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 28, 2008
 */
package org.jsoar.debugger.actions;

import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTree;

/**
 * @author ray
 */
public class ObjectPopupMenu extends JPopupMenu
{
    private static final long serialVersionUID = 6072258090248211454L;
    
    public static void show(MouseEvent e, ActionManager am, boolean fixSelection)
    {
        if(!e.isPopupTrigger())
        {
            return;
        }
        
        // Make sure that the selection follows the right-click. On Windows,
        // right-clicking does not change the selection which is retarded. So,
        // we fix it here.
        if(fixSelection && e.getComponent() instanceof JTable)
        {
            JTable t = (JTable) e.getComponent();
            int row = t.rowAtPoint(e.getPoint());
            if(row != -1)
            {
                t.getSelectionModel().setSelectionInterval(row, row);
            }
        }
        else if(fixSelection && e.getComponent() instanceof JTree)
        {
            JTree t = (JTree) e.getComponent();
            int row = t.getRowForLocation(e.getPoint().x, e.getPoint().y);
            if(row != -1)
            {
                t.setSelectionRow(row);
            }
        }
        
        Object o = am.getSelectionManager().getSelectedObject();
        if(o != null)
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

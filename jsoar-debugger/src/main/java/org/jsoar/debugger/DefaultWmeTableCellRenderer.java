/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 18, 2009
 */
package org.jsoar.debugger;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * @author ray
 */
public class DefaultWmeTableCellRenderer extends DefaultTableCellRenderer
{
    private static final long serialVersionUID = -1107115981550627952L;

    /* (non-Javadoc)
     * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int col)
    {
        final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
        if(col == 0)
        {
            setIcon(Images.WME);
        }
        return c;
    }

    
}

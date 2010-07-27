/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 26, 2010
 */
package org.jsoar.soarunit.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.jsoar.soarunit.FiringCounts;

/**
 * @author ray
 */
public class CoveragePanel extends JPanel
{
    private static final long serialVersionUID = 3969855352071214304L;
    
    private FiringCounts counts = new FiringCounts();
    private final DefaultTableModel model = new DefaultTableModel(new Object[] {"FC", "Rule Name" }, 0) {

        private static final long serialVersionUID = -511335328461698520L;

        /* (non-Javadoc)
         * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
         */
        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            if(columnIndex == 0)
            {
                return Long.class;
            }
            return super.getColumnClass(columnIndex);
        }

        /* (non-Javadoc)
         * @see javax.swing.table.DefaultTableModel#isCellEditable(int, int)
         */
        @Override
        public boolean isCellEditable(int row, int column)
        {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    
    public CoveragePanel()
    {
        super(new BorderLayout());
     
        final TableColumn fcColumn = table.getColumnModel().getColumn(0);
        fcColumn.setMaxWidth(50);
        fcColumn.setCellRenderer(new Renderer());
        table.setRowSorter(new TableRowSorter<DefaultTableModel>(model));
        table.getRowSorter().toggleSortOrder(0);
        
        add(new JScrollPane(table), BorderLayout.CENTER);
    }
    
    public void reset()
    {
        model.setRowCount(0);
    }
    
    public FiringCounts getFiringCounts()
    {
        return counts;
    }
    
    public void setFiringCounts(FiringCounts counts)
    {
        reset();
        
        this.counts = counts;
        for(Map.Entry<String, Long> e : this.counts.getEntries())
        {
            model.addRow(new Object[] {  e.getValue(), e.getKey() });
        }
    }
    
    private static class Renderer extends DefaultTableCellRenderer
    {
        private static final long serialVersionUID = 2627814040530140111L;

        /* (non-Javadoc)
         * @see javax.swing.table.DefaultTableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
         */
        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column)
        {
            final JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, 
                    isSelected, hasFocus, row, column);
            final Long longValue = (Long) value;
            
            if(!isSelected)
            {
                if(longValue.longValue() == 0L)
                {
                    label.setBackground(Constants.FAIL_COLOR);
                }
                else
                {
                    label.setBackground(table.getBackground());
                }
            }
            return label;
        }
        
    }
}

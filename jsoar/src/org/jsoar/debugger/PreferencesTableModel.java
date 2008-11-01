/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 31, 2008
 */
package org.jsoar.debugger;

import javax.swing.table.AbstractTableModel;

import org.jsoar.kernel.commands.StructuredPreferencesCommand.Result;
import org.jsoar.kernel.commands.StructuredPreferencesCommand.ResultEntry;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

/**
 * @author ray
 */
public class PreferencesTableModel extends AbstractTableModel
{
    private static final long serialVersionUID = 1244490544555893063L;
    
    private static final String[] columns = {"Type", "Support", "Id", "Attr", "Value", "Referent" };
    private static final Class<?>[] classes = { PreferenceType.class, String.class, Identifier.class, Symbol.class, Symbol.class, Symbol.class };

    private final Result result;
    
    /**
     * @param result
     */
    public PreferencesTableModel(Result result)
    {
        if(result == null)
        {
            throw new IllegalArgumentException("result");
        }
        this.result = result;
    }
    
    public ResultEntry getResultEntry(int r)
    {
        return result.getEntries().get(r);
    }

    /* (non-Javadoc)
     * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
     */
    @Override
    public Class<?> getColumnClass(int c)
    {
        return classes[c];
    }

    /* (non-Javadoc)
     * @see javax.swing.table.AbstractTableModel#getColumnName(int)
     */
    @Override
    public String getColumnName(int c)
    {
        return columns[c];
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    @Override
    public int getColumnCount()
    {
        return columns.length;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getRowCount()
     */
    @Override
    public int getRowCount()
    {
        return result.getEntries().size();
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    @Override
    public Object getValueAt(int r, int c)
    {
        ResultEntry e = result.getEntries().get(r);
        switch(c)
        {
        case 0: return e.getType();
        case 1: return e.isOSupported() ? ":O" : ":I";
        case 2: return e.getIdentifier();
        case 3: return e.getAttribute();
        case 4: return e.getValue();
        case 5: return e.getReferent();
        }
        return null;
    }

}

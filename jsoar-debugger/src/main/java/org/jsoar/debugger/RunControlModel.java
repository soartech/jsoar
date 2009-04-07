/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2008
 */
package org.jsoar.debugger;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import org.jsoar.kernel.RunType;

/**
 * @author ray
 */
public class RunControlModel
{
    private PlainDocument count = new PlainDocument();
    {
        try
        {
            count.insertString(0, "1", null);
        }
        catch (BadLocationException e)
        {
            throw new RuntimeException(e);
        }
    }
    private ComboBoxModel runType = new DefaultComboBoxModel(RunType.values());
    
    public int getCount()
    {
        try
        {
            return Integer.valueOf(count.getText(0, count.getLength()));
        }
        catch (NumberFormatException e)
        {
            return 1;
        }
        catch (BadLocationException e)
        {
            return 1;
        }
    }
    
    public RunType getType()
    {
        return (RunType) runType.getSelectedItem();
    }
    
    public JTextField createCountField()
    {
        JTextField text = new JTextField();
        text.setDocument(count);
        return text;
    }
    
    public JComboBox createTypeCombo()
    {
        return new JComboBox(runType);
    }
}

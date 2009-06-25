/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2008
 */
package org.jsoar.debugger;

import java.util.prefs.Preferences;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;

/**
 * @author ray
 */
public class RunControlModel implements Disposable
{
    private static final Log logger = LogFactory.getLog(Agent.class);
    
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
    
    public RunControlModel()
    {
        final Preferences prefs = getPrefs();
        setCount(prefs.getLong("count", 1));
        final RunType runType = RunType.valueOf(prefs.get("type", RunType.values()[0].name()));
        if(runType != null)
        {
            setType(runType);
        }
    }
    
    public long getCount()
    {
        try
        {
            return Long.valueOf(count.getText(0, count.getLength()));
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
    
    public void setCount(long count)
    {
        try
        {
            this.count.replace(0, this.count.getLength(), Long.toString(count), null);
        }
        catch (BadLocationException e)
        {
            logger.error("Failed to set count to " + count, e);
        }
    }
    
    public RunType getType()
    {
        return (RunType) runType.getSelectedItem();
    }
    
    public void setType(RunType type)
    {
        runType.setSelectedItem(type);
    }
    
    public JTextField createCountField()
    {
        JTextField text = new JTextField();
        text.setDocument(count);
        return text;
    }
    
    public JComboBox createTypeCombo()
    {
        final JComboBox cb = new JComboBox(runType);
        cb.setSelectedItem(getType());
        return cb;
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.Disposable#dispose()
     */
    @Override
    public void dispose()
    {
        final Preferences prefs = getPrefs();
        prefs.putLong("count", getCount());
        prefs.put("type", getType().name());
    }

    private Preferences getPrefs()
    {
        return JSoarDebugger.PREFERENCES.node("run");
    }
    
}

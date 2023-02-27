/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 28, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.prompt.PromptSupport;
import org.jdesktop.swingx.sort.RowFilters;

/**
 * @author ray
 */
public class TableFilterPanel extends JPanel
{
    private static final long serialVersionUID = -6163793012929297363L;
    
    private final JXTable table;
    private final int column;
    private final JTextField field = new JTextField();
    private final JCheckBox checkRegex = new JCheckBox("Regex?");
    private final Color goodBackground = field.getBackground();
    private final Color badBackground = new Color(242, 102, 96);
    private final TableRowSorter<TableModel> sorter;
    
    public TableFilterPanel(JXTable table, int column)
    {
        super(new BorderLayout());
        
        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.table = table;
        this.column = column;
        
        this.sorter = new TableRowSorter<TableModel>(table.getModel());
        this.table.setRowSorter(sorter);
        
        // Listen for changes to the text field
        field.getDocument().addDocumentListener(new DocumentListener()
        {
            
            @Override
            public void changedUpdate(DocumentEvent e)
            {
                update();
            }
            
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                update();
            }
            
            @Override
            public void removeUpdate(DocumentEvent e)
            {
                update();
            }
        });
        checkRegex.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                setPrompt();
            }
        });
        
        add(new JLabel("Filter: "), BorderLayout.WEST);
        add(field, BorderLayout.CENTER);
        add(checkRegex, BorderLayout.SOUTH);
        
        setPrompt();
    }
    
    public void setPrompt()
    {
        if(checkRegex.isSelected())
        {
            PromptSupport.setPrompt("Enter a regex...", field);
        }
        else
        {
            PromptSupport.setPrompt("Enter a string...", field);
        }
        update();
    }
    
    private void update()
    {
        try
        {
            field.setBackground(goodBackground);
            String patternText = field.getText().trim();
            if(patternText.length() == 0)
            {
                sorter.setRowFilter(null);
            }
            else
            {
                if(checkRegex.isSelected())
                {
                    sorter.setRowFilter(RowFilter.regexFilter(patternText, column));
                }
                else
                {
                    sorter.setRowFilter(RowFilters.regexFilter(Pattern.CASE_INSENSITIVE, Pattern.quote(patternText), column));
                }
            }
        }
        catch(PatternSyntaxException e)
        {
            field.setBackground(badBackground);
        }
    }
}

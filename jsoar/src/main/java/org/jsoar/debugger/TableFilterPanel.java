/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 28, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.FilterPipeline;
import org.jdesktop.swingx.decorator.PatternFilter;

/**
 * @author ray
 */
public class TableFilterPanel extends JPanel
{
    private static final long serialVersionUID = -6163793012929297363L;
    
    private final JXTable table;
    private final int column;
    private final JTextField field = new JTextField();
    private final Color goodBackground = field.getBackground();
    private final Color badBackground = new Color(242, 102, 96);
    
    public TableFilterPanel(JXTable table, int column)
    {
        super(new BorderLayout());
        
        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.table = table;
        this.column = column;
        
        // Listen for changes to the text field
        field.getDocument().addDocumentListener(new DocumentListener() {

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
            }});
        
        add(new JLabel("Filter: "), BorderLayout.WEST);
        add(field, BorderLayout.CENTER);
    }
    
    private void update()
    {
        try
        {
            field.setBackground(goodBackground);
            String patternText = field.getText().trim();
            if(patternText.length() == 0)
            {
                table.setFilters(null);
            }
            else
            {
                table.setFilters(new FilterPipeline(new PatternFilter(patternText, 0, column)));
            }
        }
        catch (PatternSyntaxException e)
        {
            field.setBackground(badBackground);
        }
    }
}

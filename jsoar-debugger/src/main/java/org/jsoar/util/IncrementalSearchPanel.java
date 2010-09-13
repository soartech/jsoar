/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 18, 2009
 */
package org.jsoar.util;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

/**
 * @author ray
 */
public class IncrementalSearchPanel extends JPanel
{
    private static final long serialVersionUID = -2622212061070295059L;
    
    private final JTextComponent target;
    private final JTextField searchField = new JTextField(15);
    private final IncrementalSearch searcher;
    private final Color normalBackground = searchField.getBackground();
    private final Color goodBackground = new Color(102, 242, 96);
    private final Color badBackground = new Color(242, 102, 96);
    
    public IncrementalSearchPanel(JTextComponent target)
    {
        super(new BorderLayout());
        
        this.target = target;
        SwingTools.addSelectAllOnFocus(searchField);
        this.searcher = new IncrementalSearch(this.target) {

            @Override
            protected void onError()
            {
                searchField.setBackground(badBackground);
            }

            @Override
            protected void onMatch()
            {
                searchField.setBackground(goodBackground);
            }

            @Override
            protected void onNoMatch()
            {
                searchField.setBackground(normalBackground);
            }
            
        };
        this.searchField.getDocument().addDocumentListener(searcher);
        this.searchField.addActionListener(searcher);
        
        add(new JLabel("  Search: "), BorderLayout.WEST);
        add(searchField, BorderLayout.CENTER);
    }

    public String getSearchText()
    {
        return searchField.getText();
    }
    
    public void setSearchText(String text)
    {
        searchField.setText(text);
    }
}

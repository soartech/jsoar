/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 18, 2009
 */
package org.jsoar.util;

import org.jsoar.debugger.JSoarDebugger;
import org.jsoar.debugger.syntax.Highlighter;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.JTextComponent;

/**
 * @author ray
 */
public class IncrementalSearchPanel extends JPanel
{
    private static final long serialVersionUID = -2622212061070295059L;

    private final JTextComponent target;
    final JTextField searchField = new JTextField(15);
    private final IncrementalSearch searcher;
    private final Color normalBackground = searchField.getBackground();
    private final Color goodBackground = new Color(102, 242, 96);
    private final Color badBackground = new Color(242, 102, 96);
    private final JButton btnNext;
    private JWindow addonWindow;
    private final JPanel addonPanel;
    @SuppressWarnings("unused")
    private boolean addonShowing = false;
    private final JLabel lblMatches;

    public IncrementalSearchPanel(JTextComponent target, JSoarDebugger debugger)
    {
        super(new BorderLayout());

        this.target = target;
        SwingTools.addSelectAllOnFocus(searchField);
        this.searcher = new IncrementalSearch(this.target)
        {

            @Override
            protected void onError()
            {
                searchField.setBackground(badBackground);
            }

            @Override
            protected void onMatch(int match, int total)
            {
                searchField.setBackground(goodBackground);
                lblMatches.setText("Match " + (match+1) + " of " + total);
            }

            @Override
            protected void onNoMatch()
            {
                searchField.setBackground(normalBackground);
                lblMatches.setText("No Matches");
            }

        };
        searcher.setHighlightColor(Highlighter.getInstance(debugger).getPatterns().getSelection());
        this.searchField.getDocument().addDocumentListener(searcher);
        this.searchField.addActionListener(searcher);
        this.searchField.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                super.focusGained(e);
                showAddon();
            }

            @Override
            public void focusLost(FocusEvent e)
            {
                super.focusLost(e);
                if (e.getOppositeComponent() == null || (e.getOppositeComponent() != addonPanel && e.getOppositeComponent().getParent() != addonPanel)) {
                    hideAddon();
                }
            }
        });


        add(new JLabel("  Search: "), BorderLayout.WEST);
        add(searchField, BorderLayout.CENTER);

        addonPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();

        btnNext = new JButton("Next");
        btnNext.addActionListener(e -> searcher.continueSearch());
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.fill = GridBagConstraints.BOTH;
        addonPanel.add(btnNext, constraints);

        JButton btnPrev = new JButton("Prev");
        btnPrev.addActionListener(e -> searcher.findPrev());
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.fill = GridBagConstraints.BOTH;
        addonPanel.add(btnPrev, constraints);

        JCheckBox chkCase = new JCheckBox("Case Sensitive?");
        chkCase.addItemListener(e ->
        {
            searcher.setMatchCase(chkCase.isSelected());
            searcher.runNewSearch(searchField.getText());
        });
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        constraints.fill = GridBagConstraints.BOTH;
        addonPanel.add(chkCase, constraints);

        JCheckBox chkRegex = new JCheckBox("Regex?");
        chkRegex.addItemListener(e ->
        {
            searcher.setUseRegex(chkRegex.isSelected());
            searcher.runNewSearch(searchField.getText());
        });
        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.fill = GridBagConstraints.BOTH;
        addonPanel.add(chkRegex, constraints);

        lblMatches = new JLabel("No Matches");
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        constraints.fill = GridBagConstraints.BOTH;
        addonPanel.add(lblMatches, constraints);

        addonWindow = new JWindow(debugger.frame);
        addonWindow.setOpacity(0.8f);
        addonWindow.setVisible(false);
        addonWindow.setFocusable(true);
        addonWindow.setAutoRequestFocus(false);
        addonWindow.setFocusableWindowState(true);
        addonWindow.add(addonPanel);
        addonWindow.pack();

        addonWindow.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                super.focusLost(e);
                if (e.getOppositeComponent() != searchField) {
                    hideAddon();
                }
            }
        });
    }

    private void hideAddon()
    {
        addonShowing = false;
        addonWindow.setVisible(false);
    }

    private void showAddon()
    {

        Point location = searchField.getLocationOnScreen();
        location.y = location.y - addonWindow.getHeight();
        location.x = location.x - (addonWindow.getWidth() - searchField.getWidth());
        addonWindow.setLocation(location);
        addonWindow.setVisible(true);
//        addonWindow.setBounds(location.x, yLoc, 220, 110);
        addonWindow.toFront();
        addonShowing = true;
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

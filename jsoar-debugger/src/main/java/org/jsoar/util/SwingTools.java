/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 14, 2008
 */
package org.jsoar.util;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

/**
 * @author ray
 */
public class SwingTools
{
    /**
     * Initialize the UI look and feel to the system look and feel. 
     */
    public static void initializeLookAndFeel()
    {
        try
        {
            // Use the look and feel of the system we're running on rather
            // than Java. If an error occurs, we proceed normally using
            // whatever L&F we get.
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (UnsupportedLookAndFeelException e) { }
        catch (ClassNotFoundException e) { }
        catch (InstantiationException e) { }
        catch (IllegalAccessException e) { }
    }
    
    /**
     * Add all the items in the given collection to the given list model
     * 
     * @param model the list model
     * @param items the items to add
     * @return the list model
     */
    public static DefaultListModel addAll(DefaultListModel model, Collection<?> items)
    {
        for(Object o : items)
        {
            model.addElement(o);
        }
        return model;
    }
    
    /**
     * May be called to set a split pane's proportional divider location before the
     * split pane is displayed. Uggh.
     * 
     * @param split the split pane
     * @param location the proportional location
     */
    public static void setDividerLocation(final JSplitPane split, final double location)
    {
        split.setResizeWeight(location);
        SwingUtilities.invokeLater(new Runnable(){

            @Override
            public void run()
            {
                split.setDividerLocation(location);
            }});
        
    }
    
    /**
     * Add undo/redo support to a text component. Undo is bound to "ctrl-z",
     * redo to "ctrl-y".
     * 
     * @param textcomp The text component.
     */
    public static void addUndoSupport(JTextComponent textcomp)
    {
        // Based on http://www.exampledepot.com/egs/javax.swing.undo/UndoText.html
        final UndoManager undo = new UndoManager();
        Document doc = textcomp.getDocument();
        
        // Listen for undo and redo events
        doc.addUndoableEditListener(new UndoableEditListener() {
            public void undoableEditHappened(UndoableEditEvent evt) {
                undo.addEdit(evt.getEdit());
            }
        });
        
        // Create an undo action and add it to the text component
        textcomp.getActionMap().put("Undo",
            new AbstractAction("Undo") {
                private static final long serialVersionUID = -1616574389415095169L;

                public void actionPerformed(ActionEvent evt) {
                    try {
                        if (undo.canUndo()) {
                            undo.undo();
                        }
                    } catch (CannotUndoException e) {
                    }
                }
           });
        
        // Bind the undo action to ctl-Z
        textcomp.getInputMap().put(KeyStroke.getKeyStroke("control Z"), "Undo");
        
        // Create a redo action and add it to the text component
        textcomp.getActionMap().put("Redo",
            new AbstractAction("Redo") {
                private static final long serialVersionUID = 58635276936990330L;

                public void actionPerformed(ActionEvent evt) {
                    try {
                        if (undo.canRedo()) {
                            undo.redo();
                        }
                    } catch (CannotRedoException e) {
                    }
                }
            });
        
        // Bind the redo action to ctl-Y
        textcomp.getInputMap().put(KeyStroke.getKeyStroke("control Y"), "Redo");
    }
    
    public static void enableAntiAliasing(Graphics g)
    {
        if(g instanceof Graphics2D)
        {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
    }
 
    public static void addSelectAllOnFocus(final JTextComponent text)
    {
        text.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent e)
            {
                text.selectAll();
            }

            @Override
            public void focusLost(FocusEvent e)
            {
            }});
    }
    public static void addSelectAllOnFocus(final JComboBox combo)
    {
        addSelectAllOnFocus((JTextComponent) combo.getEditor().getEditorComponent());
    }
}

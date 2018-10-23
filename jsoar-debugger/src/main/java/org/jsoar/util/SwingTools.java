/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 14, 2008
 */
package org.jsoar.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import sun.swing.ImageIconUIResource;
import sun.swing.plaf.synth.SynthIcon;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.Collection;
import java.util.Enumeration;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.InsetsUIResource;
import javax.swing.plaf.synth.SynthLookAndFeel;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

/**
 * @author ray
 */
public class SwingTools {
    /**
     * Initialize the UI look and feel to the system look and feel.
     */
    public static void initializeLookAndFeel() {
        try {
            // First try Nimbus because it looks nice. Then fall back to
            // the system L&F
            try {
                for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        return;
                    }
                }
            } catch (Exception e) {
                // If Nimbus is not available, you can set the GUI to another look and feel.
            }

            // Use the look and feel of the system we're running on rather
            // than Java. If an error occurs, we proceed normally using
            // whatever L&F we get.
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException e) {
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }
    }

    /**
     * Add all the items in the given collection to the given list model
     *
     * @param model the list model
     * @param items the items to add
     * @return the list model
     */
    public static DefaultListModel addAll(DefaultListModel model, Collection<?> items) {
        for (Object o : items) {
            model.addElement(o);
        }
        return model;
    }

    /**
     * May be called to set a split pane's proportional divider location before the
     * split pane is displayed. Uggh.
     *
     * @param split    the split pane
     * @param location the proportional location
     */
    public static void setDividerLocation(final JSplitPane split, final double location) {
        split.setResizeWeight(location);
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                split.setDividerLocation(location);
            }
        });

    }

    /**
     * Add undo/redo support to a text component. Undo is bound to "ctrl-z",
     * redo to "ctrl-y".
     *
     * @param textcomp The text component.
     */
    public static void addUndoSupport(JTextComponent textcomp) {
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

    public static void enableAntiAliasing(Graphics g) {
        if (g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
    }

    public static void addSelectAllOnFocus(final JTextComponent text) {
        text.addFocusListener(new FocusListener() {

            @Override
            public void focusGained(FocusEvent e) {
                text.selectAll();
            }

            @Override
            public void focusLost(FocusEvent e) {
            }
        });
    }

    public static void addSelectAllOnFocus(final JComboBox combo) {
        addSelectAllOnFocus((JTextComponent) combo.getEditor().getEditorComponent());
    }

    public static boolean hasChild(JComponent parent, Component child) {
        for (Component c : parent.getComponents()) {
            if (c == child) {
                return true;
            }
        }
        return false;
    }

    public static void setFontScale(float scale) {
        UIDefaults defaults = UIManager.getLookAndFeel().getDefaults();
        Enumeration newKeys = defaults.keys();

        while (newKeys.hasMoreElements()) {
            Object obj = newKeys.nextElement();
            Object current = UIManager.get(obj);
            if (current instanceof FontUIResource) {
                FontUIResource resource = (FontUIResource) current;
                defaults.put(obj, new FontUIResource(resource.deriveFont(resource.getSize2D() * scale)));
            } else if (current instanceof Font) {
                Font resource = (Font) current;
                defaults.put(obj, resource.deriveFont(resource.getSize2D() * scale));
            } else if (current instanceof InsetsUIResource) {
                InsetsUIResource resource = (InsetsUIResource) current;
                resource.set((int) (resource.top * scale), (int) (resource.left * scale), (int) (resource.bottom * scale), (int) (resource.right * scale));
                defaults.put(obj, resource);
            } else if (current instanceof Dimension) {
                Dimension resource = (Dimension) current;
                resource.width = (int) (resource.width * scale);
                resource.height = (int) (resource.height * scale);
                defaults.put(obj, resource);
            } else if (current instanceof ImageIconUIResource) {
                ImageIconUIResource resource = (ImageIconUIResource) current;
                Image image = resource.getImage();
                int width = image.getWidth(resource.getImageObserver());
                int height = image.getHeight(resource.getImageObserver());
                Image scaledInstance = image.getScaledInstance((int) (width * scale), (int) (height * scale), 0);
                resource.setImage(scaledInstance);
                defaults.put(obj, resource);
            } else {
                System.out.println("couldn't format " + current.getClass()+": "+current);
            }
        }
    }
}

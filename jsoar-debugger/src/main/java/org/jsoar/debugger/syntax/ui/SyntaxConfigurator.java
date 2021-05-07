package org.jsoar.debugger.syntax.ui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXColorSelectionButton;
import org.jdesktop.swingx.VerticalLayout;
import org.jsoar.debugger.JSoarDebugger;
import org.jsoar.debugger.TraceView;
import org.jsoar.debugger.syntax.SyntaxPattern;
import org.jsoar.debugger.syntax.SyntaxSettings;
import org.jsoar.debugger.syntax.TextStyle;

public class SyntaxConfigurator {

  private SyntaxSettings syntaxSettings;
  private final JXButton btnApply = new JXButton("Apply");
  private final JXButton btnOk = new JXButton("Ok");
  private final JXButton btnCancel = new JXButton("Cancel");
  private final JXButton btnAddRegex = new JXButton("Add Regex");
  private final JXButton btnAddStyle = new JXButton("Add Style");
  private final JXButton btnReloadDefaults = new JXButton("Reload Default Styles");
  private final JXColorSelectionButton btnBackgroundColorDefault;
  private final JXColorSelectionButton btnForegroundColorDefault;
  private final JXColorSelectionButton btnSelectionColorDefault;
  private JPanel syntaxList;
  private JPanel styleList;

  private final JFrame frame;

  public SyntaxConfigurator(
      final SyntaxSettings settings, final TraceView parent, final JSoarDebugger debugger) {
    this.syntaxSettings = settings;

    frame = new JFrame("Syntax Settings");
    frame.setBounds(100, 100, 1600, 1000);

    btnBackgroundColorDefault = new JXColorSelectionButton(settings.getBackground());
    btnBackgroundColorDefault.getChooser().setColor(settings.getBackground());
    btnForegroundColorDefault = new JXColorSelectionButton(settings.getForeground());
    btnForegroundColorDefault.getChooser().setColor(settings.getForeground());
    btnSelectionColorDefault = new JXColorSelectionButton(settings.getSelection());
    btnSelectionColorDefault.getChooser().setColor(settings.getSelection());
    Dimension size = new Dimension(32, 32);
    btnForegroundColorDefault.setPreferredSize(size);
    btnForegroundColorDefault.setMinimumSize(size);
    btnForegroundColorDefault.setMaximumSize(size);
    btnBackgroundColorDefault.setPreferredSize(size);
    btnBackgroundColorDefault.setMinimumSize(size);
    btnBackgroundColorDefault.setMaximumSize(size);
    btnSelectionColorDefault.setPreferredSize(size);
    btnSelectionColorDefault.setMinimumSize(size);
    btnSelectionColorDefault.setMaximumSize(size);

    JPanel bottomPanel = new JPanel();
    bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
    bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    bottomPanel.add(Box.createRigidArea(new Dimension(5, 0)));
    bottomPanel.add(btnReloadDefaults);
    bottomPanel.add(Box.createRigidArea(new Dimension(5, 0)));
    bottomPanel.add(new JLabel("Default Text Color"));

    bottomPanel.add(btnForegroundColorDefault);
    bottomPanel.add(Box.createRigidArea(new Dimension(5, 0)));
    bottomPanel.add(new JLabel("Window Background Color"));
    bottomPanel.add(btnBackgroundColorDefault);
    bottomPanel.add(Box.createRigidArea(new Dimension(5, 0)));
    bottomPanel.add(new JLabel("Selection Color"));
    bottomPanel.add(btnSelectionColorDefault);
    bottomPanel.add(Box.createHorizontalGlue());
    bottomPanel.add(btnOk);
    bottomPanel.add(Box.createRigidArea(new Dimension(5, 0)));
    bottomPanel.add(btnCancel);
    bottomPanel.add(Box.createRigidArea(new Dimension(5, 0)));
    bottomPanel.add(btnApply);

    final JPanel panel = new JPanel();
    panel.setLayout(new BorderLayout());

    panel.add(bottomPanel, BorderLayout.PAGE_END);

    syntaxList = new JPanel();
    syntaxList.setLayout(new VerticalLayout());
    LinkedList<SyntaxPattern> syntaxPatterns = settings.getSyntaxPatterns();
    Collections.sort(
        syntaxPatterns, (p1, p2) -> p1.getComment().compareToIgnoreCase(p2.getComment()));
    for (final SyntaxPattern pattern : syntaxPatterns) {
      final SyntaxPatternComponent comp =
          new SyntaxPatternComponent(pattern, settings.componentStyles.keySet(), debugger);
      final JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
      syntaxList.add(sep);
      comp.putClientProperty("JComponent.sizeVariant", "large");
      comp.addDeleteButtonListener(
          new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
              syntaxList.remove(comp);
              syntaxList.remove(sep);
              settings.getSyntaxPatterns().remove(pattern);
              onSyntaxChanged();
            }
          });
      syntaxList.add(comp);
    }
    //        syntaxList.setPreferredSize(new Dimension(1000,-1));
    syntaxList.add(btnAddRegex);

    JScrollPane scrollPane = new JScrollPane(syntaxList);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.setVisible(true);
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    panel.add(scrollPane, BorderLayout.WEST);

    styleList = new JPanel();
    styleList.setLayout(new VerticalLayout());
    java.util.List<String> sortedKeys = new ArrayList<>(settings.getComponentStyles().keySet());
    Collections.sort(sortedKeys, String.CASE_INSENSITIVE_ORDER);
    for (final String key : sortedKeys) {
      TextStyle style = settings.getComponentStyles().get(key);
      addStyleComponent(key, style);
    }
    styleList.add(btnAddStyle);

    scrollPane = new JScrollPane(styleList);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    scrollPane.setVisible(true);
    scrollPane.getVerticalScrollBar().setUnitIncrement(16);
    panel.add(scrollPane, BorderLayout.EAST);

    JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(parent.getContentPane());
    Dimension panelSize = topFrame.getContentPane().getSize();
    Dimension preferredSize = panel.getPreferredSize();
    panelSize =
        new Dimension(preferredSize.width, Math.min(panelSize.height, preferredSize.height));
    panel.setMaximumSize(panelSize);
    panel.setPreferredSize(panelSize);
    frame.getContentPane().add(panel);
    frame.pack();

    btnAddRegex.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            final SyntaxPattern newPattern = new SyntaxPattern();
            settings.getSyntaxPatterns().add(newPattern);
            final SyntaxPatternComponent comp =
                new SyntaxPatternComponent(newPattern, settings.componentStyles.keySet(), debugger);
            final JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);

            comp.addDeleteButtonListener(
                new ActionListener() {
                  @Override
                  public void actionPerformed(ActionEvent e) {
                    syntaxList.remove(comp);
                    syntaxList.remove(sep);
                    settings.getSyntaxPatterns().remove(newPattern);
                    onSyntaxChanged();
                  }
                });
            syntaxList.add(sep, syntaxList.getComponentCount() - 1);
            syntaxList.add(comp, syntaxList.getComponentCount() - 1);
            onSyntaxChanged();
          }
        });

    btnAddStyle.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            TextStyle newStyle = new TextStyle();
            String key = "new style";
            int i = 1;
            while (settings.getComponentStyles().containsKey(key)) {
              key = "new style " + i;
              i++;
            }
            settings.addTextStyle(key, newStyle);
            addStyleComponent(key, newStyle);
          }
        });

    btnReloadDefaults.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            int dialogResult =
                JOptionPane.showConfirmDialog(
                    frame,
                    "Are you sure you want to reload the default syntax? This will erase any customizations.",
                    "Are you sure?",
                    JOptionPane.YES_NO_OPTION);
            if (dialogResult == JOptionPane.YES_OPTION) {
              // reload the syntax
              syntaxSettings = parent.reloadSyntaxDefaults();
              syntaxList.removeAll();
              LinkedList<SyntaxPattern> syntaxPatterns = syntaxSettings.getSyntaxPatterns();
              Collections.sort(
                  syntaxPatterns, (p1, p2) -> p1.getComment().compareToIgnoreCase(p2.getComment()));
              for (final SyntaxPattern pattern : syntaxPatterns) {
                final SyntaxPatternComponent comp =
                    new SyntaxPatternComponent(
                        pattern, syntaxSettings.componentStyles.keySet(), debugger);
                final JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
                syntaxList.add(sep);
                comp.putClientProperty("JComponent.sizeVariant", "large");
                comp.addDeleteButtonListener(
                    new ActionListener() {
                      @Override
                      public void actionPerformed(ActionEvent e) {
                        syntaxList.remove(comp);
                        syntaxList.remove(sep);
                        syntaxSettings.getSyntaxPatterns().remove(pattern);
                        onSyntaxChanged();
                      }
                    });
                syntaxList.add(comp);
              }
              syntaxList.add(btnAddRegex);

              styleList.removeAll();
              styleList.setLayout(new VerticalLayout());
              java.util.List<String> sortedKeys =
                  new ArrayList<>(settings.getComponentStyles().keySet());
              Collections.sort(sortedKeys, String.CASE_INSENSITIVE_ORDER);
              for (final String key : sortedKeys) {
                TextStyle style = settings.getComponentStyles().get(key);
                addStyleComponent(key, style);
              }
              styleList.add(btnAddStyle);
            }
          }
        });

    btnApply.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            parent.reformatText();
          }
        });

    btnOk.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            parent.saveSyntax();
            parent.reformatText();
            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
          }
        });

    btnCancel.addActionListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            parent.reloadSyntax();
            parent.reformatText();
            frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
          }
        });

    ChangeListener colorChangeListener =
        new ChangeListener() {
          @Override
          public void stateChanged(ChangeEvent e) {
            settings.setForeground(btnForegroundColorDefault.getChooser().getColor());
            settings.setBackground(btnBackgroundColorDefault.getChooser().getColor());
            settings.setSelection(btnSelectionColorDefault.getChooser().getColor());
          }
        };
    btnBackgroundColorDefault.addChangeListener(colorChangeListener);
    btnForegroundColorDefault.addChangeListener(colorChangeListener);
    btnSelectionColorDefault.addChangeListener(colorChangeListener);
  }

  public void addStyleComponent(String newStyleName, final TextStyle textStyle) {
    final String key = newStyleName;
    final TextStyleComponent comp = new TextStyleComponent(key, textStyle);
    final JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
    comp.addDeleteButtonListener(
        new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            styleList.remove(comp);
            styleList.remove(sep);
            syntaxSettings.getComponentStyles().remove(key);
            onStyleChanged();
          }
        });
    comp.addNameChangeListener(
        new TextStyleComponent.NameChangeListener() {
          @Override
          public void onChange(String oldName, String newName, TextStyle style) {
            syntaxSettings.getComponentStyles().remove(oldName);
            syntaxSettings.addTextStyle(newName, style);
            onStyleChanged();
          }
        });
    styleList.add(sep, styleList.getComponentCount() - 1);
    styleList.add(comp, styleList.getComponentCount() - 1);
    onStyleChanged();
  }

  public void go() {
    frame.setVisible(true);
  }

  private void onSyntaxChanged() {
    syntaxList.revalidate();
  }

  private void onStyleChanged() {
    for (int i = 0; i < syntaxList.getComponentCount(); i++) {
      Component component = syntaxList.getComponent(i);
      if (!(component instanceof SyntaxPatternComponent)) continue;
      SyntaxPatternComponent patternComponent = (SyntaxPatternComponent) component;
      patternComponent.resetStyleNames(syntaxSettings.componentStyles.keySet());
    }
    styleList.revalidate();
  }
}

package org.jsoar.debugger.syntax.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jdesktop.swingx.JXColorSelectionButton;
import org.jsoar.debugger.syntax.TextStyle;

@SuppressWarnings("serial")
public class TextStyleComponent extends JPanel {

    private final JButton btnDelete;
    private final JTextField txtName;
    private String name;
    private TextStyle style;
    private NameChangeListener listener;

    public TextStyleComponent(String name, final TextStyle style){
        this.name = name;
        this.style = style;
        GridBagLayout mgr = new GridBagLayout();
        this.setLayout(mgr);

        this.setBorder(new EmptyBorder(5,5,5,5));

        final JCheckBox chkEnabled = new JCheckBox("Enabled?");
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx=0;
        constraints.gridy=2;
        constraints.anchor=GridBagConstraints.LINE_START;
        chkEnabled.setSelected(style.isEnabled());
        this.add(chkEnabled,constraints);

        constraints = new GridBagConstraints();
        constraints.gridx=0;
        constraints.gridy=0;
        constraints.anchor=GridBagConstraints.LINE_START;
        constraints.gridwidth=2;
        this.add(new JLabel("Name"),constraints);

        //type of thing to highlight
        txtName = new JTextField(name);
        constraints = new GridBagConstraints();
        constraints.gridx=0;
        constraints.gridy=1;
        constraints.gridwidth=2;
        constraints.anchor=GridBagConstraints.LINE_START;
        constraints.fill=GridBagConstraints.HORIZONTAL;
        txtName.setColumns(20);
        this.add(txtName,constraints);

        final JCheckBox chkBold = new JCheckBox("Bold");
        chkBold.setSelected(style.isBold());
        constraints = new GridBagConstraints();
        constraints.gridx=2;
        constraints.gridy=0;
        constraints.anchor=GridBagConstraints.LINE_START;
        this.add(chkBold,constraints);


        final JCheckBox chkItalic = new JCheckBox("Italic");
        chkItalic.setSelected(style.isItalic());
        constraints = new GridBagConstraints();
        constraints.gridx=2;
        constraints.gridy=1;
        constraints.anchor=GridBagConstraints.LINE_START;
        this.add(chkItalic,constraints);


        final JCheckBox chkUnderline = new JCheckBox("Underline");
        chkUnderline.setSelected(style.isUnderline());
        constraints = new GridBagConstraints();
        constraints.gridx=2;
        constraints.gridy=2;
        constraints.anchor=GridBagConstraints.LINE_START;
        this.add(chkUnderline,constraints);

        final JCheckBox chkStrike = new JCheckBox("Strikethrough");
        chkStrike.setSelected(style.isStrikethrough());
        constraints = new GridBagConstraints();
        constraints.gridx=2;
        constraints.gridy=3;
        constraints.anchor=GridBagConstraints.LINE_START;
        this.add(chkStrike,constraints);

        constraints.gridx=3;
        constraints.gridy=0;
        constraints.anchor=GridBagConstraints.LINE_START;
        this.add(new JLabel("Foreground"),constraints);

        final JXColorSelectionButton btnForeground = new JXColorSelectionButton(style.getForeground());
        btnForeground.getChooser().setColor(style.getForeground());
        constraints.gridx=3;
        constraints.gridy=1;
        constraints.anchor=GridBagConstraints.CENTER;
        this.add(btnForeground,constraints);

        constraints.gridx=3;
        constraints.gridy=2;
        constraints.anchor=GridBagConstraints.LINE_START;
        this.add(new JLabel("Background"),constraints);

        final JXColorSelectionButton btnBackground = new JXColorSelectionButton(style.getBackground());
        btnBackground.getChooser().setColor(style.getBackground());
        constraints.gridx=3;
        constraints.gridy=3;
        constraints.anchor=GridBagConstraints.CENTER;
        this.add(btnBackground,constraints);

        btnDelete = new JButton("Delete");
        constraints.gridx = 0;
        constraints.gridy=3;
        constraints.anchor=GridBagConstraints.LINE_START;
        this.add(btnDelete,constraints);

        ActionListener listener = e ->
        {
            style.setBold(chkBold.isSelected());
            style.setItalic(chkItalic.isSelected());
            style.setStrikethrough(chkStrike.isSelected());
            style.setUnderline(chkUnderline.isSelected());
            style.setEnabled(chkEnabled.isSelected());
            style.setStyleType(txtName.getText());
        };
        chkBold.addActionListener(listener);
        chkItalic.addActionListener(listener);
        chkUnderline.addActionListener(listener);
        chkStrike.addActionListener(listener);
        chkEnabled.addActionListener(listener);

        ChangeListener colorChangeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                style.setForeground(btnForeground.getChooser().getColor());
                style.setBackground(btnBackground.getChooser().getColor());
            }
        };
        
        btnForeground.getChooser().getSelectionModel().addChangeListener(colorChangeListener);
        btnBackground.getChooser().getSelectionModel().addChangeListener(colorChangeListener);

        txtName.addActionListener(listener);
        txtName.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateName();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateName();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateName();
            }
        });

    }

    public void updateName() {
        String newName = txtName.getText();
        style.setStyleType(newName);
        listener.onChange(name,newName,style);
        name = newName;
    }

    public void addDeleteButtonListener(ActionListener actionListener) {
        btnDelete.addActionListener(actionListener);
    }

    public void addNameChangeListener(NameChangeListener listener) {
        this.listener = listener;
    }

    public static abstract class NameChangeListener {
        public abstract void onChange(String oldName, String newName, TextStyle style);
    }
}

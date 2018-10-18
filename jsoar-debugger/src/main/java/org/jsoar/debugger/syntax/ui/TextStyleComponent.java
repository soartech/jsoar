package org.jsoar.debugger.syntax.ui;

import javafx.scene.control.CheckBox;
import org.jdesktop.swingx.JXColorSelectionButton;
import org.jsoar.debugger.syntax.TextStyle;

import javax.swing.*;
import java.awt.*;

public class TextStyleComponent extends JPanel {
    public TextStyleComponent(String name, TextStyle style){
        GridBagLayout mgr = new GridBagLayout();
        this.setLayout(mgr);


        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx=0;
        constraints.gridy=0;
        constraints.anchor=GridBagConstraints.LINE_START;
        this.add(new JLabel("Name"),constraints);

        //type of thing to highlight
        JTextField txtName = new JTextField(name);
        constraints = new GridBagConstraints();
        constraints.gridx=0;
        constraints.gridy=1;
        constraints.anchor=GridBagConstraints.LINE_START;
        constraints.fill=GridBagConstraints.HORIZONTAL;
        txtName.setColumns(18);
        this.add(txtName,constraints);

        JCheckBox chkBold = new JCheckBox("Bold");
        chkBold.setSelected(style.isBold());
        constraints = new GridBagConstraints();
        constraints.gridx=1;
        constraints.gridy=0;
        constraints.anchor=GridBagConstraints.LINE_START;
        this.add(chkBold,constraints);


        JCheckBox chkItalic = new JCheckBox("Italic");
        chkItalic.setSelected(style.isItalic());
        constraints = new GridBagConstraints();
        constraints.gridx=1;
        constraints.gridy=1;
        constraints.anchor=GridBagConstraints.LINE_START;
        this.add(chkItalic,constraints);


        JCheckBox chkUnderline = new JCheckBox("Underline");
        chkUnderline.setSelected(style.isItalic());
        constraints = new GridBagConstraints();
        constraints.gridx=1;
        constraints.gridy=2;
        constraints.anchor=GridBagConstraints.LINE_START;
        this.add(chkUnderline,constraints);

        JCheckBox chkStrike = new JCheckBox("Strikethrough");
        chkStrike.setSelected(style.isStrikethrough());
        constraints = new GridBagConstraints();
        constraints.gridx=1;
        constraints.gridy=3;
        constraints.anchor=GridBagConstraints.LINE_START;
        this.add(chkStrike,constraints);

        constraints.gridx=2;
        constraints.gridy=0;
        constraints.anchor=GridBagConstraints.LINE_START;
        this.add(new JLabel("Foreground"),constraints);

        JXColorSelectionButton btnForeground = new JXColorSelectionButton(style.getForeground());
        constraints.gridx=2;
        constraints.gridy=1;
        constraints.anchor=GridBagConstraints.CENTER;
        this.add(btnForeground,constraints);

        constraints.gridx=2;
        constraints.gridy=2;
        constraints.anchor=GridBagConstraints.LINE_START;
        this.add(new JLabel("Background"),constraints);

        JXColorSelectionButton btnBackground = new JXColorSelectionButton(style.getBackground());
        constraints.gridx=2;
        constraints.gridy=3;
        constraints.anchor=GridBagConstraints.CENTER;
        this.add(btnBackground,constraints);

        JButton btnDelete = new JButton("Delete");
        constraints.gridx = 0;
        constraints.gridy=3;
        constraints.anchor=GridBagConstraints.LINE_START;
        this.add(btnDelete,constraints);

    }
}

package org.jsoar.debugger.syntax.ui;

import org.jsoar.debugger.syntax.SyntaxPattern;

import javax.swing.*;
import java.awt.*;

public class SyntaxPatternComponent extends JPanel {
    public SyntaxPatternComponent(SyntaxPattern pattern) {
        GridBagLayout mgr = new GridBagLayout();
        this.setLayout(mgr);


        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx=0;
        constraints.gridy=0;
        constraints.anchor=GridBagConstraints.LINE_START;
        this.add(new JLabel("Name"),constraints);

        //syntax pattern name
        JTextField txtName = new JTextField(pattern.getName());

        constraints = new GridBagConstraints();
        constraints.gridx=0;
        constraints.gridy=1;
        constraints.anchor=GridBagConstraints.LINE_START;
        constraints.fill=GridBagConstraints.HORIZONTAL;
        txtName.setColumns(18);

        this.add(txtName,constraints);

        //regex
        constraints = new GridBagConstraints();
        constraints.gridx=1;
        constraints.gridy=0;
        constraints.anchor=GridBagConstraints.LINE_START;
        this.add(new JLabel("Regex"),constraints);


        //syntax pattern
        JTextField txtRegex = new JTextField(pattern.getRegex());
        constraints = new GridBagConstraints();
        constraints.gridx=1;
        constraints.gridy=1;
        constraints.anchor=GridBagConstraints.LINE_START;
        constraints.fill=GridBagConstraints.HORIZONTAL;
        txtRegex.setColumns(60);
        this.add(txtRegex,constraints);

        constraints = new GridBagConstraints();
        constraints.gridx=1;
        constraints.gridy=2;
        constraints.anchor=GridBagConstraints.LINE_START;

        JButton btnUpdate = new JButton("Update");
        this.add(btnUpdate,constraints);



        //capture groups
        //regex
        constraints = new GridBagConstraints();
        constraints.gridx=2;
        constraints.gridy=0;
        constraints.anchor=GridBagConstraints.LINE_START;

        this.add(new JLabel("Capture Groups"),constraints);

        //build row data
        String[][] rowData = new String[pattern.getComponents().size()][3];
        for (int i = 0; i < pattern.getComponents().size(); i++) {
            String style = pattern.getComponents().get(i);
            rowData[i][0] = "Group " + i;
            rowData[i][1] = style;
        }


        JTable tblCaptureGroups = new JTable(rowData,new String[]{"Number","Type"});
        constraints = new GridBagConstraints();
        constraints.gridx=2;
        constraints.gridy=1;
        constraints.gridheight=2;
        constraints.fill=GridBagConstraints.BOTH;
        this.add(tblCaptureGroups,constraints);
    }
}

package org.jsoar.debugger.syntax.ui;

import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;
import org.jdesktop.swingx.JXComboBox;
import org.jdesktop.swingx.JXTable;
import org.jsoar.debugger.syntax.SyntaxPattern;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

public class SyntaxPatternComponent extends JPanel {
    private Set<String> styleNames;
    private final SyntaxPattern pattern;
    private final CaptureGroupTableModel tableModel = new CaptureGroupTableModel();
    private final JXTable tblCaptureGroups;

    private static final Color goodBackground = new Color(102, 242, 96);
    private static final Color badBackground = new Color(242, 102, 96);
    private final JButton btnDelete = new JButton("Delete");

    public SyntaxPatternComponent(final SyntaxPattern pattern, Set<String> styleNames) {
        this.styleNames = styleNames;
        this.pattern = pattern;
        GridBagLayout mgr = new GridBagLayout();
        this.setLayout(mgr);

        this.setBorder(new EmptyBorder(5, 5, 5, 5));
        GridBagConstraints constraints;


        //regex label
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.LINE_START;
        this.add(new JLabel("Regex"), constraints);

        //regex
        final JTextField txtRegex = new JTextField(pattern.getRegex());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth=2;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        txtRegex.setColumns(50);
        this.add(txtRegex, constraints);

        //controls
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        JButton btnUpdate = new JButton("Test & Update");
        this.add(btnUpdate, constraints);

        final JCheckBox chkEnabled = new JCheckBox("Enabled?");
        constraints.gridx = 1;
        constraints.gridy = 2;
        constraints.anchor = GridBagConstraints.LINE_START;
        this.add(chkEnabled, constraints);

        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        this.add(btnDelete, constraints);

        //comment
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.LINE_START;
        this.add(new JLabel("Comment"), constraints);

        //syntax pattern comment
        final JTextArea txtComment = new JTextArea(pattern.getComment());
        constraints = new GridBagConstraints();
        constraints.gridx = 2;
        constraints.gridy = 1;
        constraints.gridheight = 3;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        txtComment.setColumns(20);
        txtComment.setRows(6);
        this.add(txtComment, constraints);




        //capture groups
        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.LINE_START;
        this.add(new JLabel("Capture Groups"), constraints);

        tblCaptureGroups = new JXTable(tableModel);
        //combo box column
        resetStyleNames(styleNames);
        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 1;
        constraints.gridheight = 3;
        constraints.fill = GridBagConstraints.BOTH;
        tblCaptureGroups.packAll();
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.add(tblCaptureGroups, BorderLayout.CENTER);
        jPanel.add(tblCaptureGroups.getTableHeader(), BorderLayout.NORTH);
        this.add(jPanel, constraints);

        //handlers
        btnUpdate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Pattern p = Pattern.compile(txtRegex.getText());
                    int groupCount = p.groupCount();
                    while (groupCount > pattern.getComponents().size()) {
                        pattern.getComponents().add("");
                    }
                    while (groupCount < pattern.getComponents().size()) {
                        pattern.getComponents().remove(pattern.getComponents().size() - 1);
                    }
                    pattern.setRegex(txtRegex.getText());
                    tableModel.fireTableDataChanged();
                    txtRegex.setBackground(goodBackground);
                } catch (PatternSyntaxException ignored) {
                    txtRegex.setBackground(badBackground);
                }
            }
        });

        chkEnabled.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pattern.setEnabled(chkEnabled.isEnabled());
            }
        });
        txtComment.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                pattern.setComment(txtComment.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                pattern.setComment(txtComment.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                pattern.setComment(txtComment.getText());
            }
        });

    }

    public void resetStyleNames(Set<String> styleNames) {
        TableColumn styleColumn = tblCaptureGroups.getColumnModel().getColumn(1);

        JXComboBox comboBox = new JXComboBox();

        for (String name : styleNames) {
            comboBox.addItem(name);
        }
        styleColumn.setCellEditor(new DefaultCellEditor(comboBox));
    }

    public void addDeleteButtonListener(ActionListener actionListener) {
        btnDelete.addActionListener(actionListener);
    }


    private class CaptureGroupTableModel extends AbstractTableModel {
        private String[] columnNames = {
                "Capture Group",
                "Style Name"};

        @Override
        public int getRowCount() {
            return pattern.getComponents().size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return "Group " + rowIndex;
            } else {
                return pattern.getComponents().get(rowIndex);
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex != 0;
        }

        @Override
        public void setValueAt(Object newValue, int rowIndex, int columnIndex) {
            if (columnIndex == 1) {
                pattern.getComponents().set(rowIndex, newValue.toString());
            }
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

    }
}

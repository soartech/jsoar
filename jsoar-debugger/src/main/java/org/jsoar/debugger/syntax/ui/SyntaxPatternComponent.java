package org.jsoar.debugger.syntax.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import org.jdesktop.swingx.JXTable;
import org.jsoar.debugger.JSoarDebugger;
import org.jsoar.debugger.syntax.SyntaxPattern;

import com.google.re2j.Pattern;
import com.google.re2j.PatternSyntaxException;

@SuppressWarnings("serial")
public class SyntaxPatternComponent extends JPanel
{
    @SuppressWarnings("unused")
    private Set<String> styleNames;
    private final SyntaxPattern pattern;
    private JSoarDebugger debugger;
    private final CaptureGroupTableModel tableModel = new CaptureGroupTableModel();
    private final JXTable tblCaptureGroups;
    
    private static final Color GOOD_BACKGROUND = new Color(102, 242, 96);
    private static final Color BAD_BACKGROUND = new Color(242, 102, 96);
    private final JButton btnDelete = new JButton("Delete");
    
    public SyntaxPatternComponent(final SyntaxPattern pattern, Set<String> styleNames, final JSoarDebugger debugger)
    {
        this.styleNames = styleNames;
        this.pattern = pattern;
        this.debugger = debugger;
        GridBagLayout mgr = new GridBagLayout();
        this.setLayout(mgr);
        
        this.setBorder(new EmptyBorder(5, 5, 5, 5));
        GridBagConstraints constraints;
        
        // regex label
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.LINE_START;
        this.add(new JLabel("Regex"), constraints);
        
        // regex
        final JTextField txtRegex = new JTextField(pattern.getRegex());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 3;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        txtRegex.setColumns(45);
        this.add(txtRegex, constraints);
        
        // controls
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
        chkEnabled.setSelected(pattern.isEnabled());
        this.add(chkEnabled, constraints);
        
        final JCheckBox chkImportant = new JCheckBox("Always Instant?");
        constraints.gridx = 2;
        constraints.gridy = 2;
        constraints.anchor = GridBagConstraints.LINE_START;
        chkEnabled.setSelected(pattern.isImportant());
        this.add(chkImportant, constraints);
        
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        this.add(btnDelete, constraints);
        
        // comment
        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.LINE_START;
        this.add(new JLabel("Comment"), constraints);
        
        // syntax pattern comment
        final JTextArea txtComment = new JTextArea(pattern.getComment());
        constraints = new GridBagConstraints();
        constraints.gridx = 3;
        constraints.gridy = 1;
        constraints.gridheight = 3;
        constraints.anchor = GridBagConstraints.FIRST_LINE_START;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        txtComment.setColumns(20);
        txtComment.setRows(6);
        txtComment.setLineWrap(true);
        this.add(txtComment, constraints);
        
        // capture groups
        constraints = new GridBagConstraints();
        constraints.gridx = 4;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.LINE_START;
        this.add(new JLabel("Capture Groups"), constraints);
        
        tblCaptureGroups = new JXTable(tableModel);
        // combo box column
        resetStyleNames(styleNames);
        constraints = new GridBagConstraints();
        constraints.gridx = 4;
        constraints.gridy = 1;
        constraints.gridheight = 3;
        constraints.fill = GridBagConstraints.BOTH;
        // tblCaptureGroups.packAll();
        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.add(tblCaptureGroups, BorderLayout.CENTER);
        jPanel.add(tblCaptureGroups.getTableHeader(), BorderLayout.NORTH);
        this.add(jPanel, constraints);
        
        // handlers
        btnUpdate.addActionListener(e ->
        {
            SyntaxPattern testSyntax = new SyntaxPattern();
            testSyntax.setRegex(txtRegex.getText());
            testSyntax.expandMacros(SyntaxPatternComponent.this.debugger);
            String text = testSyntax.getExpandedRegex();
            try
            {
                Pattern p = Pattern.compile(text);
                int groupCount = p.groupCount();
                while(groupCount > pattern.getComponents().size())
                {
                    pattern.getComponents().add("");
                }
                while(groupCount < pattern.getComponents().size())
                {
                    pattern.getComponents().remove(pattern.getComponents().size() - 1);
                }
                pattern.setRegex(txtRegex.getText());
                tableModel.fireTableDataChanged();
                txtRegex.setBackground(GOOD_BACKGROUND);
                txtRegex.setToolTipText("<html><b>Detected " + groupCount + " groups in pattern:</b><br>" + text + "</html>");
                
            }
            catch(PatternSyntaxException ex)
            {
                txtRegex.setBackground(BAD_BACKGROUND);
                txtRegex.setToolTipText("<html><b>" + ex.getDescription() + "</b><br>" + text + "</html>");
            }
        });
        
        chkEnabled.addActionListener(e -> pattern.setEnabled(chkEnabled.isSelected()));
        chkImportant.addActionListener(e -> pattern.setImportant(chkImportant.isSelected()));
        
        txtComment.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                pattern.setComment(txtComment.getText());
            }
            
            @Override
            public void removeUpdate(DocumentEvent e)
            {
                pattern.setComment(txtComment.getText());
            }
            
            @Override
            public void changedUpdate(DocumentEvent e)
            {
                pattern.setComment(txtComment.getText());
            }
        });
        
    }
    
    public void resetStyleNames(Set<String> styleNames)
    {
        TableColumn styleColumn = tblCaptureGroups.getColumnModel().getColumn(1);
        
        JComboBox<String> comboBox = new JComboBox<>();
        comboBox.addPopupMenuListener(new ExpandingWidthComboBoxListener(true, false));
        // comboBox.setPrototypeDisplayValue("Use this for width because we need a fixed width");//will use this string to set max width of the combo box
        comboBox.setMaximumSize(comboBox.getPreferredSize());
        for(String name : styleNames)
        {
            comboBox.addItem(name);
        }
        styleColumn.setCellEditor(new DefaultCellEditor(comboBox));
    }
    
    public void addDeleteButtonListener(ActionListener actionListener)
    {
        btnDelete.addActionListener(actionListener);
    }
    
    private class CaptureGroupTableModel extends AbstractTableModel
    {
        private String[] columnNames = {
                "Capture Group",
                "Style Name" };
        
        @Override
        public int getRowCount()
        {
            return pattern.getComponents().size();
        }
        
        @Override
        public int getColumnCount()
        {
            return 2;
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            if(columnIndex == 0)
            {
                return "Group " + rowIndex;
            }
            else
            {
                return pattern.getComponents().get(rowIndex);
            }
        }
        
        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return columnIndex != 0;
        }
        
        @Override
        public void setValueAt(Object newValue, int rowIndex, int columnIndex)
        {
            if(columnIndex == 1)
            {
                pattern.getComponents().set(rowIndex, newValue.toString());
            }
        }
        
        @Override
        public String getColumnName(int col)
        {
            return columnNames[col];
        }
        
    }
}

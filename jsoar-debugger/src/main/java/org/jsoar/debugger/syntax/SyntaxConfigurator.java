package org.jsoar.debugger.syntax;

import org.jdesktop.swingx.HorizontalLayout;

import javax.swing.*;
import java.awt.*;

public class SyntaxConfigurator{

    private final ListCellRenderer<SyntaxPattern> syntaxCellRenderer = new ListCellRenderer<SyntaxPattern>() {
        @Override
        public Component getListCellRendererComponent(JList<? extends SyntaxPattern> list, SyntaxPattern pattern, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel component = new JPanel();
            GridBagLayout mgr = new GridBagLayout();
            component.setLayout(mgr);


            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx=0;
            constraints.gridy=0;
            constraints.anchor=GridBagConstraints.LINE_START;
            component.add(new JLabel("Name"),constraints);

            //syntax pattern name
            JTextField txtName = new JTextField(pattern.getName());
            constraints = new GridBagConstraints();
            constraints.gridx=0;
            constraints.gridy=1;
            constraints.anchor=GridBagConstraints.LINE_START;
            constraints.fill=GridBagConstraints.HORIZONTAL;
            component.add(txtName,constraints);

            //regex
            constraints = new GridBagConstraints();
            constraints.gridx=1;
            constraints.gridy=0;
            constraints.anchor=GridBagConstraints.LINE_START;

            component.add(new JLabel("Regex"),constraints);


            //syntax pattern
            JTextField txtRegex = new JTextField(pattern.getName());
            constraints = new GridBagConstraints();
            constraints.gridx=1;
            constraints.gridy=1;
            constraints.anchor=GridBagConstraints.LINE_START;
            constraints.fill=GridBagConstraints.HORIZONTAL;
            component.add(txtRegex,constraints);

            //capture groups
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
            constraints.gridy=0;
            constraints.gridheight=2;
            constraints.fill=GridBagConstraints.BOTH;
            component.add(tblCaptureGroups,constraints);

            return component;
        }
    };

    private final ListCellRenderer<String> styleRenderer = new ListCellRenderer<String>() {
        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String styleName, int index, boolean isSelected, boolean cellHasFocus) {
            TextStyle style = syntaxSettings.getComponentStyles().get(styleName);




            return null;
        }
    };
    private final SyntaxSettings syntaxSettings;

    JButton btnOk = new JButton("Ok");
    JButton btnCancel = new JButton("Cancel");
    private JList<SyntaxPattern> syntaxList ;
    private JList<String> styleList;

    private final JFrame frame;

    public SyntaxConfigurator(SyntaxSettings syntaxSettings){
        this.syntaxSettings = syntaxSettings;

        frame = new JFrame("Syntax Settings");
        frame.setBounds(100,100,800,600);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new HorizontalLayout());
        bottomPanel.add(btnOk);
        bottomPanel.add(btnCancel);


        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        panel.add(bottomPanel,BorderLayout.PAGE_END);

        DefaultListModel<SyntaxPattern> listmod = new DefaultListModel<>();
        for(SyntaxPattern pattern: syntaxSettings.getSyntaxPatterns()){
            listmod.addElement(pattern);
        }
        syntaxList = new JList<>(listmod);
        syntaxList.setCellRenderer(syntaxCellRenderer);
        syntaxList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);


        JScrollPane scrollPane = new JScrollPane(syntaxList);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setVisible(true);
        panel.add(scrollPane,BorderLayout.CENTER);

        frame.getContentPane().add(panel);
        frame.pack();

    }
    public void go(){
        frame.setVisible(true);
    }


}

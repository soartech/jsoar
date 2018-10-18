package org.jsoar.debugger.syntax.ui;

import org.jdesktop.swingx.HorizontalLayout;
import org.jdesktop.swingx.VerticalLayout;
import org.jsoar.debugger.syntax.SyntaxPattern;
import org.jsoar.debugger.syntax.SyntaxSettings;
import org.jsoar.debugger.syntax.TextStyle;

import javax.swing.*;
import java.awt.*;

public class SyntaxConfigurator{

    private final ListCellRenderer<SyntaxPattern> syntaxCellRenderer = new ListCellRenderer<SyntaxPattern>() {
        @Override
        public Component getListCellRendererComponent(JList<? extends SyntaxPattern> list, SyntaxPattern pattern, int index, boolean isSelected, boolean cellHasFocus) {
            JPanel component = new SyntaxPatternComponent(pattern);


            return component;
        }
    };

    private final ListCellRenderer<String> styleRenderer = new ListCellRenderer<String>() {
        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String styleName, int index, boolean isSelected, boolean cellHasFocus) {
            TextStyle style = syntaxSettings.getComponentStyles().get(styleName);
            JPanel component = new TextStyleComponent(styleName,style);

            return component;
        }
    };
    private final SyntaxSettings syntaxSettings;

    JButton btnOk = new JButton("Ok");
    JButton btnCancel = new JButton("Cancel");
    private JPanel syntaxList;
    private JPanel styleList;

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

        syntaxList = new JPanel();
        syntaxList.setLayout(new VerticalLayout());
        for(SyntaxPattern pattern: syntaxSettings.getSyntaxPatterns()){
            syntaxList.add(new SyntaxPatternComponent(pattern));
        }

        JScrollPane scrollPane = new JScrollPane(syntaxList);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setVisible(true);
        panel.add(scrollPane,BorderLayout.EAST);

        styleList = new JPanel();
        styleList.setLayout(new VerticalLayout());
        for(String key: syntaxSettings.getComponentStyles().keySet()){
            styleList.add(new TextStyleComponent(key,syntaxSettings.getComponentStyles().get(key)));
        }

        scrollPane = new JScrollPane(styleList);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setVisible(true);
        panel.add(scrollPane,BorderLayout.WEST);


        frame.getContentPane().add(panel);
        frame.pack();
    }
    public void go(){
        frame.setVisible(true);
    }


}

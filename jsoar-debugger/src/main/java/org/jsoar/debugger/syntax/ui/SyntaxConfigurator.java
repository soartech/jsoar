package org.jsoar.debugger.syntax.ui;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.VerticalLayout;
import org.jsoar.debugger.TraceView;
import org.jsoar.debugger.syntax.SyntaxPattern;
import org.jsoar.debugger.syntax.SyntaxSettings;
import org.jsoar.debugger.syntax.TextStyle;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.LinkedList;

public class SyntaxConfigurator{

    private final SyntaxSettings syntaxSettings;
    private TraceView parent;
    private final JXButton btnApply = new JXButton("Apply");
    private final JXButton btnOk = new JXButton("Ok");
    private final JXButton btnCancel = new JXButton("Cancel");
    private final JXButton btnAddRegex = new JXButton("Add Regex");
    private final JXButton btnAddStyle = new JXButton("Add Style");
    private JPanel syntaxList;
    private JPanel styleList;

    private final JFrame frame;

    public SyntaxConfigurator(final SyntaxSettings syntaxSettings, final TraceView parent){
        this.syntaxSettings = syntaxSettings;
        this.parent = parent;

        frame = new JFrame("Syntax Settings");
        frame.setBounds(100,100,800,600);

        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel,BoxLayout.LINE_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottomPanel.add(Box.createHorizontalGlue());
        bottomPanel.add(btnOk);
        bottomPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        bottomPanel.add(btnCancel);
        bottomPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        bottomPanel.add(btnApply);


        final JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());

        panel.add(bottomPanel,BorderLayout.PAGE_END);

        syntaxList = new JPanel();
        syntaxList.setLayout(new VerticalLayout());
        LinkedList<SyntaxPattern> syntaxPatterns = syntaxSettings.getSyntaxPatterns();
        for (Iterator<SyntaxPattern> iterator = syntaxPatterns.iterator(); iterator.hasNext(); ) {
            final SyntaxPattern pattern = iterator.next();
            final SyntaxPatternComponent comp = new SyntaxPatternComponent(pattern, syntaxSettings.componentStyles.keySet());
            final JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
            syntaxList.add(sep);

            comp.addDeleteButtonListener(new ActionListener() {
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

        JScrollPane scrollPane = new JScrollPane(syntaxList);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setVisible(true);
        panel.add(scrollPane,BorderLayout.WEST);

        styleList = new JPanel();
        styleList.setLayout(new VerticalLayout());
        for (Iterator<String> iterator = syntaxSettings.getComponentStyles().keySet().iterator(); iterator.hasNext(); ) {
            final String key = iterator.next();
            final TextStyleComponent comp = new TextStyleComponent(key, syntaxSettings.getComponentStyles().get(key));
            final JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
            styleList.add(sep);
            comp.addDeleteButtonListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    styleList.remove(comp);
                    styleList.remove(sep);
                    syntaxSettings.getComponentStyles().remove(key);
                    onStyleChanged();
                }
            });
            styleList.add(comp);
        }
        styleList.add(btnAddStyle);

        scrollPane = new JScrollPane(styleList);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setVisible(true);
        panel.add(scrollPane,BorderLayout.EAST);


        frame.getContentPane().add(panel);
        frame.pack();


        btnAddRegex.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SyntaxPattern newPattern = new SyntaxPattern();
                syntaxSettings.getSyntaxPatterns().add(newPattern);
                syntaxList.add(new JSeparator(JSeparator.HORIZONTAL), syntaxList.getComponentCount()-1);
                syntaxList.add(new SyntaxPatternComponent(newPattern,syntaxSettings.componentStyles.keySet()), syntaxList.getComponentCount()-1);
                onSyntaxChanged();
            }
        });

        btnAddStyle.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TextStyle newStyle = new TextStyle();
                String key = "new style";
                int i = 1;
                while(syntaxSettings.getComponentStyles().containsKey(key)) {
                    key = "new style "+i;
                    i++;
                }
                syntaxSettings.addTextStyle(key,newStyle);
                styleList.add(new JSeparator(JSeparator.HORIZONTAL), styleList.getComponentCount()-1);
                styleList.add(new TextStyleComponent(key,newStyle), styleList.getComponentCount()-1);
                onStyleChanged();
            }
        });

        btnApply.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parent.reformatText();
            }
        });

        btnOk.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parent.saveSyntax();
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }
        });

        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                parent.reloadSyntax();
                parent.reformatText();
                frame.dispatchEvent(new WindowEvent(frame,WindowEvent.WINDOW_CLOSING));
            }
        });
    }
    public void go(){
        frame.setVisible(true);
    }


    private void onSyntaxChanged(){
        syntaxList.revalidate();
    }

    private void onStyleChanged(){
        for (int i = 0; i < syntaxList.getComponentCount(); i++){
            Component component = syntaxList.getComponent(i);
            if (!(component instanceof SyntaxPatternComponent))
                continue;
            SyntaxPatternComponent patternComponent = (SyntaxPatternComponent) component;
            patternComponent.resetStyleNames(syntaxSettings.componentStyles.keySet());
        }
        styleList.revalidate();

    }

}

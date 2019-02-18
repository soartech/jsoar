package org.jsoar.debugger.syntax;

import org.jsoar.debugger.JSoarDebugger;
import org.jsoar.util.Prefs;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.TreeSet;


public class Highlighter
{
    private static Highlighter highlighter;
    private SyntaxSettings patterns;
    private AttributeSet defaultAttributes = new SimpleAttributeSet();

    private JSoarDebugger debugger;

    private Highlighter(JSoarDebugger debugger){
        this.debugger = debugger;
        reloadSyntax();
    }


    public static Highlighter getInstance(JSoarDebugger debugger){
        if (highlighter == null){
            highlighter = new Highlighter(debugger);
        }
        return highlighter;
    }

    public void setDefaultTextStyle(JTextPane outputWindow)
    {
        Color backgroundColor = patterns.getBackground();
        outputWindow.setBackground(backgroundColor);
        outputWindow.setSelectionColor(patterns.getSelection());
        UIDefaults defaults = new UIDefaults();
        defaults.put("TextPane[Enabled].backgroundPainter", backgroundColor);

        outputWindow.putClientProperty("Nimbus.Overrides", defaults);
        outputWindow.putClientProperty("Nimbus.Overrides.InheritDefaults", true);
        outputWindow.setBackground(backgroundColor);
        Color defaultTextColor = patterns.getForeground();
        outputWindow.setForeground(defaultTextColor);
    }

    public AttributeSet getDefaultAttributes(){
        return defaultAttributes;
    }

    public SyntaxSettings getPatterns(){
        return patterns;
    }

    public void reloadSyntax()
    {
        patterns = Prefs.loadSyntax();

        if (patterns == null) {
            patterns = Prefs.loadDefaultSyntax();
            Prefs.storeSyntax(patterns);
        }
        patterns.expandAllMacros(debugger);
        defaultAttributes = StyleContext.getDefaultStyleContext().addAttribute(new SimpleAttributeSet(), StyleConstants.Foreground, patterns.getForeground());
    }

    public SyntaxSettings reloadSyntaxDefaults()
    {
        patterns = Prefs.loadDefaultSyntax();
        patterns.expandAllMacros(debugger);
        return patterns;
    }

    public void save()
    {
        Prefs.storeSyntax(patterns);
    }

    public void formatText(JTextPane outputWindow)
    {
        DefaultStyledDocument styledDocument = (DefaultStyledDocument) outputWindow.getStyledDocument();
        try {
            final String str = styledDocument.getText(0, styledDocument.getLength());

            final long time = System.currentTimeMillis();
            final TreeSet<StyleOffset> styles = highlighter.getPatterns().getForAll(str, debugger);


            System.out.println("Processing buffer with size " + str.length() + " took " + (System.currentTimeMillis() - time));
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    int caretPosition = outputWindow.getCaretPosition();
                    try {
                        setDefaultTextStyle(outputWindow);
                        if (styles.isEmpty()) {
                            return;
                        } else {
                            outputWindow.setDocument(new DefaultStyledDocument());
                            Position startPosition = styledDocument.getStartPosition();
                            //reset default text color and size
                            styledDocument.replace(0,str.length(),str,highlighter.getDefaultAttributes());
                            int index = 0;
                            for (StyleOffset offset : styles) {
                                int start = offset.start;
                                int end = offset.end;
                                //don't apply any matches that start before the end of our last match, or that have length 0
                                if (start >= end || start < index) {
                                    continue;
                                }
                                //the matched stuff
                                int offsetStart = startPosition.getOffset() + start;
                                int offsetEnd = startPosition.getOffset() + (end - start);
//                                System.out.println("Replacing between " + offsetStart + " and " + offsetEnd + " for string " + str.substring(start, end));

                                styledDocument.replace(offsetStart, offsetEnd, str.substring(start, end), offset.style);
                                index = end;
                            }
                            outputWindow.setDocument(styledDocument);
                        }

                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                    outputWindow.setCaretPosition(caretPosition);
                    //System.out.println("Printing buffer with size " + str.length() + " took " + (System.currentTimeMillis() - time));
                }

            });
        } catch (BadLocationException ignored) {
        }
    }
}

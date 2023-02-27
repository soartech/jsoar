package org.jsoar.debugger.syntax;

import java.awt.Color;

import javax.swing.JTextPane;
import javax.swing.UIDefaults;
import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;

import org.jsoar.debugger.JSoarDebugger;
import org.jsoar.debugger.util.Prefs;

public class Highlighter
{
    private static Highlighter highlighter;
    private SyntaxSettings patterns;
    private AttributeSet defaultAttributes = new SimpleAttributeSet();
    
    private JSoarDebugger debugger;
    
    private Highlighter(JSoarDebugger debugger)
    {
        this.debugger = debugger;
        reloadSyntax();
    }
    
    public static Highlighter getInstance(JSoarDebugger debugger)
    {
        if(highlighter == null)
        {
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
    
    public AttributeSet getDefaultAttributes()
    {
        return defaultAttributes;
    }
    
    public SyntaxSettings getPatterns()
    {
        return patterns;
    }
    
    public void reloadSyntax()
    {
        patterns = Prefs.loadSyntax();
        
        if(patterns == null)
        {
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
}

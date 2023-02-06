package org.jsoar.debugger.syntax;

import java.awt.Color;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.text.SimpleAttributeSet;

import org.jsoar.debugger.JSoarDebugger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;

public class SyntaxSettings {
    public HashMap<String, TextStyle> componentStyles = new HashMap<>();
    public LinkedList<SyntaxPattern> syntaxPatterns = new LinkedList<>();
    private Color foreground;
    private Color background;
    private Color selection;
    
    private Map<String,Pattern> patternMap = new HashMap<>();

    public SyntaxSettings() {

    }

    /**
     * generates a list of all style matches for this pattern that occur in this string.
     * @param input The string to match against
     * @param syntax The syntax pattern object containing the regex
     * @param styles A list of styles to be applied based on the syntax pattern
     * @param debugger A debugger instance (needed to get the interpreter to expand macros)
     * @return A list of StyleOffset objects containing a text style and position
     */
    public synchronized List<StyleOffset> matchAll(String input, SyntaxPattern syntax, HashMap<String, TextStyle> styles, JSoarDebugger debugger) {
        List<StyleOffset> matches = new LinkedList<>();

        String regex = syntax.getExpandedRegex();
        List<String> components = syntax.getComponents();
        try {
            Pattern pattern = retrievePattern(regex);
            Matcher m = pattern.matcher(input);
            while (m.find()) {
                int groupCount = m.groupCount();
                
                // Why are there two loops here (they look identical, the for version should work for the groupCount == 1 case)
                if (groupCount == 1) {
                    int start = m.start(1);
                    int end = m.end(1);
                    if (components.isEmpty()) {
                        matches.add(new StyleOffset(start, end, SimpleAttributeSet.EMPTY));
                    } else {
                        String type = components.get(0);
                        TextStyle style = styles.get(type);
                        if (style == null || !style.isEnabled()) {
                            matches.add(new StyleOffset(start, end, SimpleAttributeSet.EMPTY));
                        } else {
                            matches.add(new StyleOffset(start, end, style.getAttributes()));
                        }
                    }
                } else {
                    for (int i = 1; i < groupCount + 1; i++) {//start with match 1, because match 0 is the whole regex not the capture group
                        int start = m.start(i);
                        int end = m.end(i);
                        if (components.size() < i) {
                            matches.add(new StyleOffset(start, end, SimpleAttributeSet.EMPTY));
                        } else {
                            String type = components.get(i - 1);
                            TextStyle style = styles.get(type);
                            if (style == null) {
                                matches.add(new StyleOffset(start, end, SimpleAttributeSet.EMPTY));
                            } else {
                                matches.add(new StyleOffset(start, end, style.getAttributes()));
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.err.println("attempted regex: " + regex);
        }

        return matches;
    }

    /**
     * Handles caching compiled patterns. Compiling regex's to patterns over and over is expensive
     */
    private Pattern retrievePattern(String regex)
    {
        Pattern retval = this.patternMap.get(regex);
        if ( retval == null ) {
            retval = Pattern.compile(regex);
            this.patternMap.put(regex, retval);
        }
        return retval;
    }

    /**
     * Generate a TreeSet (which will be iterated through in sorted order) of this syntax across the input string
     * @param str The string to apply syntax highlighting to
     * @param debugger A debugger instance, used for macro expansion
     * @return A TreeSet of style offsets
     */
    public TreeSet<StyleOffset> getForAll(String str, JSoarDebugger debugger) {
        TreeSet<StyleOffset> offsets = new TreeSet<>();
//        if ( str.isEmpty() || str.isBlank() ) {
//            return offsets;
//        }
        
        for (SyntaxPattern pattern : syntaxPatterns) {
            if (pattern.isEnabled()) {
                offsets.addAll(matchAll(str, pattern, componentStyles, debugger));
            }
        }
        return offsets;
    }


    public HashMap<String, TextStyle> getComponentStyles() {
        return componentStyles;
    }

    public void setComponentStyles(HashMap<String, TextStyle> componentStyles) {
        this.componentStyles = componentStyles;
    }

    public LinkedList<SyntaxPattern> getSyntaxPatterns() {
        return syntaxPatterns;
    }

    public void setSyntaxPatterns(LinkedList<SyntaxPattern> syntaxPatterns) {
        this.syntaxPatterns = syntaxPatterns;
    }

    public void addTextStyle(String name, TextStyle style) {
        componentStyles.put(name, style);
    }

    public void addPattern(SyntaxPattern pattern) {
        syntaxPatterns.add(pattern);
    }

    public void expandAllMacros(JSoarDebugger debugger)
    {
        for(SyntaxPattern pattern: syntaxPatterns){
            pattern.expandMacros(debugger);
        }
    }

    @JsonIgnore
    public Color getForeground() {
        return foreground;
    }

    public void setForeground(Color foreground) {
        this.foreground = foreground;
    }

    @JsonIgnore
    public Color getBackground() {
        return background;
    }

    public void setBackground(Color background) {
        this.background = background;
    }

    @JsonIgnore
    public Color getSelection() {
        return selection;
    }

    public void setSelection(Color selection) {
        this.selection = selection;
    }
    
    public void setForegroundRgb(float[] components) {
        foreground = new Color(components[0], components[1], components[2], components[3]);
    }

    public float[] getForegroundRgb() {
        return foreground.getRGBComponents(null);
    }

    public void setBackgroundRgb(float[] components) {
        background = new Color(components[0], components[1], components[2], components[3]);
    }

    public float[] getBackgroundRgb() {
        return background.getRGBComponents(null);
    }
    
    public void setSelectionRgb(float[] components) {
        selection = new Color(components[0], components[1], components[2], components[3]);
    }

    public float[] getSelectionRgb() {
        return selection.getRGBComponents(null);
    }
}

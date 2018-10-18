package org.jsoar.debugger.syntax;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;

import javax.swing.text.SimpleAttributeSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

public class SyntaxSettings {
    public HashMap<String, TextStyle> componentStyles = new HashMap<>();
    public LinkedList<SyntaxPattern> syntaxPatterns = new LinkedList<>();

    public SyntaxSettings() {

    }

    public synchronized List<StyleOffset> matchAll(String input, SyntaxPattern syntax, HashMap<String, TextStyle> styles) {
        List<StyleOffset> matches = new LinkedList<>();
        String regex = syntax.getRegex();
        List<String> components = syntax.getComponents();
        try {
            Pattern pattern = Pattern.compile(regex);
            Matcher m = pattern.matcher(input);
            while (m.find()) {
                int groupCount = m.groupCount();
                if (groupCount == 1) {
                    int start = m.start();
                    int end = m.end();
                    if (components.isEmpty()) {
                        matches.add(new StyleOffset(start, end, new SimpleAttributeSet()));
                    } else {
                        String type = components.get(0);
                        TextStyle style = styles.get(type);
                        if (style == null) {
                            matches.add(new StyleOffset(start, end, new SimpleAttributeSet()));
                        } else {
                            matches.add(new StyleOffset(start, end, style.getAttributes()));
                        }
                    }
                } else {
                    for (int i = 1; i < groupCount; i++) {
                        int start = m.start(i);
                        int end = m.end(i);
                        if (components.size() < i) {
                            matches.add(new StyleOffset(start, end, new SimpleAttributeSet()));
                        } else {
                            String type = components.get(i - 1);
                            TextStyle style = styles.get(type);
                            if (style == null) {
                                matches.add(new StyleOffset(start, end, new SimpleAttributeSet()));
                            } else {
                                matches.add(new StyleOffset(start, end, style.getAttributes()));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return matches;
    }

    public TreeSet<StyleOffset> getForAll(String str) {
        TreeSet<StyleOffset> offsets = new TreeSet<>();
        for (SyntaxPattern pattern : syntaxPatterns) {
            offsets.addAll(matchAll(str, pattern, componentStyles));
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

    public void addTextStyle(String phase, TextStyle style) {
        componentStyles.put(phase,style);
    }

    public void addPattern(SyntaxPattern pattern){
        syntaxPatterns.add(pattern);
    }
}

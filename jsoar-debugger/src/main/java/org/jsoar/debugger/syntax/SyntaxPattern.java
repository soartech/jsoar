package org.jsoar.debugger.syntax;


import com.google.re2j.Matcher;
import com.google.re2j.Pattern;

import javax.swing.text.AttributeSet;
import java.util.ArrayList;
import java.util.List;


public class SyntaxPattern {
    private String regex;
    private AttributeSet[] styling;
    private List<StyleOffset> matches = new ArrayList<>();

    public SyntaxPattern(String regex, AttributeSet[] styling) {
        this.regex = regex;
        this.styling = styling;
    }

    public synchronized List<StyleOffset> matchAll(String input, AttributeSet defaultAttributes) {
        matches.clear();
        try {
            Pattern pattern = Pattern.compile(regex);
            Matcher m = pattern.matcher(input);
            while (m.find()) {
                int groupCount = m.groupCount();
                if (groupCount == 1) {
                    matches.add(new StyleOffset(m.start(),m.end(),styling[0]));
                } else {
                    for (int i = 1; i < groupCount; i++) {
                        int start = m.start(i);
                        int end = m.end(i);
                        matches.add(new StyleOffset(start, end, styling[i-1]));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return matches;
    }

    public class StyleOffset implements Comparable<StyleOffset>{
        public int start = 0, end = 0;
        public AttributeSet style;

        public StyleOffset(int start, int end, AttributeSet attributeSet) {
            this.start = start;
            this.end = end;
            this.style = attributeSet;
        }

        @Override
        public int compareTo(StyleOffset o) {
            return start - o.start;
        }
    }
}

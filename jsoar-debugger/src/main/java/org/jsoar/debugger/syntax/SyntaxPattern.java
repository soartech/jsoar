package org.jsoar.debugger.syntax;


import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import javax.swing.text.AttributeSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;


public class SyntaxPattern {
    private String regex;
    private AttributeSet[] styling;


    public SyntaxPattern(String regex, AttributeSet[] styling) {
        this.regex = regex;
        this.styling = styling;
    }


    public synchronized List<StyleOffset> matchAll(String input, AttributeSet defaultAttributes) {
        List<StyleOffset> matches = new LinkedList<>();
        try {
            Pattern pattern = Pattern.compile(regex);
            Matcher m = pattern.matcher(input);
            while (m.find()) {
                int groupCount = m.groupCount();
                if (groupCount == 1) {
                    matches.add(new StyleOffset(m.start(), m.end(), styling[0]));
                } else {
                    for (int i = 1; i < groupCount; i++) {
                        int start = m.start(i);
                        int end = m.end(i);
                        matches.add(new StyleOffset(start, end, styling[i - 1]));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return matches;
    }

    public static TreeSet<StyleOffset> getForAll(String str, List<SyntaxPattern> patterns, AttributeSet defaultAttributes) {
        TreeSet<StyleOffset> offsets = new TreeSet<>();
        for (SyntaxPattern pattern : patterns) {
            offsets.addAll(pattern.matchAll(str, defaultAttributes));
        }
        return offsets;
    }


    @SafeVarargs
    public static TreeSet<StyleOffset> mergeAll(List<StyleOffset>... offsets) {
        TreeSet<StyleOffset> list = new TreeSet<>();
        for (List<StyleOffset> offset : offsets)
            list.addAll(offset);

        return list;
    }

    public class StyleOffset implements Comparable<StyleOffset> {
        public int start = 0, end = 0;
        public AttributeSet style;

        public StyleOffset(int start, int end, AttributeSet attributeSet) {
            this.start = start;
            this.end = end;
            this.style = attributeSet;
        }

        public int length() {
            return end - start;
        }

        @Override
        public int compareTo(StyleOffset o) {
            if (o != null) {
                if (start == o.start)
                    return length() - o.length();
                else
                    return start - o.start;
            } else {
                return -1;
            }
        }
    }
}

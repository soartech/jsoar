package org.jsoar.debugger.syntax;


import com.google.re2j.Matcher;
import com.google.re2j.Pattern;

import java.util.*;


public class SyntaxPattern {
    private String comment ="";
    private String regex;
    private List<String> components;
    private boolean enabled = true;

    public SyntaxPattern() {
        components = new LinkedList<>();
        regex = "";
        fixSize();
    }

    public SyntaxPattern(String regex, List<String> components) {
        this.regex = regex;
        this.components = components;
        fixSize();
    }

    public SyntaxPattern(String regex, String[] strings) {
        components = Arrays.asList(strings);
        this.regex = regex;
        fixSize();
    }

    public void fixSize() {
        Pattern p = Pattern.compile(this.regex);
        int size = components.size();
        int i1 = p.groupCount();
        for (int i = 0; i < (i1 - size); i++){
            this.components.add("");
        }
    }


    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public List<String> getComponents() {
        return components;
    }

    public void setComponents(List<String> components) {
        this.components = components;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}



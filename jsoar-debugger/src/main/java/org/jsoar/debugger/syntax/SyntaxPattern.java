package org.jsoar.debugger.syntax;


import com.google.re2j.Matcher;
import com.google.re2j.Pattern;

import javax.swing.text.SimpleAttributeSet;
import java.util.*;


public class SyntaxPattern {
    private String name ="";
    private String regex;
    private List<String> components;

    public SyntaxPattern() {
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}



package org.jsoar.debugger.syntax;

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import org.jsoar.debugger.JSoarDebugger;
import org.jsoar.kernel.SoarException;
import org.jsoar.tcl.SoarTclInterface;
import org.jsoar.util.commands.DefaultInterpreter;
import org.jsoar.util.commands.SoarCommandInterpreter;

import javax.swing.text.SimpleAttributeSet;
import java.util.*;

public class SyntaxSettings {
    public HashMap<String, TextStyle> componentStyles = new HashMap<>();
    public LinkedList<SyntaxPattern> syntaxPatterns = new LinkedList<>();

    public SyntaxSettings() {

    }

    public synchronized List<StyleOffset> matchAll(String input, SyntaxPattern syntax, HashMap<String, TextStyle> styles, JSoarDebugger debugger) {
        List<StyleOffset> matches = new LinkedList<>();
        String regex = syntax.getRegex();

        //search for special commands %commands% and %alias%
        regex = expandMacros(debugger, regex);
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
                        if (style == null || !style.isEnabled()) {
                            matches.add(new StyleOffset(start, end, new SimpleAttributeSet()));
                        } else {
                            matches.add(new StyleOffset(start, end, style.getAttributes()));
                        }
                    }
                } else {
                    for (int i = 1; i < groupCount + 1; i++) {//start with match 1, because match 0 is the whole regex not the capture group
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
            System.err.println(e.getMessage());
            System.err.println("attempted regex: " + regex);
        }

        return matches;
    }

    public static String expandMacros(JSoarDebugger debugger, String regex) {
        if (regex.contains("%aliases%")) {
            StringBuilder aliasesStr = new StringBuilder();
            SoarCommandInterpreter interpreter = debugger.getAgent().getInterpreter();
            if (interpreter instanceof DefaultInterpreter) {
                List<String> aliases = new ArrayList<>(((DefaultInterpreter) interpreter).getAliasStrings());
                Collections.sort(aliases, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return o2.length() - o1.length();
                    }
                });
                for (Iterator<String> iterator = aliases.iterator(); iterator.hasNext(); ) {
                    String alias = iterator.next();
                    if (alias.equals("?")) {
                        alias = "\\?";
                    }
                    aliasesStr.append(alias);
                    if (iterator.hasNext())
                        aliasesStr.append("|");
                }

            } else if (interpreter instanceof SoarTclInterface) {
                try {
                    String aliases = interpreter.eval("alias");
                    String[] lines = aliases.split("\\r?\\n");
                    for (int i = 0; i < lines.length; i++) {
                        String line = lines[i];
                        if (!line.isEmpty()) {
                            String alias = line.split(" -> ")[0];
                            if (alias.equals("?")) {
                                alias = "\\?";
                            }
                            aliasesStr.append(alias);
                            if (i < lines.length - 1) {
                                aliasesStr.append("|");
                            }
                        }
                    }

                } catch (SoarException e) {
                    e.printStackTrace();
                }
            }
            regex = regex.replaceAll("%aliases%", Matcher.quoteReplacement(aliasesStr.toString()));
        }
        if (regex.contains("%commands%")) {
            StringBuilder commandsStr = new StringBuilder();
            SoarCommandInterpreter interpreter = debugger.getAgent().getInterpreter();
            if (interpreter instanceof DefaultInterpreter) {
                List<String> commands = new ArrayList<>(((DefaultInterpreter) interpreter).getCommandStrings());
                Collections.sort(commands, new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return o2.length() - o1.length();
                    }
                });
                for (Iterator<String> iterator = commands.iterator(); iterator.hasNext(); ) {
                    String command = iterator.next();
                    commandsStr.append(command);
                    if (iterator.hasNext())
                        commandsStr.append("|");
                }

            } else if (interpreter instanceof SoarTclInterface) {
                try {
                    commandsStr.append(interpreter.eval("info commands").replace(" ", "|").replace("?","\\?"));
                    commandsStr.append("|");
                    commandsStr.append(interpreter.eval("info procs").replace(" ", "|").replace("?","\\?"));
                } catch (SoarException e) {
                    e.printStackTrace();
                }
            }
            regex = regex.replaceAll("%commands%", Matcher.quoteReplacement(commandsStr.toString()));
        }
        return regex;
    }

    public TreeSet<StyleOffset> getForAll(String str, JSoarDebugger debugger) {
        TreeSet<StyleOffset> offsets = new TreeSet<>();
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

    public void addTextStyle(String phase, TextStyle style) {
        componentStyles.put(phase, style);
    }

    public void addPattern(SyntaxPattern pattern) {
        syntaxPatterns.add(pattern);
    }
}

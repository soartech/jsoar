package org.jsoar.debugger.syntax;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import org.jsoar.debugger.JSoarDebugger;
import org.jsoar.debugger.syntax.ui.SyntaxConfigurator;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.rhs.functions.RhsFunctionHandler;
import org.jsoar.tcl.SoarTclInterface;
import org.jsoar.util.commands.DefaultInterpreter;
import org.jsoar.util.commands.SoarCommandInterpreter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;


public class SyntaxPattern {
    private String comment ="";
    private String regex;
    private List<String> components;
    @SuppressWarnings("unused")
    private static final Logger logger = LoggerFactory.getLogger(SyntaxConfigurator.class);
    private boolean enabled = true;
    private String expandedRegex;
    private boolean important;

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

    @JsonIgnore
    public String getExpandedRegex(){
        if (expandedRegex == null){
            return regex;
        }
        return expandedRegex;
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


    /**
     * Expand %commands% and %aliases% into regex code
     * @param debugger A debugger instance, needed to get the interpreter instance
     */
    public void expandMacros(JSoarDebugger debugger)
    {
//        logger.debug("expanding "+regex);
        expandedRegex = regex;
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
            expandedRegex = expandedRegex.replaceAll("%aliases%", Matcher.quoteReplacement(aliasesStr.toString()));
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
            expandedRegex = expandedRegex.replaceAll("%commands%", Matcher.quoteReplacement(commandsStr.toString()));
        }
        if(regex.contains("%rhsfuncs%")) {
            StringBuilder rhsFuncsString = new StringBuilder();
            List<String> rhsFuncs = debugger.getAgent().getRhsFunctions().getHandlers().stream().map(RhsFunctionHandler::getName).collect(Collectors.toList());
            
            // sort by length, so longer sequences are matched on in the regex first
            Collections.sort(rhsFuncs, (s1, s2) -> s2.length() - s1.length());
            
            for(Iterator<String> iterator = rhsFuncs.iterator(); iterator.hasNext();) {
                String rhsFunc = escapeRegexSpecialChars(iterator.next());
                rhsFuncsString.append(rhsFunc);
                if(iterator.hasNext())
                    rhsFuncsString.append("|");
            }
            expandedRegex = expandedRegex.replaceAll("%rhsfuncs%", Matcher.quoteReplacement(rhsFuncsString.toString()));
        }
    }
    
    private final String regexSpecialChars = "[\\^$.|?*+(){}";
    private String escapeRegexSpecialChars(String s) {
        String result = s;
        for(char c : regexSpecialChars.toCharArray()) {
            result = result.replace(String.valueOf(c), "\\" + c);
        }
        return result;
    }

    public boolean isImportant()
    {
        return important;
    }

    public void setImportant(boolean important)
    {
        this.important = important;
    }
}



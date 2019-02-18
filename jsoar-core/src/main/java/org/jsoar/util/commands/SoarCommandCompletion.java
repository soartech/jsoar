package org.jsoar.util.commands;

import picocli.CommandLine;

import java.util.ArrayList;

public class SoarCommandCompletion
{
    /**
     * Generate a completion list for a CommandLine object, based on cursor position and input
     * @param commandLine The CommandLine to perform conpletion on
     * @param input The typed input to generate a completion list for
     * @param cursorPosition The user's cursor position in the string
     * @return
     */
    public static String[] complete(CommandLine commandLine, String input, int cursorPosition)
    {
        if (commandLine != null) {

            String[] parts = input.split(" ");
            int argIndex = 0;
            int positionInArg = 0;
            //figure out argIndex
            for (int i = 0; i < input.length(); i++){
                char c = input.charAt(i);
                if (i != 0) {
                    char prev = input.charAt(i-1);
                    if (c == ' ' && prev != ' ') {
                        argIndex++;
                        positionInArg = 0;
                    }
                } 
                if (c != ' ') {
                    positionInArg++;
                }
            }

            ArrayList<CharSequence> longResults = new ArrayList<>();
            //System.out.println("argIndex: "+argIndex+", position: "+positionInArg+", command: "+input);
            picocli.AutoComplete.complete(commandLine.getCommandSpec(), parts, argIndex, positionInArg, input.length(), longResults);

            for (int i = 0; i < longResults.size(); i++) {
                longResults.set(i, input + longResults.get(i));
            }

            if (longResults.isEmpty()){
                longResults.add(input);
            }

            return longResults.toArray(new String[0]);
        }

        return null;
    }
}

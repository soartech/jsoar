package org.jsoar.util.commands;

import java.util.ArrayList;

import picocli.CommandLine;

public class SoarCommandCompletion
{
    /**
     * Generate a completion list for a CommandLine object, based on cursor position and input
     * NOTE: This must happen on the Soar thread to avoid issues with command execution, which definitely happens on the Soar thread, trying to use the CommandSpec object in parallel.
     * @param commandLine The CommandLine to perform completion on
     * @param input The typed input to generate a completion list for
     * @param cursorPosition The user's cursor position in the string
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

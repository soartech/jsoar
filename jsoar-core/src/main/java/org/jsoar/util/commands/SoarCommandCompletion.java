package org.jsoar.util.commands;

import java.util.ArrayList;

import picocli.CommandLine;

public class SoarCommandCompletion
{
    /**
     * Generate a completion list for a CommandLine object, based on cursor position and input
     * NOTE: This must happen on the Soar thread to avoid issues with command execution, which definitely happens on the Soar thread, trying to use the CommandSpec object in parallel.
     * 
     * @param commandLine The CommandLine to perform completion on
     * @param input The typed input to generate a completion list for
     */
    public static String[] complete(CommandLine commandLine, String input)
    {
        if(commandLine != null)
        {
            // picocli expects subcommands to be given as part of a full command
            // so if this command is actually an alias for a subcommand of another command, we have to construct the full command
            CommandLine parent = commandLine;
            String parentCommands = "";
            
            while(parent.getParent() != null)
            {
                parent = commandLine.getParent();
                parentCommands = parent.getCommandName() + " ";
            }
            
            String fullCommand = parentCommands + input.trim();
            
            // now that we have the full command, we can identify the things picocli needs to generate auto complete candidates:
            // the parts: an array of the command + subcommands)
            // argIndex: which subcommand we're completing
            // positionInArg: how much of the last arg has been typed
            
            String[] parts = fullCommand.split(" ");
            
            int argIndex = parts.length - 1;
            int positionInArg = parts[argIndex].length();
            
            ArrayList<CharSequence> longResults = new ArrayList<>();
            
            picocli.AutoComplete.complete(parent.getCommandSpec(), parts, argIndex, positionInArg, fullCommand.length(), longResults);
            
            for(int i = 0; i < longResults.size(); i++)
            {
                longResults.set(i, input + longResults.get(i));
            }
            
            if(longResults.isEmpty())
            {
                longResults.add(input);
            }
            
            return longResults.toArray(new String[0]);
        }
        
        return null;
    }
}

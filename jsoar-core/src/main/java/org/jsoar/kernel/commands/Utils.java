package org.jsoar.kernel.commands;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.output.WriterOutputStream;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.commands.DebugCommand.Debug;

import picocli.CommandLine;
import picocli.CommandLine.RunLast;

public class Utils
{
    /**
     * Helper method intended to connect SoarCommands to the picocli parser
     * Typically called from the SoarCommand's execute method
     * Any output generated by picocli will automatically be passed to the agent's writer
     * Note that while SoarCommands is the typical use case, there was no reason to restrict this to that, so it accepts any object
     * @param agent SoarAgent executing the command
     * @param command An Object, should be annotated with picocli's @Command annotation
     * @param args The args as received by a SoarCommand's execute method (i.e., the 0th arg should be the string for the command itself)
     */
    public static void parseAndRun(Agent agent, Object command, String[] args) {
        OutputStream os = new WriterOutputStream(agent.getPrinter().getWriter(), Charset.defaultCharset(), 1024, true);
        PrintStream ps = new PrintStream(os);
        
        parseAndRun(command, args, ps);
    }
    
    public static List<Object> parseAndRun(Object command, String[] args, PrintStream ps) {
        
        CommandLine commandLine = new CommandLine(command);
        
        // The "debug time" command takes a command as a parameter, which can contain options
        // In order to inform picocli that the options are part of the command parameter
        // the following boolean must be set to true
        if (command.getClass() == Debug.class)
        {
            commandLine.setUnmatchedOptionsArePositionalParams(true);
        }
        
        return commandLine.parseWithHandlers(
                new RunLast().useOut(ps),
                CommandLine.defaultExceptionHandler().useErr(ps),
                Arrays.copyOfRange(args, 1, args.length)); // picocli expects the first arg to be the first arg of the command, but for SoarCommands its the name of the command, so get the subarray starting at the second arg
    }
    
    public static String parseAndRun(Object command, String[] args) throws SoarException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PrintStream ps = new PrintStream(baos, true, "UTF-8")) {
            List<Object> results = parseAndRun(command, args, ps);
            for(Object o : results) {
                ps.print(o.toString());
            }
        }
        catch (UnsupportedEncodingException e)
        {
            throw new SoarException(e);
        }
        final String result = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        return result;
    }
}

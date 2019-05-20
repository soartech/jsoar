package org.jsoar.kernel.exceptions;


import org.jsoar.util.commands.SoarCommandContext;

/*
    Class used by soar language server for exceptions that are thrown
    during tcl execution but are caught and not propagated up
 */
public class SoftTclInterpreterException {

    private String message;
    private SoarCommandContext commandContext;
    private String command;

    public SoftTclInterpreterException(String message, SoarCommandContext commandContext, String command) {
        this.message = message;
        this.commandContext = commandContext;
        this.command = command;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SoarCommandContext getCommandContext() {
        return commandContext;
    }

    public void setCommandContext(SoarCommandContext commandContext) {
        this.commandContext = commandContext;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }
}

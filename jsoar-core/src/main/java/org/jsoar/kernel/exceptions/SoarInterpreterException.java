package org.jsoar.kernel.exceptions;


import org.jsoar.kernel.SoarException;
import org.jsoar.util.ByRef;
import org.jsoar.util.SourceLocation;
import org.jsoar.util.commands.ParsedCommand;

public class SoarInterpreterException extends SoarException {

    private static final long serialVersionUID = 7681071169683948896L;
    private ByRef<ParsedCommand> parsedCommand;

    /**
     *
     */
    public SoarInterpreterException() { }

    public SoarInterpreterException(String message) {
        super(message);
    }

    public SoarInterpreterException(String message, ByRef<ParsedCommand> command) {
        super(message);
        this.parsedCommand = command;
    }

    /**
     * @param cause
     */
    public SoarInterpreterException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public SoarInterpreterException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParsedCommand getParsedCommand() {
        return parsedCommand.value;
    }

    public SourceLocation getSourceLocation() {
        return parsedCommand.value.getLocation();
    }

}

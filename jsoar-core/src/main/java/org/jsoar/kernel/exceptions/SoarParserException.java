package org.jsoar.kernel.exceptions;

public class SoarParserException extends SoarInterpreterException {

    // file offset for start of exception
    private int offset;

    public SoarParserException(String message, int offset) {
        super(message);

        this.offset = offset - 1;
    }

    public int getOffset() {
        return offset;
    }
}

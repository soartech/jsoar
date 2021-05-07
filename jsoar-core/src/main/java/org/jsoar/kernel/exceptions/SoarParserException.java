package org.jsoar.kernel.exceptions;

public class SoarParserException extends SoarInterpreterException {

  private static final long serialVersionUID = -3172987832934479286L;
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

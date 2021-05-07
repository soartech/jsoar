/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 23, 2010
 */
package org.jsoar.util.commands;

import java.io.IOException;
import java.io.PushbackReader;
import org.jsoar.util.DefaultSourceLocation;
import org.jsoar.util.SourceLocation;

/**
 * A buffer for a parser to read from. Keeps track of the current file, character offset and line
 * number. Otherwise, just a wrapper for {@code PushbackReader}.
 *
 * @author ray
 */
public class ParserBuffer {
  private final PushbackReader reader;
  private String file;
  private int currentOffset;
  private int line;

  public ParserBuffer(PushbackReader reader) {
    this.reader = reader;
    this.file = null;
    currentOffset = 0;
    line = 0;
  }

  /** @return the file or null if unknown */
  public String getFile() {
    return file;
  }

  /** @param file the file */
  public void setFile(String file) {
    this.file = file;
  }

  /** @return the current character offset */
  public int getCurrentOffset() {
    return currentOffset;
  }

  /** @param currentOffset the current character offset */
  public void setCurrentOffset(int currentOffset) {
    this.currentOffset = currentOffset;
  }

  /** @return the current line, starting at 0. */
  public int getCurrentLine() {
    return line;
  }

  /** @param line the current line, starting at 0. */
  public void setCurrentLine(int line) {
    this.line = line;
  }

  /**
   * Read a character.
   *
   * @return the character, or {@code -1} on eof.
   * @throws IOException
   */
  public int read() throws IOException {
    final int c = reader.read();
    if (c != -1) {
      currentOffset++;
    }

    if (c == '\n') {
      line++;
    }

    return c;
  }

  /**
   * Unread a character
   *
   * @param c the character
   * @throws IOException
   */
  public void unread(int c) throws IOException {
    if (c == '\n') {
      line--;
    }
    currentOffset--;
    reader.unread(c);
  }

  /**
   * Close the underlying reader.
   *
   * @throws IOException
   */
  public void close() throws IOException {
    reader.close();
  }

  /** @return a source location for the current location of the buffer */
  public SourceLocation getLocation() {
    return DefaultSourceLocation.newBuilder().file(file).offset(currentOffset).line(line).build();
  }
}

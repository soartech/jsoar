/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 24, 2009
 */
package org.jsoar.legilimens.trace;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.jsoar.util.FileTools;

/** @author ray */
class FileBuffer extends Writer {
  private final RingBuffer ringBuffer;
  private final File file;
  private final BufferedWriter writer;
  private int charsWritten;

  public FileBuffer(String name, RingBuffer ringBuffer) throws IOException {
    this.ringBuffer = ringBuffer;

    final String cleanName = FileTools.replaceIllegalCharacters(name, "_");

    final File cd = new File(System.getProperty("user.dir"));
    final Date now = new Date();
    final SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");

    this.file = new File(cd, "jsoar.legilimens." + cleanName + "." + format.format(now) + ".trace");
    this.writer = new BufferedWriter(new FileWriter(this.file));
  }

  public File getFile() {
    return file;
  }

  public int getLength() {
    synchronized (ringBuffer) {
      return charsWritten;
    }
  }

  public TraceRange getRange(int start, int max) throws IOException {
    final BufferedReader reader = new BufferedReader(new FileReader(file));
    try {
      reader.skip(start);

      char[] buffer = new char[max];
      int total = 0;
      while (total < max) {
        int read = reader.read(buffer, total, max - total);
        if (read < -1) {
          break;
        }
        total += read;
      }
      return new TraceRange(start, buffer, total);
    } finally {
      reader.close();
    }
  }

  /* (non-Javadoc)
   * @see java.io.Writer#close()
   */
  @Override
  public void close() throws IOException {
    this.writer.close();
  }

  /* (non-Javadoc)
   * @see java.io.Writer#flush()
   */
  @Override
  public void flush() throws IOException {
    this.writer.flush();
  }

  /* (non-Javadoc)
   * @see java.io.Writer#write(char[], int, int)
   */
  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    synchronized (ringBuffer) {
      charsWritten += len;
      this.ringBuffer.write(cbuf, off, len);
    }
    this.writer.write(cbuf, off, len);
  }
}

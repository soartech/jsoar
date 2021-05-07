/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2009
 */
package org.jsoar.legilimens.trace;

import java.util.Arrays;

/** @author ray */
class RingBuffer {
  private final char[] buffer;
  private int head; // the next index to write to

  public RingBuffer(int size) {
    buffer = new char[size];
    head = 0;
  }

  public int size() {
    return buffer.length;
  }

  public synchronized void write(char[] chars, int start, int length) {
    int end = start + length;
    int available = buffer.length - head;
    while (start < end) {
      int toWrite = Math.min(end - start, available);

      System.arraycopy(chars, start, buffer, head, toWrite);

      head = (head + toWrite) % buffer.length;
      start += toWrite;
      available = buffer.length - head;
    }
  }

  public synchronized char[] getTail(int count) {
    return getTail(count, -1);
  }

  public synchronized char[] getTail(int charsBack, int max) {
    if (charsBack < 0) {
      throw new IllegalArgumentException("count must be positive");
    }
    if (charsBack > buffer.length) {
      throw new IllegalArgumentException("count must be < " + buffer.length);
    }
    if (max < 0) {
      max = charsBack;
    }

    int startPoint = head - charsBack;
    if (startPoint < 0) {
      startPoint += buffer.length;
    }
    final int endPoint = (startPoint + max) % buffer.length;

    if (startPoint <= endPoint && max != buffer.length) {
      return Arrays.copyOfRange(buffer, startPoint, endPoint);
    } else {
      final char[] result = new char[max];
      final int firstLength = buffer.length - startPoint;
      System.arraycopy(buffer, startPoint, result, 0, firstLength);
      System.arraycopy(buffer, 0, result, firstLength, endPoint);
      return result;
    }
  }

  synchronized int getHead() {
    return head;
  }

  char[] getRawBuffer() {
    return buffer;
  }
}

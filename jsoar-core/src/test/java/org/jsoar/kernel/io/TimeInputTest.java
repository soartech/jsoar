package org.jsoar.kernel.io;

import org.junit.Test;

public class TimeInputTest {

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorThrowsExceptionIfIoIsNull() {
    new TimeInput(null);
  }
}

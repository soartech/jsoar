package org.jsoar.runtime;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jsoar.kernel.Production;
import org.junit.Test;

public class WaitInfoTest {

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorThrowsExceptionIfTimeoutZeroOrLess() {
    new WaitInfo(0, mock(Production.class));
  }

  @Test
  public void testToStringIfWaitingForever() {
    // Given wait info with timeout forever
    Production cause = mock(Production.class);
    when(cause.toString()).thenReturn("CAUSE");
    WaitInfo waitInfo = new WaitInfo(Long.MAX_VALUE, cause);

    // When printing wait info
    String output = waitInfo.toString();

    // Then output matches 'Waiting forever [CAUSE]'
    assertEquals("Waiting forever [CAUSE]", output);
  }

  @Test
  public void testToStringIfWaiting() {
    // Given wait info with timeout 6
    Production cause = mock(Production.class);
    when(cause.toString()).thenReturn("CAUSE");
    WaitInfo waitInfo = new WaitInfo(6, cause);

    // When printing wait info
    String output = waitInfo.toString();

    // Then output matches 'Waiting 6 ms [CAUSE]'
    assertEquals("Waiting 6 ms [CAUSE]", output);
  }

  @Test
  public void testToStringIfNotWaiting() {
    // Given not waiting info
    WaitInfo waitInfo = WaitInfo.NOT_WAITING;

    // When printing wait info
    String output = waitInfo.toString();

    // Then output matches 'No wait'
    assertEquals("No wait", output);
  }
}

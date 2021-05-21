package org.jsoar.runtime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

public class SwingCompletionHandlerTest {

  @Test(expected = IllegalArgumentException.class)
  public void testNewInstanceThrowsExceptionWhenInnerCompletionHandlerIsNull() {
    SwingCompletionHandler.newInstance(null);
  }

  @Test
  public void testFinish() throws InterruptedException {
    // Given a Swing completion handler
    // And a nested inner completion handler
    CompletionHandler<String> innerCompletionHandler = mock(CompletionHandler.class);
    CompletionHandler<String> swingCompletionHandler =
        SwingCompletionHandler.newInstance(innerCompletionHandler);

    // When finishing with result
    String result = "RESULT";
    swingCompletionHandler.finish(result);

    // Then finish on inner completion handler is called
    // And result is passed
    // NOTE: Since event is put on queue and not immediately executed wait for a while, might be buggy
    Thread.sleep(500);
    verify(innerCompletionHandler, times(1)).finish(result);
  }
}

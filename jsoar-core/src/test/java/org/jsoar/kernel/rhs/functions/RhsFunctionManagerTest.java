package org.jsoar.kernel.rhs.functions;

import static org.mockito.Mockito.mock;

import org.junit.Test;

public class RhsFunctionManagerTest {

  @Test(expected = IllegalArgumentException.class)
  public void testRegisterHandlerThrowsExceptionIfHandlerIsNull() {
    RhsFunctionManager manager = new RhsFunctionManager(mock(RhsFunctionContext.class));
    manager.registerHandler(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUnregisterHandlerThrowsExceptionIfNameIsNull() {
    RhsFunctionManager manager = new RhsFunctionManager(mock(RhsFunctionContext.class));
    manager.unregisterHandler(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDisableHandlerThrowsExceptionIfNameIsNull() {
    RhsFunctionManager manager = new RhsFunctionManager(mock(RhsFunctionContext.class));
    manager.disableHandler(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEnableHandlerThrowsExceptionIfNameIsNull() {
    RhsFunctionManager manager = new RhsFunctionManager(mock(RhsFunctionContext.class));
    manager.enableHandler(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testIsDisabledThrowsExceptionIfNameIsNull() {
    RhsFunctionManager manager = new RhsFunctionManager(mock(RhsFunctionContext.class));
    manager.isDisabled(null);
  }
}

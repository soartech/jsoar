package org.jsoar.kernel.rhs.functions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.jsoar.kernel.symbols.Symbol;
import org.junit.Test;

public class RhsFunctionManagerTest {

  @Test
  public void testRegisterHandler() {
    // Given a RHS function handler
    RhsFunctionHandler handler = mock(RhsFunctionHandler.class);
    when(handler.getName()).thenReturn("TEST-REGISTER-HANDLER");
    // And RHS function manager
    RhsFunctionManager manager = new RhsFunctionManager(mock(RhsFunctionContext.class));

    // When registering handler
    manager.registerHandler(handler);

    // Then handler is added to manager
    assertTrue(manager.getHandlers().contains(handler));
    assertNotNull(manager.getHandler(handler.getName()));
    // And handler is enabled
    assertFalse(manager.isDisabled(handler.getName()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testRegisterHandlerThrowsExceptionIfHandlerIsNull() {
    RhsFunctionManager manager = new RhsFunctionManager(mock(RhsFunctionContext.class));
    manager.registerHandler(null);
  }

  @Test
  public void testUnregisterEnabledHandler() {
    // Given a RHS function manager
    RhsFunctionManager manager = new RhsFunctionManager(mock(RhsFunctionContext.class));
    // And a registered RHS function handler which is enabled
    RhsFunctionHandler handler = mock(RhsFunctionHandler.class);
    when(handler.getName()).thenReturn("TEST-REGISTER-HANDLER");
    manager.registerHandler(handler);

    // When unregistering handler
    manager.unregisterHandler(handler.getName());

    // Then handler is removed from enabled handlers of managers
    assertFalse(manager.getHandlers().contains(handler));
    assertNull(manager.getHandler(handler.getName()));
  }

  @Test
  public void testUnregisterDisabledHandler() {
    // Given a RHS function manager
    RhsFunctionManager manager = new RhsFunctionManager(mock(RhsFunctionContext.class));
    // And a registered RHS function handler which is disabled
    RhsFunctionHandler handler = mock(RhsFunctionHandler.class);
    when(handler.getName()).thenReturn("TEST-REGISTER-HANDLER");
    manager.registerHandler(handler);
    manager.disableHandler(handler.getName());

    // When unregistering handler
    manager.unregisterHandler(handler.getName());

    // Then handler still exist as disabled handler in manager
    assertTrue(manager.getDisabledHandlers().contains(handler));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testUnregisterHandlerThrowsExceptionIfNameIsNull() {
    RhsFunctionManager manager = new RhsFunctionManager(mock(RhsFunctionContext.class));
    manager.unregisterHandler(null);
  }

  @Test
  public void testDisableHandler() {
    // Given a RHS function manager
    RhsFunctionManager manager = new RhsFunctionManager(mock(RhsFunctionContext.class));
    // And a registered RHS function handler which is enabled
    RhsFunctionHandler handler = mock(RhsFunctionHandler.class);
    when(handler.getName()).thenReturn("TEST-REGISTER-HANDLER");
    manager.registerHandler(handler);

    // When disabling handler
    manager.disableHandler(handler.getName());

    // Then handler exists as disabled handler in manager
    assertTrue(manager.getDisabledHandlers().contains(handler));
    assertTrue(manager.isDisabled(handler.getName()));
    // And removed from enabled handlers in manager
    assertFalse(manager.getHandlers().contains(handler));
  }

  @Test
  public void testDisableNonExistingHandler() {
    // Given a RHS function manager
    RhsFunctionManager manager = new RhsFunctionManager(mock(RhsFunctionContext.class));
    // And a registered RHS function handler which is enabled
    RhsFunctionHandler handler = mock(RhsFunctionHandler.class);
    when(handler.getName()).thenReturn("TEST-REGISTER-HANDLER");
    manager.registerHandler(handler);

    // When disabling non existing handler
    manager.disableHandler("NON-EXISTING");

    // Then manager is unchanged
    assertTrue(manager.getHandlers().contains(handler));
    assertTrue(manager.getDisabledHandlers().isEmpty());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testDisableHandlerThrowsExceptionIfNameIsNull() {
    RhsFunctionManager manager = new RhsFunctionManager(mock(RhsFunctionContext.class));
    manager.disableHandler(null);
  }

  @Test
  public void testEnableHandler() {
    // Given a RHS function manager
    RhsFunctionManager manager = new RhsFunctionManager(mock(RhsFunctionContext.class));
    // And a registered RHS function handler which is disabled
    RhsFunctionHandler handler = mock(RhsFunctionHandler.class);
    when(handler.getName()).thenReturn("TEST-REGISTER-HANDLER");
    manager.registerHandler(handler);
    manager.disableHandler(handler.getName());

    // When enabling handler
    manager.enableHandler(handler.getName());

    // Then handler exists as enabled handler in manager
    assertTrue(manager.getHandlers().contains(handler));
    assertNotNull(manager.getHandler(handler.getName()));
    assertFalse(manager.isDisabled(handler.getName()));
    // And removed from disabled handlers in manager
    assertFalse(manager.getDisabledHandlers().contains(handler));
  }

  @Test
  public void testEnableNonExistingHandler() {
    // Given a RHS function manager
    RhsFunctionManager manager = new RhsFunctionManager(mock(RhsFunctionContext.class));
    // And a registered RHS function handler which is disabled
    RhsFunctionHandler handler = mock(RhsFunctionHandler.class);
    when(handler.getName()).thenReturn("TEST-REGISTER-HANDLER");
    manager.registerHandler(handler);
    manager.disableHandler(handler.getName());

    // When enabling handler
    manager.enableHandler("NON-EXISTING");

    // Then nothing has changed within manager
    assertTrue(manager.getHandlers().isEmpty());
    assertTrue(manager.getDisabledHandlers().contains(handler));
    assertTrue(manager.isDisabled(handler.getName()));
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

  @Test
  public void testExecuteRhsFunction() throws RhsFunctionException {
    Symbol expectedResultExecution = mock(Symbol.class);

    // Given a RHS function manager
    RhsFunctionContext context = mock(RhsFunctionContext.class);
    RhsFunctionManager manager = new RhsFunctionManager(context);
    // And a registered RHS function handler which is enabled
    RhsFunctionHandler handler = mock(RhsFunctionHandler.class);
    when(handler.execute(any(),any())).thenReturn(expectedResultExecution);
    when(handler.getName()).thenReturn("TEST-REGISTER-HANDLER");
    manager.registerHandler(handler);

    // When executing RHS function
    List<Symbol> arguments = List.of(mock(Symbol.class));
    Symbol resultExecution = manager.execute(handler.getName(), arguments);

    // Then RHS function is invoked
    verify(handler).execute(context,arguments);
    // And result matches expected result
    assertEquals(expectedResultExecution, resultExecution);
  }

  @Test
  public void testExecuteDisabledRhsFunction() throws RhsFunctionException {
    // Given a RHS function manager
    RhsFunctionContext context = mock(RhsFunctionContext.class);
    RhsFunctionManager manager = new RhsFunctionManager(context);
    // And a registered RHS function handler which is disabled
    RhsFunctionHandler handler = mock(RhsFunctionHandler.class);
    when(handler.getName()).thenReturn("TEST-REGISTER-HANDLER");
    manager.registerHandler(handler);
    manager.disableHandler(handler.getName());

    // When executing RHS function
    List<Symbol> arguments = List.of(mock(Symbol.class));
    Symbol resultExecution = manager.execute(handler.getName(), arguments);

    // Then result of execution is null
    assertNull(resultExecution);
    // And RHS function is NOT invoked
    verify(handler, never()).execute(context, arguments);
  }

  @Test(expected = RhsFunctionException.class)
  public void testExecuteThrowsExceptionIfNonExistingRhsFunction() throws RhsFunctionException {
    // Given a RHS function manager
    RhsFunctionContext context = mock(RhsFunctionContext.class);
    RhsFunctionManager manager = new RhsFunctionManager(context);

    // When executing non existing RHS function
    // Then exception is thrown
    manager.execute("NON-EXISTING", List.of(mock(Symbol.class)));
  }
}

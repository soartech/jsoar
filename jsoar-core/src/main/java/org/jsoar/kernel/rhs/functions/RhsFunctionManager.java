/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 18, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.NonNull;
import org.jsoar.kernel.symbols.Symbol;

/**
 * Manages registered RHS functions for an agent
 *
 * @author ray, adam.sypniewski
 * @see RhsFunctionHandler
 */
public class RhsFunctionManager {

  private final RhsFunctionContext rhsContext;
  private final Map<String, RhsFunctionHandler> handlers =
      new ConcurrentHashMap<>();
  private final Map<String, RhsFunctionHandler> disabledHandlers =
      new ConcurrentHashMap<>();

  /**
   * Construct a new RHS function manager with the given execution context
   *
   * @param rhsContext The context in which functions will be executed
   */
  public RhsFunctionManager(RhsFunctionContext rhsContext) {
    this.rhsContext = rhsContext;
  }

  /**
   * Returns a list of all registered RHS function handlers. The list is a copy and may be modified
   * by the caller.
   *
   * <p>This method may be called from any thread
   *
   * @return Copy of list of all registered RHS function handlers
   */
  public List<RhsFunctionHandler> getHandlers() {
    return new ArrayList<>(handlers.values());
  }

  /**
   * Register a RHS function
   *
   * <p>This method may be called from any thread
   *
   * @param handler The handler to call for the function
   * @return The previously registered handler
   */
  public RhsFunctionHandler registerHandler(@NonNull RhsFunctionHandler handler) {
    return handlers.put(handler.getName(), handler);
  }

  /**
   * Lookup a registered handler by name
   *
   * <p>This method may be called from any thread
   *
   * @param name The name of the handler
   * @return The handler or null if none is registered
   */
  public RhsFunctionHandler getHandler(String name) {
    return handlers.get(name);
  }

  /**
   * Remove a RHS function previously registered with {@link #registerHandler(RhsFunctionHandler)}
   *
   * <p>This method may be called from any thread
   *
   * @param name Name of handler to remove
   */
  public void unregisterHandler(@NonNull String name) {
    handlers.remove(name);
  }

  /**
   * Disables a RHS function handler (so that calling it is a NOP).
   *
   * @param name Name of handler to disable.
   */
  public void disableHandler(@NonNull String name) {
    if (handlers.containsKey(name)) {
      disabledHandlers.put(name, handlers.remove(name));
    }
  }

  /**
   * Enables a previously disabled RHS function handler (so that calling it is no longer a NOP).
   *
   * @param name Name of handler to enable.
   */
  public void enableHandler(@NonNull String name) {
    if (disabledHandlers.containsKey(name) && !handlers.containsKey(name)) {
      handlers.put(name, disabledHandlers.remove(name));
    }
  }

  /**
   * Checks to see if a function handler is disabled.
   *
   * @param name Name of handler to check/
   * @return true if the handler is disabled; otherwise--if it is enabled or not registered--returns
   * false.
   */
  public boolean isDisabled(@NonNull String name) {
    return disabledHandlers.containsKey(name);
  }

  public List<RhsFunctionHandler> getDisabledHandlers() {
    return new ArrayList<>(disabledHandlers.values());
  }

  /**
   * Execute the named RHS function with the given arguments.
   *
   * @param name      The name of the RHS function to execute
   * @param arguments The arguments
   * @return The result
   * @throws RhsFunctionException if an error occurs or there is no such RHS function.
   */
  public Symbol execute(String name, List<Symbol> arguments) throws RhsFunctionException {
    RhsFunctionHandler handler = handlers.get(name);

    if (handler != null) {
      return handler.execute(rhsContext, arguments);
    } else if (disabledHandlers.containsKey(name)) {
      return null;
    }

    throw new RhsFunctionException("No function '" + name + "' registered");
  }
}

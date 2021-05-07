package org.jsoar.kernel.io.commands;

import org.jsoar.kernel.symbols.Identifier;

/**
 * A handler for WMEs added to the output link in a standard pattern:
 *
 * <p>
 *
 * <pre>
 *      (I3 ^command-name C1) </pre>
 *
 * Where the attribute of the WME is the name of the command and C1 is the command's identifier.
 */
public interface OutputCommandHandler {
  /**
   * Fired when a command is added to the output link.
   *
   * <p>For example, if the following WME is added to the output link I3, then the commandName
   * parameter is "command-attr" and the commandId parameter is "C1":
   *
   * <pre>
   *    (I3 ^command-attr C1)</pre>
   *
   * @param commandName Name of the command,
   * @param commandId Identifier of the command.
   */
  void onCommandAdded(String commandName, Identifier commandId);

  /**
   * Fired when a command is removed from the output link.
   *
   * @param commandName Name of the command
   * @param commandId Identifier of the command.
   * @see OutputCommandHandler#onCommandAdded(String, Identifier)
   */
  void onCommandRemoved(String commandName, Identifier commandId);
}

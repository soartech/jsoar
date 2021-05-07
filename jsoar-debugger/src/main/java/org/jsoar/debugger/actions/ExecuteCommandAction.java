/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 20, 2009
 */
package org.jsoar.debugger.actions;

import java.awt.event.ActionEvent;
import org.jsoar.debugger.CommandLineRunnable;
import org.jsoar.debugger.JSoarDebugger;

/** @author ray */
public class ExecuteCommandAction extends AbstractDebuggerAction {
  private static final long serialVersionUID = 3118895619620083031L;

  private final JSoarDebugger debugger;
  private final String command;

  public ExecuteCommandAction(JSoarDebugger debugger, String command) {
    super(command);
    this.debugger = debugger;
    this.command = command;
  }

  /* (non-Javadoc)
   * @see org.jsoar.debugger.actions.AbstractDebuggerAction#update()
   */
  @Override
  public void update() {}

  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    debugger.getAgent().execute(new CommandLineRunnable(debugger, command), null);
  }
}

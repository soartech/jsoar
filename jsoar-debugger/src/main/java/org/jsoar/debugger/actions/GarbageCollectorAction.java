/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on June 24, 2009
 */
package org.jsoar.debugger.actions;

import java.awt.event.ActionEvent;

/** @author ray */
public class GarbageCollectorAction extends AbstractDebuggerAction {
  private static final long serialVersionUID = -663358241651603549L;

  public GarbageCollectorAction(ActionManager manager) {
    super(manager, "Run Garbage Collector");
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
    System.gc();
    System.runFinalization();
  }
}

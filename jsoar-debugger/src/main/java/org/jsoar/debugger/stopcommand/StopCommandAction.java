package org.jsoar.debugger.stopcommand;

import java.awt.event.ActionEvent;
import org.jsoar.debugger.actions.AbstractDebuggerAction;
import org.jsoar.debugger.actions.ActionManager;

@SuppressWarnings("serial")
public class StopCommandAction extends AbstractDebuggerAction {

  public StopCommandAction(ActionManager manager) {
    super(manager, "Stop Command");
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
    getApplication().addStopCommandView();
  }
}

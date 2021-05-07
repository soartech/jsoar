/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 25, 2008
 */
package org.jsoar.debugger.actions;

import java.awt.event.ActionEvent;
import java.util.List;
import org.jsoar.debugger.Images;
import org.jsoar.debugger.ProductionEditView;
import org.jsoar.kernel.Production;
import org.jsoar.util.adaptables.Adaptables;

/** @author ray */
public class EditProductionAction extends AbstractDebuggerAction {
  private static final long serialVersionUID = -1460902354871319429L;

  /** @param manager the owning action manager */
  public EditProductionAction(ActionManager manager) {
    super(manager, "Edit", Images.EDIT, Production.class, true);

    setToolTip("Edit selected production");
  }

  /* (non-Javadoc)
   * @see org.jsoar.debugger.actions.AbstractDebuggerAction#update()
   */
  @Override
  public void update() {
    final List<Production> prods =
        Adaptables.adaptCollection(getSelectionManager().getSelection(), Production.class);
    setEnabled(prods.size() == 1);
  }

  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent arg0) {
    final List<Production> prods =
        Adaptables.adaptCollection(getSelectionManager().getSelection(), Production.class);
    if (prods.isEmpty()) {
      return;
    }
    ProductionEditView view = Adaptables.adapt(getApplication(), ProductionEditView.class);
    if (view != null) {
      view.editProduction(prods.get(0).getName());
    }
  }
}

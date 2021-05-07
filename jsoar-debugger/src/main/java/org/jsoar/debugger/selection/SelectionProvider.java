/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger.selection;

import java.util.List;

/** @author ray */
public interface SelectionProvider {
  void activate(SelectionManager manager);

  void deactivate();

  Object getSelectedObject();

  List<Object> getSelection();
}

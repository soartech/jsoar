/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import bibliothek.gui.Dockable;
import bibliothek.gui.dock.common.DefaultSingleCDockable;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.common.intern.DefaultCommonDockable;
import java.util.prefs.Preferences;
import org.jsoar.util.adaptables.Adaptable;
import org.jsoar.util.adaptables.Adaptables;

/** @author ray */
public abstract class AbstractAdaptableView extends DefaultSingleCDockable implements Adaptable {
  /**
   * Given an arbitrary dockable dig around and try to find the JSoar view associated with it.
   *
   * @param dockable the dockable
   * @return a view, or {@code null}
   */
  public static AbstractAdaptableView fromDockable(Dockable dockable) {
    if (dockable instanceof AbstractAdaptableView) {
      return (AbstractAdaptableView) dockable;
    } else if (dockable instanceof DefaultCommonDockable) {
      final CDockable cdockable = ((DefaultCommonDockable) dockable).getDockable();
      if (cdockable instanceof AbstractAdaptableView) {
        return (AbstractAdaptableView) cdockable;
      }
    }
    return null;
  }

  public AbstractAdaptableView(String persistentId, String title) {
    super(persistentId, title);

    setCloseable(true);
  }

  public String getShortcutKey() {
    return null;
  }

  /** Called when the view becomes active */
  public void activate() {}

  /* (non-Javadoc)
   * @see org.jsoar.util.adaptables.AbstractAdaptable#getAdapter(java.lang.Class)
   */
  @Override
  public Object getAdapter(Class<?> klass) {
    return Adaptables.adapt(this, klass, false);
  }

  public Preferences getPreferences() {
    return JSoarDebugger.getPreferences().node("views/" + this.getUniqueId());
  }
}

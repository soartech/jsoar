/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 28, 2008
 */
package org.jsoar.debugger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.events.ProductionAddedEvent;
import org.jsoar.kernel.events.ProductionExcisedEvent;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.jsoar.util.events.SoarEventManager;

/** @author ray */
public class ProductionTableModel extends AbstractTableModel {
  private static final long serialVersionUID = -6372714301859379317L;

  private final ThreadedAgent agent;
  private final Listener listener = new Listener();
  private final List<Production> productions =
      Collections.synchronizedList(new ArrayList<Production>());

  /** @param agent */
  public ProductionTableModel(ThreadedAgent agent) {
    this.agent = agent;
  }

  public void initialize() {
    final SoarEventManager eventManager = this.agent.getEvents();
    eventManager.addListener(ProductionAddedEvent.class, listener);
    eventManager.addListener(ProductionExcisedEvent.class, listener);

    // TODO does this need to block for any reason?
    this.agent.execute(
        new Callable<Void>() {

          @Override
          public Void call() {
            synchronized (productions) {
              for (ProductionType pt : ProductionType.values()) {
                // RPM: in general, justifications can come and go rapidly, so we're not going to
                // try to show them in the debugger
                //      we will also ignore them below where new rules are added/removed
                //      in at least one project, this makes a significant performance/memory
                // difference
                if (pt == ProductionType.JUSTIFICATION) {
                  continue;
                }
                productions.addAll(agent.getProductions().getProductions(pt));
              }
            }
            return null;
          }
        },
        null);
  }

  /**
   * Look up a production by name. This method may be safely called from any thread.
   *
   * @param name the name of the production to find
   * @return the production, or {@code null} if not found
   */
  public Production getProduction(String name) {
    synchronized (productions) {
      for (Production p : productions) {
        if (name.equals(p.getName().toString())) {
          return p;
        }
      }
      return null;
    }
  }

  /** @return The list of productions in the model. */
  public List<Production> getProductions() {
    return productions;
  }

  /* (non-Javadoc)
   * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
   */
  @Override
  public Class<?> getColumnClass(int c) {
    switch (c) {
      case 0:
        return Production.class;
      case 1:
        return Long.class;
      case 2:
        return String.class;
    }
    return super.getColumnClass(c);
  }

  /* (non-Javadoc)
   * @see javax.swing.table.AbstractTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int c) {
    switch (c) {
      case 0:
        return "Name";
      case 1:
        return "FC";
      case 2:
        return "Type";
    }
    return super.getColumnName(c);
  }

  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  @Override
  public int getColumnCount() {
    return 3;
  }

  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getRowCount()
   */
  @Override
  public int getRowCount() {
    return productions.size();
  }

  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  @Override
  public Object getValueAt(int row, int column) {
    synchronized (productions) {
      Production p = productions.get(row);
      switch (column) {
        case 0:
          return p;
        case 1:
          return p.getFiringCount();
        case 2:
          return p.getType().getDisplayString();
      }
    }
    return null;
  }

  private void handleProductionAdded(Production p) {
    if (p.getType() == ProductionType.JUSTIFICATION) {
      return;
    }

    int row = 0;
    synchronized (productions) {
      row = productions.size();
      productions.add(p);
    }
    fireTableRowsInserted(row, row);
  }

  private void handleProductionExcised(Production p) {
    if (p.getType() == ProductionType.JUSTIFICATION) {
      return;
    }

    int row = 0;
    synchronized (productions) {
      row = productions.indexOf(p);
      if (row == -1) {
        return;
      }
      productions.remove(row);
    }
    fireTableRowsDeleted(row, row);
  }

  private class Listener implements SoarEventListener {
    @Override
    public void onEvent(final SoarEvent event) {
      Runnable runnable =
          new Runnable() {
            public void run() {
              if (event instanceof ProductionAddedEvent) {
                handleProductionAdded(((ProductionAddedEvent) event).getProduction());
              } else {
                handleProductionExcised(((ProductionExcisedEvent) event).getProduction());
              }
            }
          };
      if (SwingUtilities.isEventDispatchThread()) {
        runnable.run();
      } else {
        SwingUtilities.invokeLater(runnable);
      }
    }
  }
}

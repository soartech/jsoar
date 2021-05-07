/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 31, 2008
 */
package org.jsoar.debugger;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

/** @author ray */
public class DefaultWmeTableModel extends AbstractTableModel {
  private static final long serialVersionUID = -8187445208277014970L;

  public static enum Columns {
    Id(Identifier.class),
    Attr(Symbol.class),
    Value(Symbol.class),
    Timetag(Integer.class),
    Acceptable(String.class);

    Columns(Class<?> type) {
      this.type = type;
    }

    private final Class<?> type;
  }

  private final List<Wme> wmes;

  public DefaultWmeTableModel() {
    this.wmes = new ArrayList<Wme>();
  }

  /** @return the wmes */
  public List<Wme> getWmes() {
    return wmes;
  }

  public void setWmes(List<Wme> newWmes) {
    this.wmes.clear();
    if (newWmes != null) {
      this.wmes.addAll(newWmes);
    }
    fireTableDataChanged();
  }

  /* (non-Javadoc)
   * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
   */
  @Override
  public Class<?> getColumnClass(int columnIndex) {
    return Columns.values()[columnIndex].type;
  }
  /* (non-Javadoc)
   * @see javax.swing.table.AbstractTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int column) {
    return Columns.values()[column].name();
  }
  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  @Override
  public int getColumnCount() {
    return Columns.values().length;
  }
  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getRowCount()
   */
  @Override
  public int getRowCount() {
    return wmes.size();
  }
  /* (non-Javadoc)
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  @Override
  public Object getValueAt(int r, int c) {
    Wme w = wmes.get(r);
    switch (c) {
      case 0:
        return w.getIdentifier();
      case 1:
        return w.getAttribute();
      case 2:
        return w.getValue();
      case 3:
        return w.getTimetag();
      case 4:
        return w.isAcceptable() ? "+" : "";
    }
    return null;
  }
}

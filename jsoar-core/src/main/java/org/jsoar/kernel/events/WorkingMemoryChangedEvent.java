/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 15, 2008
 */
package org.jsoar.kernel.events;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.util.ListHead;
import org.jsoar.util.ListItem;
import org.jsoar.util.events.SoarEvent;

/**
 * Event fired after working memory changes
 *
 * <p>callback.h:76:WM_CHANGES_CALLBACK
 *
 * @author ray
 */
public class WorkingMemoryChangedEvent implements SoarEvent {
  private final ListHead<WmeImpl> added;
  private final ListHead<WmeImpl> removed;

  /**
   * Construct a new event
   *
   * @param added the head of the list of added WMEs
   * @param removed the head of the list of removed WMEs
   */
  public WorkingMemoryChangedEvent(ListHead<WmeImpl> added, ListHead<WmeImpl> removed) {
    this.added = ListHead.newInstance(added);
    this.removed = ListHead.newInstance(removed);
  }

  /**
   * Returns an iterator over the WMEs that were added. {@link Iterator#remove()} is <b>not</b>
   * supported by the returned iterator.
   *
   * @return iterator over WMEs that were added
   */
  public Iterator<Wme> getAddedWmes() {
    return new WmeIterator(added.first);
  }

  /**
   * Returns an iterator over the WMEs that were removed. {@link Iterator#remove()} is <b>not</b>
   * supported by the returned iterator.
   *
   * @return iterator over the WMEs that were removed
   */
  public Iterator<Wme> getRemovedWmes() {
    return new WmeIterator(removed.first);
  }

  private static class WmeIterator implements Iterator<Wme> {
    private ListItem<WmeImpl> next;

    /** @param next */
    WmeIterator(ListItem<WmeImpl> next) {
      this.next = next;
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {
      return next != null;
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    @Override
    public Wme next() {
      if (next == null) {
        throw new NoSuchElementException();
      }
      Wme temp = next.item;
      next = next.next;
      return temp;
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#remove()
     */
    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }
}

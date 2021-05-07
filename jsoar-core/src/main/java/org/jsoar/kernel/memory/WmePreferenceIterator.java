/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 3, 2008
 */
package org.jsoar.kernel.memory;

import java.util.Iterator;
import java.util.NoSuchElementException;
import org.jsoar.kernel.symbols.Symbol;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 *
 * <p>An iterator over all the preferences for a single WME.
 *
 * <p>This is package private, only for use by the implementation of {@link Wme#getPreferences()}.
 *
 * @author ray
 */
class WmePreferenceIterator implements Iterator<Preference> {
  private final Symbol value;
  private Preference next;

  /**
   * Construct iterator
   *
   * @param wme The Wme to iterate over
   */
  public WmePreferenceIterator(WmeImpl wme) {
    this.value = wme.value;

    // Get the slot. If we find it, we'll iterate over all preferences
    // in the slot and filter by the wme value which we store above.
    final Slot slot = Slot.find_slot(wme.id, wme.attr);
    if (slot != null) {
      this.next = getNext(slot.getAllPreferences(), this.value);
    } else {
      this.next = null;
    }
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
  public Preference next() {
    if (next == null) {
      throw new NoSuchElementException();
    }

    final Preference result = next;
    next = getNext(next.nextOfSlot, value);
    return result;
  }

  /* (non-Javadoc)
   * @see java.util.Iterator#remove()
   */
  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }

  private static Preference getNext(Preference start, Symbol value) {
    while (start != null) {
      if (start.value == value) {
        return start;
      }
      start = start.nextOfSlot;
    }
    return null;
  }
}

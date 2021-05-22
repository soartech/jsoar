/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import com.google.common.collect.Iterators;
import java.util.EnumSet;
import java.util.Formatter;
import java.util.Iterator;
import java.util.NoSuchElementException;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.jsoar.kernel.Goal;
import org.jsoar.kernel.GoalDependencySet;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.memory.WmeType;
import org.jsoar.util.ListHead;
import org.jsoar.util.ListItem;
import org.jsoar.util.markers.Marker;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 *
 * <p>This is the internal implementation class for identifier symbols. It should only be used in
 * the kernel. External code (I/O and RHS functions) should use {@link Identifier}
 *
 * <p>Field omitted because they were unused or unnecessary:
 *
 * <ul>
 *   <li>did_PE
 * </ul>
 *
 * @author ray
 */
public class IdentifierImpl extends SymbolImpl implements Identifier {
  private final long name_number; // TODO make this a long

  @Getter private final char nameLetter;

  /**
   * This value is incremented for every incoming ^operator link pointing to this identifier. It is
   * used solely for printing operators.
   */
  public short isa_operator;

  public boolean could_be_a_link_from_below;

  @Getter @Setter private int level;

  public int promotion_level;
  public int link_count;
  public ListItem<IdentifierImpl> unknown_level;
  public Slot slots; // dll of slots for this identifier
  public Marker tc_number; /* used for transitive closures, marking, etc. */
  public SymbolImpl variablization; /* used by the chunker */

  public GoalIdentifierInfo goalInfo;

  // fields used for Soar I/O stuff
  private WmeImpl input_wmes;

  public int depth; /* used to track depth of print (bug 988) RPM 4/07 */

  /* Contains Long Term Identifier for Semantic Memory. Is 0 in case of short term identifier*/
  public long semanticMemoryId = 0;

  public /*epmem_time_id*/ long smem_time_id = 0;
  public /*uint_ptr_t*/ long id_smem_valid = 0;

  public /*epmem_node_id*/ long epmem_id = 0;
  public /*uint64_t*/ long epmem_valid = 0;

  IdentifierImpl(SymbolFactory factory, int hash_id, char name_letter, long name_number) {
    super(factory, hash_id);

    this.nameLetter = name_letter;
    this.name_number = name_number;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.Identifier#getNameNumber()
   */
  @Override
  public long getNameNumber() {
    return name_number;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.IdSymbol#getWmes()
   */
  @Override
  public Iterator<Wme> getWmes() {
    // Return an iterator that is a concatenation of iterators over input WMEs
    // and the WME in each slot
    return Iterators.concat(new WmeIteratorSet(this));
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.Identifier#getWmes(java.util.EnumSet)
   */
  @Override
  public Iterator<Wme> getWmes(EnumSet<WmeType> desired) {
    // Return an iterator that is a concatenation of iterators over input WMEs
    // and the WME in each slot
    return Iterators.concat(new WmeIteratorSet(this, desired));
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.Symbol#asIdentifier()
   */
  @Override
  public IdentifierImpl asIdentifier() {
    return this;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolImpl#isSameTypeAs(org.jsoar.kernel.symbols.SymbolImpl)
   */
  @Override
  public boolean isSameTypeAs(SymbolImpl other) {
    return other.asIdentifier() != null;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolImpl#getFirstLetter()
   */
  @Override
  public char getFirstLetter() {
    return nameLetter;
  }

  /**
   * Returns whether this Identifier is a Long Term Identifier (LTI) within the semantic memory
   *
   * @return true if Long Term Identifier Semantic memory; false otherwise
   */
  public boolean isLongTermIdentifier() {
    return semanticMemoryId != 0;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.Identifier#isGoal()
   */
  @Override
  public boolean isGoal() {
    return goalInfo != null;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.Identifier#isOperator()
   */
  @Override
  public boolean isOperator() {
    return isa_operator != 0;
  }

  /** production.cpp:1043:mark_identifier_if_unmarked */
  private void mark_identifier_if_unmarked(Marker tc, ListHead<IdentifierImpl> id_list) {
    if (tc_number != (tc)) {
      tc_number = (tc);
      if (id_list != null) {
        id_list.push(this);
      }
    }
  }

  public WmeImpl getInputWmes() {
    return input_wmes;
  }

  public void addInputWme(WmeImpl w) {
    this.input_wmes = w.addToList(this.input_wmes);
  }

  public void removeInputWme(WmeImpl w) {
    this.input_wmes = w.removeFromList(this.input_wmes);
  }

  public void removeAllInputWmes() {
    this.input_wmes = null;
  }

  /** production.cpp:1068:unmark_identifiers_and_free_list */
  public static void unmark(ListHead<IdentifierImpl> ids) {
    for (ListItem<IdentifierImpl> id = ids.first; id != null; id = id.next) {
      id.item.tc_number = null;
    }
  }

  public void addSlot(@NonNull Slot slot) {
    slot.next = slots;
    slot.prev = null;

    if (slots != null) {
      slots.prev = slot;
    }
    slots = slot;
  }

  public void removeSlot(Slot slot) {
    if (slot == slots) {
      slots = slot.next;
      if (slots != null) {
        slots.prev = null;
      }
    } else {
      slot.prev.next = slot.next;
      if (slot.next != null) {
        slot.next.prev = slot.prev;
      }
    }
    slot.next = slot.prev = null;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolImpl#add_symbol_to_tc(int, java.util.LinkedList, java.util.LinkedList)
   */
  @Override
  public void add_symbol_to_tc(
      Marker tc, ListHead<IdentifierImpl> id_list, ListHead<Variable> var_list) {
    mark_identifier_if_unmarked(tc, id_list);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolImpl#symbol_is_in_tc(int)
   */
  @Override
  public boolean symbol_is_in_tc(Marker tc) {
    return tc_number == tc;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolImpl#importInto(org.jsoar.kernel.symbols.SymbolFactory)
   */
  @Override
  Symbol importInto(SymbolFactory factory) {
    throw new IllegalStateException("Cannot import identifiers");
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolImpl#numericLess(org.jsoar.kernel.symbols.SymbolImpl)
   */
  @Override
  public boolean numericLess(SymbolImpl other) {
    IdentifierImpl i = other.asIdentifier();
    if (i != null) {
      if (this.nameLetter == i.nameLetter) {
        return this.name_number < i.name_number;
      } else {
        return this.nameLetter < i.nameLetter;
      }
    }

    return super.numericLess(other);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolImpl#numericLessOrEqual(org.jsoar.kernel.symbols.SymbolImpl)
   */
  @Override
  public boolean numericLessOrEqual(SymbolImpl other) {
    IdentifierImpl i = other.asIdentifier();
    if (i != null) {
      if (this.nameLetter == i.nameLetter) {
        return this.name_number <= i.name_number;
      } else {
        return this.nameLetter <= i.nameLetter;
      }
    }

    return super.numericLessOrEqual(other);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolImpl#numericGreater(org.jsoar.kernel.symbols.SymbolImpl)
   */
  @Override
  public boolean numericGreater(SymbolImpl other) {
    IdentifierImpl i = other.asIdentifier();
    if (i != null) {
      if (this.nameLetter == i.nameLetter) {
        return this.name_number > i.name_number;
      } else {
        return this.nameLetter > i.nameLetter;
      }
    }

    return super.numericGreater(other);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolImpl#numericGreaterOrEqual(org.jsoar.kernel.symbols.SymbolImpl)
   */
  @Override
  public boolean numericGreaterOrEqual(SymbolImpl other) {
    IdentifierImpl i = other.asIdentifier();
    if (i != null) {
      if (this.nameLetter == i.nameLetter) {
        return this.name_number >= i.name_number;
      } else {
        return this.nameLetter >= i.nameLetter;
      }
    }

    return super.numericGreaterOrEqual(other);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return (isLongTermIdentifier() ? "@" : "") + nameLetter + name_number;
  }

  /* (non-Javadoc)
   * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
   */
  @Override
  public void formatTo(Formatter formatter, int flags, int width, int precision) {
    formatter.format(toString());
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolImpl#getAdapter(java.lang.Class)
   */
  @Override
  public Object getAdapter(Class<?> klass) {
    if (Goal.class.equals(klass)) {
      return goalInfo;
    } else if (isGoal() && GoalDependencySet.class.equals(klass)) {
      return goalInfo.gds;
    }

    return super.getAdapter(klass);
  }

  private static class WmeIteratorSet implements Iterator<Iterator<Wme>> {
    private final IdentifierImpl id;
    private boolean didImpasseWmes = false;
    private boolean didInputs = false;
    private Slot slot;

    public WmeIteratorSet(IdentifierImpl id, EnumSet<WmeType> desired) {
      this.id = id;
      this.didImpasseWmes = !desired.contains(WmeType.IMPASSE);
      this.didInputs = !desired.contains(WmeType.INPUT);
      this.slot = desired.contains(WmeType.NORMAL) ? id.slots : null;
    }

    public WmeIteratorSet(IdentifierImpl id) {
      this.id = id;
      this.slot = id.slots;
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#hasNext()
     */
    @Override
    public boolean hasNext() {
      return (!didImpasseWmes && id.goalInfo != null && id.goalInfo.getImpasseWmes() != null)
          || (!didInputs && id.getInputWmes() != null)
          || slot != null;
    }

    /* (non-Javadoc)
     * @see java.util.Iterator#next()
     */
    @Override
    public Iterator<Wme> next() {
      // First try to return an iterator over the impasse wmes
      if (!didImpasseWmes && id.goalInfo != null) {
        didImpasseWmes = true;
        if (id.goalInfo.getImpasseWmes() != null) {
          return id.goalInfo.getImpasseWmes().iterator();
        }
      }
      // Next try to return an iterator over the input wmes
      if (!didInputs) {
        didInputs = true;
        if (id.getInputWmes() != null) {
          return id.getInputWmes().iterator();
        }
      }
      // Next return an iterator over the wmes in the current slot and
      // advance to the next slots
      if (slot == null) {
        throw new NoSuchElementException();
      }
      Iterator<Wme> r = slot.getWmeIterator();
      slot = slot.next;
      return r;
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

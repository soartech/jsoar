/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.memory;

import java.util.Collections;
import java.util.FormattableFlags;
import java.util.Formatter;
import java.util.Iterator;
import lombok.Getter;
import lombok.NonNull;
import org.jsoar.kernel.GoalDependencySet;
import org.jsoar.kernel.GoalDependencySetImpl;
import org.jsoar.kernel.io.InputWme;
import org.jsoar.kernel.rete.RightMemory;
import org.jsoar.kernel.rete.Token;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.wma.WorkingMemoryActivation;
import org.jsoar.util.adaptables.AbstractAdaptable;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 *
 * <p>Internal implementation of {@link Wme} interface.
 *
 * <p>Fields in a WME:
 *
 * <ul>
 *   <li>id, attr, value: points to symbols for the wme fields
 *   <li>acceptable: TRUE iff this is an acceptable pref. wme
 *   <li>timetag: timetag of the wme
 *   <li>reference count: (see below)
 *   <li>rete_next, rete_prev: pointers in the doubly-linked list of all wmes currently known to the
 *       rete (header is all_wmes_in_rete) (this equals WM except while WM is being changed)
 *   <li>right_mems: header of a doubly-linked list of right memory entries (in one or more alpha
 *       memories containing the wme). This is used only by the Rete, as part of list-based remove.
 *   <li>tokens: header of a doubly-linked list of tokens in the Rete. This is used only by the
 *       Rete, as part of list-based remove.
 *   <li>next, prev: pointers in a doubly-linked list of wmes. Depending on the wme type, the header
 *       of this DLL is: - slot.wmes (for ordinary wmes) - slot.acceptable_preference_wmes (for
 *       acceptable pref. wmes) - id.impasse_wmes (for architecture-created goal/impasse wmes) -
 *       id.input_wmes (for Soar I/O wmes)
 *   <li>preference: points to the preference supporting the wme. For I/O wmes and (most)
 *       architecture-created wmes, this is NIL.
 *   <li>output_link: this is used only for top-state output links. It points to an output_link
 *       structure used by the I/O routines.
 *   <li>grounds_tc, potentials_tc, locals_tc: used by the chunker to indicate whether this wme is
 *       in the grounds, potentials, and/or locals sets
 *   <li>chunker_bt_pref: used by the chunker; set to {@code cond->bt.trace} when a wme is added to
 *       either the potentials or locals set
 *   <li>These are the additions to the WME structure that will be used to track dependencies for
 *       goals. Each working memory element now includes a pointer to a gds_struct (defined below)
 *       and pointers to other WMEs on the same GDS.
 *   <li>gds: the goal dependency set the wme is in
 *   <li>gds_next, gds_prev: used for dll of all wmes in gds
 * </ul>
 *
 * <p>If a particular working memory element is not dependent for any goal, then the values for
 * these pointers will all be NIL. If a WME is dependent for more than one goal, then it will point
 * to the GDS of the highest goal.
 *
 * <p>Reference counts on wmes: +1 if the wme is currently in WM +1 for each instantiation condition
 * that points to it (bt.wme) We deallocate a wme when its reference count goes to 0.
 *
 * <p>wmem.h:125:wme
 *
 * <p>The following fields or functions were removed because they were unused or unnecessary in
 * Java:
 *
 * <ul>
 *   <li>reference_count
 *   <li>wmem.h:160:wme_add_ref
 *   <li>wme_remove_ref
 * </ul>
 *
 * @author ray
 */
public class WmeImpl extends AbstractAdaptable implements Wme {

  /*
   * Identifier of Working Memory Element
   */
  @NonNull public final IdentifierImpl id;

  public final SymbolImpl attr;
  public final SymbolImpl value;
  public final boolean acceptable;

  /* (non-Javadoc)
   * @see org.jsoar.kernel.memory.Wme#getTimetag()
   */
  @Getter private final int timetag;

  private InputWme outerInputWme;

  private RightMemory right_mems; // used for dll of rm's it's in
  public Token tokens = null; // dll of tokens in rete

  public Preference preference; // pref. supporting it, or null

  public int grounds_tc; /* for chunker use only */
  public int potentials_tc;
  public int locals_tc;

  public /*epmem_node_id*/ long epmem_id = 0;
  public /*uint64_t*/ long epmem_valid = 0;

  public Preference chunker_bt_pref;

  public GoalDependencySetImpl gds;
  public WmeImpl gds_next;
  public WmeImpl gds_prev; // part of dll of wmes in gds

  public WorkingMemoryActivation wma;

  public WmeImpl(
      IdentifierImpl id, SymbolImpl attr, SymbolImpl value, boolean acceptable, int timetag) {
    this(id, attr, value, acceptable, timetag, null);
  }

  public WmeImpl(
      @NonNull IdentifierImpl id,
      SymbolImpl attr,
      SymbolImpl value,
      boolean acceptable,
      int timetag,
      WorkingMemoryActivation wma) {
    this.id = id;
    this.attr = attr;
    this.value = value;
    this.acceptable = acceptable;
    this.timetag = timetag;

    this.wma = wma;
  }

  /**
   * Set the containing InputWme for this wme. This method is only intended to be called by {@link
   * InputWme} implementations.
   *
   * @param outer the outer input wme, or null when detaching
   */
  public void setOuterInputWme(InputWme outer) {
    assert (this.outerInputWme == null && outer != null)
        || (this.outerInputWme != null && outer == null);

    this.outerInputWme = outer;
  }

  /**
   * Retrieve a field by index, 0 = id, 1 = attr, 2 = value.
   *
   * <p>rete.cpp:273:field_from_wme
   *
   * @param field The field index
   * @return The field
   * @throws IllegalArgumentException if index is not 0, 1, or 2
   */
  public SymbolImpl getField(int field) {
    switch (field) {
      case 0:
        return id;
      case 1:
        return attr;
      case 2:
        return value;
    }
    throw new IllegalArgumentException("field_num must be 0, 1, or 2, got" + field);
  }

  public RightMemory getRightMemories() {
    return right_mems;
  }

  public void clearRightMemories() {
    right_mems = null;
  }

  public void addRightMemory(RightMemory rm) {
    right_mems = rm.addToWme(right_mems);
  }

  public void removeRightMemory(RightMemory rm) {
    right_mems = rm.removeFromWme(right_mems);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.memory.Wme#getAttribute()
   */
  @Override
  public Symbol getAttribute() {
    return attr;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.memory.Wme#getIdentifier()
   */
  @Override
  public Identifier getIdentifier() {
    return id;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.memory.Wme#getValue()
   */
  @Override
  public Symbol getValue() {
    return value;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.memory.Wme#isAcceptable()
   */
  @Override
  public boolean isAcceptable() {
    return acceptable;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.memory.Wme#getChildren()
   */
  @Override
  public Iterator<Wme> getChildren() {
    Identifier valueAsId = value.asIdentifier();
    return valueAsId != null ? valueAsId.getWmes() : Collections.emptyIterator();
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.memory.Wme#getPreferences()
   */
  @Override
  public Iterator<Preference> getPreferences() {
    return new WmePreferenceIterator(this);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "(" + timetag + ": " + id + " ^" + attr + " " + value + ")";
  }

  /* (non-Javadoc)
   * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
   */
  @Override
  public void formatTo(Formatter fmt, int f, int width, int precision) {
    // print.cpp:981:print_wme
    // print.cpp:981:print_wme_without_timetag

    // TODO: I don't think that this should automatically insert a newline!
    if ((f & FormattableFlags.ALTERNATE) == 0) {
      // This is the normal print_wme case. It is specified with the
      // usual %s format string
      if (wma != null && wma.wma_enabled()) {
        fmt.format(
            "(%d: %s ^%s %s [%1.2g] %s)\n",
            timetag,
            id,
            attr,
            value,
            wma.wma_get_wme_activation(this, true),
            acceptable ? " +" : "");
      } else {
        fmt.format("(%d: %s ^%s %s%s)\n", timetag, id, attr, value, acceptable ? " +" : "");
      }
    } else {
      // This is the print_wme_without_timetag case
      // It is specified with the %#s format string.
      fmt.format("(%s ^%s %s%s)\n", id, attr, value, acceptable ? " +" : "");
    }

    // <wme tag="123" id="s1" attr="foo" attrtype="string" val="123" valtype="string"></wme>
    // TODO xml_object( thisAgent, w );
  }

  /* (non-Javadoc)
   * @see org.jsoar.util.adaptables.AbstractAdaptable#getAdapter(java.lang.Class)
   */
  @Override
  public Object getAdapter(Class<?> klass) {
    if (InputWme.class.equals(klass)) {
      return outerInputWme;
    } else if (GoalDependencySet.class.equals(klass)) {
      return gds;
    }
    return super.getAdapter(klass);
  }

  /**
   * soar_module.h: 43 This was placed here because it is being used as a concise version of a Wme
   *
   * @author ACNickels
   */
  public static class SymbolTriple {
    public final SymbolImpl id;
    public final SymbolImpl attr;
    public final SymbolImpl value;

    public SymbolTriple(SymbolImpl id, SymbolImpl attr, SymbolImpl value) {
      this.id = id;
      this.attr = attr;
      this.value = value;
    }
  }
}

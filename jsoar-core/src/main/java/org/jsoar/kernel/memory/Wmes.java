/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 16, 2008
 */
package org.jsoar.kernel.memory;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import lombok.NonNull;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.util.Arguments;
import org.jsoar.util.StringTools;

/**
 * {@link Wme} utility routines
 *
 * @author ray
 */
public class Wmes {
  /**
   * Given an iterator over a list of values, constructs a linked list of WMEs using {@link
   * Symbols#create(SymbolFactory, Object)} to convert the values.
   *
   * <p>For example, this code
   *
   * <pre>{@code
   * RhsFunctions.createLinkedList(context, Arrays.asList("hi", "bye", 12345, 3.14).iterator());
   * }</pre>
   *
   * <p>would yield this WM structure:
   *
   * <pre>{@code
   * ^value |hi|
   * ^next
   *    ^value |bye|
   *    ^next
   *        ^value 12345
   *        ^next
   *            ^value 3.14
   *            ^next nil
   * }</pre>
   *
   * The end of the list is marked with {@code ^next nil}. If the list is empty, just {@code nil}
   * will be returned rather than an {@link Identifier}.
   *
   * @param context the RHS function context
   * @param values iterator over the list of values
   * @return the id of the head of the list. If the list is empty, the id will have no attributes.
   */
  public static Symbol createLinkedList(WmeFactory<?> context, Iterator<?> values) {
    return createLinkedList(context, values, "next", "value");
  }

  /**
   * Same as {@link #createLinkedList(WmeFactory, Iterator)}, but the name of the "value" and "next"
   * attributes in the list can be specified
   *
   * @param context the RHS function context
   * @param values iterator over the list of values
   * @param nextName the name of the next attribute
   * @param valueName the name of the value attribute
   * @return the id of the head of the list. If the list is empty, the id will have no attributes.
   */
  public static Symbol createLinkedList(
      WmeFactory<?> context, Iterator<?> values, String nextName, String valueName) {
    final SymbolFactory syms = context.getSymbols();
    final Symbol nextSym = syms.createString(nextName);
    final Symbol valueSym = syms.createString(valueName);

    Identifier head = null;
    Identifier last = null;
    while (values.hasNext()) {
      final Object o = values.next();
      final Identifier current = syms.createIdentifier('N');

      if (head == null) {
        head = current;
      }
      if (last != null) {
        context.addWme(last, nextSym, current);
      }
      context.addWme(current, valueSym, Symbols.create(syms, o));

      last = current;
    }
    if (last != null) {
      context.addWme(last, nextSym, syms.createString("nil"));
    }
    return head != null ? head : syms.createString("nil");
  }

  /**
   * Search working memory for WMEs that match glob expressions.
   *
   * @param wmes iterator over wmes to filter
   * @param id glob expression, or {@code null} for any id
   * @param attr glob expression, or {@code null} for any attr
   * @param value glob expression, or {@code null} for any value
   * @return iterator over wmes matching all three globs
   */
  public static Iterator<Wme> search(Iterator<Wme> wmes, String id, String attr, String value) {
    final Pattern idPattern = Pattern.compile(StringTools.createRegexFromGlob(id).toUpperCase());
    final Pattern attrPattern = Pattern.compile(StringTools.createRegexFromGlob(attr));
    final Pattern valuePattern = Pattern.compile(StringTools.createRegexFromGlob(value));

    final Predicate<Wme> predicate =
        new Predicate<Wme>() {

          @Override
          public boolean apply(Wme w) {
            return idPattern.matcher(w.getIdentifier().toString()).matches()
                && attrPattern.matcher(w.getAttribute().toString()).matches()
                && valuePattern.matcher(w.getValue().toString()).matches();
          }
        };
    return Iterators.filter(wmes, predicate);
  }

  /**
   * Convenience version of {@link Wmes#search(Iterator, String, String, String)} which searches all
   * WMEs in working memory and returns a list.
   *
   * @param agent the agent
   * @param id glob expression, or {@code null} for any id
   * @param attr glob expression, or {@code null} for any attr
   * @param value glob expression, or {@code null} for any value
   * @return List of matching WMEs
   */
  public static List<Wme> search(Agent agent, String id, String attr, String value) {
    return Lists.newArrayList(search(agent.getAllWmesInRete().iterator(), id, attr, value));
  }

  /**
   * Begin constructing a new WME matcher. This uses a builder pattern. Chain methods together to
   * construct a predicate the WME you'd like to find.
   *
   * <p>For example, to find a WME on the output link with the attribute "my-command":
   *
   * <pre>{@code
   * Wme w = Wmes.matcher(agent).attr("my-command").find(agent.getInputOutput().getOutputLink());
   * }</pre>
   *
   * @param syms the agent's symbol factory
   * @return a matcher builder
   */
  public static MatcherBuilder matcher(SymbolFactory syms) {
    return new MatcherBuilder(syms);
  }

  /**
   * Convenience version of {@link #matcher(SymbolFactory)}.
   *
   * @param agent the agent
   * @return new matcher builder
   * @see #matcher(SymbolFactory)
   */
  public static MatcherBuilder matcher(Agent agent) {
    return matcher(agent.getSymbols());
  }

  /**
   * Create a new predicate that matches a particular id/attr/value pattern for a wme. The returned
   * predicate can be used in the filter methods of the Google collections API.
   *
   * <p><b>Note</b>: It is generally preferable to use {@link #matcher(Agent)} to build a match
   * predicate rather than using this method directly.
   *
   * @param syms A symbol factory
   * @param id Desired id, or <code>null</code> for any id
   * @param attr Desired attribute, or <code>null</code> for any attribute
   * @param value Desired value, or <code>null</code> for any value
   * @param timetag Desired timetag, or -1 for any timetag
   * @return New predicate object
   */
  public static Predicate<Wme> newMatcher(
      @NonNull SymbolFactory syms, Identifier id, Object attr, Object value, int timetag) {
    return new MatcherPredicate(
        id,
        attr != null ? Symbols.create(syms, attr) : null,
        value != null ? Symbols.create(syms, value) : null,
        timetag);
  }

  /**
   * Find the first wme that matches the given predicate
   *
   * @param it Iterator to search over
   * @param pred The predicate
   * @return The first wme for which predicate is true, or <code>null</code>
   */
  public static Wme find(Iterator<Wme> it, Predicate<Wme> pred) {
    for (; it.hasNext(); ) {
      final Wme w = it.next();
      if (pred.apply(w)) {
        return w;
      }
    }
    return null;
  }

  /**
   * Filter the given iterator of WMEs with the given predicate.
   *
   * @param it the WME iterator
   * @param pred predicate that tests WMEs
   * @return list of all WMEs {@code w} for whom {@code pred.apply(w)} is true
   */
  public static List<Wme> filter(Iterator<Wme> it, Predicate<Wme> pred) {
    List<Wme> result = new ArrayList<Wme>();
    for (; it.hasNext(); ) {
      final Wme w = it.next();
      if (pred.apply(w)) {
        result.add(w);
      }
    }
    return result;
  }

  /**
   * compare two collections of wmes by value if ignoreIdentifers, then doesn't check identifiers
   * for equality this is useful, e.g., if testing a set of wmes produced by Soar against an
   * expected set, but the ids might be different due to implementation detail changes return true
   * if "equal" return false if not
   */
  public static boolean equal(
      final Collection<Wme> c1, final Collection<Wme> c2, final boolean ignoreIdentifiers) {
    if (c1.size() != c2.size()) {
      return false;
    }

    for (Wme w1 : c1) {
      boolean foundMatch = false;
      for (Wme w2 : c2) {
        if (equal(w1, w2, ignoreIdentifiers)) {
          foundMatch = true;
          break;
        }
      }

      if (!foundMatch) {
        return false;
      }
    }

    // if get here, must be a perfect match
    return true;
  }

  public static boolean equal(Wme w1, Wme w2, boolean ignoreIdentifiers) {
    if (!ignoreIdentifiers && w1.getIdentifier() != w2.getIdentifier()) {
      return false;
    }

    if (!ignoreIdentifiers || w1.getAttribute().asIdentifier() == null) {
      if (w1.getAttribute() != w2.getAttribute()) {
        return false;
      }
    }

    if (!ignoreIdentifiers || w1.getValue().asIdentifier() == null) {
      if (w1.getValue() != w2.getValue()) {
        return false;
      }
    }
    return true;
  }

  public static boolean equalByValue(Wme w1, Wme w2) {
    return (Symbols.equalByValue(w1.getIdentifier(), w2.getIdentifier())
        && Symbols.equalByValue(w1.getAttribute(), w2.getAttribute())
        && Symbols.equalByValue(w1.getValue(), w2.getValue()));
  }

  public static class MatcherBuilder {
    private final SymbolFactory syms;
    private Identifier id;
    private Object attr;
    private Object value;
    private int timetag = -1;

    private MatcherBuilder(SymbolFactory syms) {
      this.syms = syms;
    }

    public MatcherBuilder reset() {
      this.id = null;
      this.attr = null;
      this.value = null;
      this.timetag = -1;
      return this;
    }

    /**
     * @param id the desired id, or <code>null</code> for don't care
     * @return this
     */
    public MatcherBuilder id(Identifier id) {
      this.id = id;
      return this;
    }

    /**
     * @param attr the desired attribute, or <code>null</code> for don't care
     * @return this
     */
    public MatcherBuilder attr(Object attr) {
      this.attr = attr;
      return this;
    }

    /**
     * @param value the desired value, or <code>null</code> for don't care
     * @return this
     */
    public MatcherBuilder value(Object value) {
      this.value = value;
      return this;
    }

    /**
     * @param timetag the desired timetag, or <code>-1</code> for don't care
     * @return this
     */
    public MatcherBuilder timetag(int timetag) {
      this.timetag = timetag;
      return this;
    }

    /**
     * Find a WME in the given iterator
     *
     * @param it the iterator to search
     * @return the WME, or <code>null</code> if not found
     */
    public Wme find(Iterator<Wme> it) {
      return Wmes.find(it, createPredicate());
    }

    /**
     * Find a WME in the set of WMEs with the given id, i.e. using {@link Identifier#getWmes()}
     *
     * @param id the id
     * @return the WME, or <code>null</code> if not found
     */
    public Wme find(Identifier id) {
      return find(id.getWmes());
    }

    public Wme find(Wme parent) {
      return find(parent.getChildren());
    }

    /**
     * Find a WME in the given collection of WMEs
     *
     * @param wmes the wmes
     * @return the WME, or <code>null</code> if not found
     */
    public Wme find(Collection<Wme> wmes) {
      return find(wmes.iterator());
    }

    public List<Wme> filter(Iterator<Wme> it) {
      return Wmes.filter(it, createPredicate());
    }

    public List<Wme> filter(Identifier id) {
      return filter(id.getWmes());
    }

    public List<Wme> filter(Wme parent) {
      return filter(parent.getChildren());
    }

    public List<Wme> filter(Collection<Wme> wmes) {
      return filter(wmes.iterator());
    }

    /** @return a predicate for the current state of this builder */
    public Predicate<Wme> createPredicate() {
      return newMatcher(syms, id, attr, value, timetag);
    }
  }

  private static class MatcherPredicate implements Predicate<Wme> {
    private final Identifier id;
    private final Symbol attr;
    private final Symbol value;
    private final int timetag;

    MatcherPredicate(Identifier id, Symbol attr, Symbol value, int timetag) {
      this.id = id;
      this.attr = attr;
      this.value = value;
      this.timetag = timetag;
    }

    /* (non-Javadoc)
     * @see com.google.common.base.Predicate#apply(java.lang.Object)
     */
    @Override
    public boolean apply(Wme w) {
      if (id != null && id != w.getIdentifier()) {
        return false;
      }
      if (attr != null && attr != w.getAttribute()) {
        return false;
      }
      if (value != null && value != w.getValue()) {
        return false;
      }
      if (timetag >= 0 && timetag != w.getTimetag()) {
        return false;
      }
      return true;
    }
  }
}

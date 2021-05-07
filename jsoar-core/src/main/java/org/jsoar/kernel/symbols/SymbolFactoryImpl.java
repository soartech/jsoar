/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import com.google.common.collect.MapMaker;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.jsoar.kernel.SoarConstants;
import org.jsoar.util.ByRef;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 *
 * <p>This is the internal implementation class for the symbol factory. It should only be used in
 * the kernel. External code (I/O and RHS functions) should use {@link SymbolFactory}
 *
 * <p>Primary symbol management class. This class maintains the symbol "cache" for an agent. When a
 * symbol is created, it is cached for reuse the next time a symbol with the same value is
 * requested. We use <a
 * href="https://guava.dev/releases/snapshot-jre/api/docs/com/google/common/collect/MapMaker.html">Google
 * Guava MapMaker</a> to make weak maps to correctly manage the symbols. Without this, the memory
 * would grow larger over time because symbols can't be garbage collected as long as they're in the
 * cache.
 *
 * <p>The following symtab.cpp functions have been dropped because they're not needed in Java:
 *
 * <ul>
 *   <li>deallocate_symbol
 *   <li>production.cpp:128:copy_symbol_list_adding_references - not needed
 * </ul>
 *
 * <p>symtab.cpp
 *
 * @author ray
 */
public class SymbolFactoryImpl implements SymbolFactory {
  /**
   * A helper method to make the initializations below a little less ugly.
   *
   * @param <K> Map key type
   * @param <V> Map value type
   * @return Reference map with strong key references and weak value references
   */
  private static <K, V> Map<K, V> newReferenceMap() {
    return new MapMaker().weakValues().makeMap();
  }

  private final long id_counter[] = new long[26];
  private final Map<String, StringSymbolImpl> symConstants = newReferenceMap();
  private final Map<Long, IntegerSymbolImpl> intConstants = newReferenceMap();
  private final Map<Double, DoubleSymbolImpl> floatConstants = newReferenceMap();
  private final Map<IdKey, IdentifierImpl> identifiers = newReferenceMap();
  private final Map<String, Variable> variables = newReferenceMap();
  private final Map<Object, JavaSymbolImpl> javaSyms = newReferenceMap();
  private final JavaSymbolImpl nullJavaSym;
  private int current_symbol_hash_id = 0;

  private final VariableGenerator vars = new VariableGenerator(this);

  public SymbolFactoryImpl() {
    nullJavaSym = new JavaSymbolImpl(this, get_next_hash_id(), null);
    reset();
  }

  public VariableGenerator getVariableGenerator() {
    return vars;
  }

  /**
   * Returns a list of all known symbols for use with the "symbols" command.
   *
   * @return a list of all known symbols.
   */
  public List<Symbol> getAllSymbols() {
    final List<Symbol> result = new ArrayList<Symbol>();
    result.addAll(identifiers.values());
    result.addAll(symConstants.values());
    result.addAll(intConstants.values());
    result.addAll(floatConstants.values());
    result.addAll(javaSyms.values());
    return result;
  }

  /**
   * Returns a list of symbols with the given type
   *
   * @param <T> type of symbol
   * @param klass klass of desired symbol type
   * @return list of symbols of the desired type
   * @throws IllegalArgumentException if the desired type is not a known symbol type
   */
  @SuppressWarnings("unchecked")
  public <T extends Symbol> List<T> getSymbols(Class<T> klass) {
    if (klass.isAssignableFrom(StringSymbolImpl.class)) {
      return new ArrayList<T>((Collection<? extends T>) symConstants.values());
    } else if (klass.isAssignableFrom(IntegerSymbolImpl.class)) {
      return new ArrayList<T>((Collection<? extends T>) intConstants.values());
    } else if (klass.isAssignableFrom(DoubleSymbol.class)) {
      return new ArrayList<T>((Collection<? extends T>) floatConstants.values());
    } else if (klass.isAssignableFrom(IdentifierImpl.class)) {
      return new ArrayList<T>((Collection<? extends T>) identifiers.values());
    } else if (klass.isAssignableFrom(Variable.class)) {
      return new ArrayList<T>((Collection<? extends T>) variables.values());
    } else if (klass.isAssignableFrom(JavaSymbolImpl.class)) {
      List<T> result = new ArrayList<T>((Collection<? extends T>) javaSyms.values());
      result.add((T) nullJavaSym);
      return result;
    } else {
      throw new IllegalArgumentException(
          "Expected Symbol implementation class, got '" + klass + "'");
    }
  }

  /** symtab.cpp:474:reset_id_counters */
  public void reset() {
    // Note: In csoar, a warning was printed if any identifiers remained in
    // the cache. This was an indication of a memory leak since ids should
    // have been cleaned up during re-initialization. In Java, the garbage
    // collector picks up symbols when it gets a chance. So, if we're
    // reinitializing, it should be fine to throw out all the existing ids
    // and start over.

    // SMEM - only clear non LTIs
    for (final Iterator<IdentifierImpl> it = identifiers.values().iterator(); it.hasNext(); ) {
      final IdentifierImpl id = it.next();
      if (id.smem_lti == 0) {
        it.remove();
      }
    }

    for (int i = 0; i < id_counter.length; ++i) {
      id_counter[i] = 1;
    }

    // SMEM - id counters are reset externally to this call.
  }

  /** symtab.cpp:510:reset_id_and_variable_tc_numbers */
  public void reset_id_and_variable_tc_numbers() {
    for (IdentifierImpl id : identifiers.values()) {
      id.tc_number = null;
    }
    for (Variable v : variables.values()) {
      v.tc_number = null;
    }
  }

  /** symtab.cpp:523:reset_variable_gensym_numbers */
  public void reset_variable_gensym_numbers() {
    for (Variable v : variables.values()) {
      v.gensym_number = 0;
    }
  }

  /**
   * Added to support SMEM. If the current index for the given letter is less than the max value
   * given, bump it up to max plus one.
   *
   * <p>semantic_memory.cpp:1082:smem_reset_id_counters
   *
   * @param name_letter
   * @param letter_max
   */
  public void resetIdNumber(char name_letter, long letter_max) {
    final int name_letter_index = name_letter - 'A';
    if (id_counter[name_letter_index] <= letter_max) {
      id_counter[name_letter_index] = (int) letter_max + 1; // TODO SMEM make name numbers long
    }
  }

  public long getIdNumber(char name_letter) {
    return id_counter[name_letter - 'A'];
  }

  public long incrementIdNumber(char name_letter) {
    return id_counter[name_letter - 'A']++;
  }

  /**
   * Find the variable with the given name
   *
   * <p>symtab.cpp:195:find_variable
   *
   * @param name the variable name
   * @return the variable, or {@code null} if not found
   */
  public Variable find_variable(String name) {
    return variables.get(name);
  }

  /**
   * Find the variable with the given name or create it if it doesn't exist.
   *
   * <p>symtab.cpp::make_variable
   *
   * @param name the variable name
   * @return a new variable
   */
  public Variable make_variable(String name) {
    Variable v = find_variable(name);
    if (v == null) {
      v = new Variable(this, get_next_hash_id(), name);
      variables.put(v.name, v);
    }
    return v;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolFactory#findIdentifier(char, int)
   */
  public IdentifierImpl findIdentifier(char name_letter, long name_number) {
    return identifiers.get(getIdKey(name_letter, name_number));
  }

  /**
   * Tries to find an identifier and if it finds it in the map of identifiers, sets the value of the
   * key to be null.
   *
   * @param identifier
   * @return Whether or not the identifier was found. Will also return false if identifier is null.
   */
  public boolean findAndNullIdentifier(IdentifierImpl identifier) {
    if (identifier == null) {
      return false;
    }

    boolean found = identifiers.containsValue(identifier);

    if (found) {
      identifiers.remove(new IdKey(identifier.getNameLetter(), identifier.getNameNumber()));
    }

    return found;
  }

  /**
   * symtab.cpp:280:make_new_identifier
   *
   * @param name_letter the name letter
   * @param level the goal stack level of the id
   * @return the new identifier
   */
  public IdentifierImpl make_new_identifier(char name_letter, int /*goal_stack_level*/ level) {
    name_letter = Character.isLetter(name_letter) ? Character.toUpperCase(name_letter) : 'I';
    long name_number = id_counter[name_letter - 'A']++;

    IdentifierImpl id = new IdentifierImpl(this, get_next_hash_id(), name_letter, name_number);

    id.level = level;
    id.promotion_level = level;

    identifiers.put(getIdKey(id.getNameLetter(), id.getNameNumber()), id);
    return id;
  }

  /**
   * This version creates an id with a specific number. NOTE: it does not check if an id with that
   * number already exists. This should only be called indirectly via other methods that do check
   * (e.g., findOrCreateIdentifierExact)
   *
   * @param name_letter the name letter
   * @param name_number the name number
   * @param level the goal stack level of the id
   * @return the new identifier
   */
  public IdentifierImpl make_new_identifier(
      char name_letter, long name_number, int /*goal_stack_level*/ level) {
    name_letter = Character.isLetter(name_letter) ? Character.toUpperCase(name_letter) : 'I';
    if (name_number >= id_counter[name_letter - 'A']) {
      id_counter[name_letter - 'A'] = name_number + 1;
    }
    IdentifierImpl id = new IdentifierImpl(this, get_next_hash_id(), name_letter, name_number);

    id.level = level;
    id.promotion_level = level;

    identifiers.put(getIdKey(id.getNameLetter(), id.getNameNumber()), id);
    return id;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolFactory#createIdentifier(char)
   */
  public IdentifierImpl createIdentifier(char name_letter) {
    return make_new_identifier(name_letter, SoarConstants.TOP_GOAL_LEVEL);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolFactory#findOrcreateIdentifier(char, long)
   */
  @Override
  public IdentifierImpl findOrCreateIdentifier(char nameLetter, long nameNumber) {
    IdentifierImpl id = findIdentifier(nameLetter, nameNumber);

    if (id == null) {
      id = createIdentifier(nameLetter);
    }

    return id;
  }

  /*
   * If the id gets created, this version creates an id with the actual number specified,
   * as opposed to using the next available number
   */
  public IdentifierImpl findOrCreateIdentifierExact(char nameLetter, long nameNumber) {
    IdentifierImpl id = findIdentifier(nameLetter, nameNumber);

    if (id == null) {
      id = make_new_identifier(nameLetter, nameNumber, SoarConstants.TOP_GOAL_LEVEL);
    }

    return id;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolFactory#find_sym_constant(java.lang.String)
   */
  public StringSymbolImpl findString(String name) {
    return symConstants.get(name);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolFactory#make_sym_constant(java.lang.String)
   */
  public StringSymbolImpl createString(String name) {
    StringSymbolImpl sym = findString(name);
    if (sym == null) {
      sym = new StringSymbolImpl(this, get_next_hash_id(), name);
      symConstants.put(name, sym);
    }
    return sym;
  }

  /**
   * symtab.cpp:546:generate_new_sym_constant
   *
   * @param prefix Prefix for the constant
   * @param number Starting index for search. Receives one more than final value of postfix index.
   * @return New StringSymbolImpl
   */
  public StringSymbol generateUniqueString(String prefix, ByRef<Integer> number) {
    String name = prefix + number.value++;
    StringSymbolImpl sym = findString(name);
    while (sym != null) {
      name = prefix + number.value++;
      sym = findString(name);
    }
    return createString(name);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolFactory#createInteger(long)
   */
  public IntegerSymbolImpl createInteger(long value) {
    IntegerSymbolImpl sym = findInteger(value);
    if (sym == null) {
      sym = new IntegerSymbolImpl(this, get_next_hash_id(), value);
      intConstants.put(value, sym);
    }
    return sym;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolFactory#findInteger(long)
   */
  public IntegerSymbolImpl findInteger(long value) {
    return intConstants.get(value);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolFactory#make_float_constant(double)
   */
  public DoubleSymbolImpl createDouble(double value) {
    DoubleSymbolImpl sym = findDouble(value);
    if (sym == null) {
      sym = new DoubleSymbolImpl(this, get_next_hash_id(), value);
      floatConstants.put(value, sym);
    }
    return sym;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolFactory#find_float_constant(double)
   */
  public DoubleSymbolImpl findDouble(double value) {
    return floatConstants.get(value);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolFactory#createJavaSymbol(java.lang.Object)
   */
  @Override
  public JavaSymbolImpl createJavaSymbol(Object value) {
    JavaSymbolImpl sym = findJavaSymbol(value);
    if (sym == null) {
      sym = new JavaSymbolImpl(this, get_next_hash_id(), value);
      javaSyms.put(value, sym);
    }
    return sym;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolFactory#findJavaSymbol(java.lang.Object)
   */
  @Override
  public JavaSymbolImpl findJavaSymbol(Object value) {
    return value != null ? javaSyms.get(value) : nullJavaSym;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.symbols.SymbolFactory#importSymbol(org.jsoar.kernel.symbols.Symbol)
   */
  @Override
  public Symbol importSymbol(Symbol s) {
    if (s instanceof Identifier) {
      throw new IllegalArgumentException(
          "Tried to import identifier " + s + " into symbol factory.");
    }
    if (s instanceof Variable) {
      throw new IllegalArgumentException("Tried to import variable " + s + " into symbol factory.");
    }

    return ((SymbolImpl) s).importInto(this);
  }

  /** symtab.cpp:153:get_next_hash_id */
  private int get_next_hash_id() {
    return current_symbol_hash_id += 137;
  }

  private static IdKey getIdKey(char letter, long number) {
    // Using just a packed only has very minor memory usage or
    // performance gains, so don't bother. IdKey is clearer.
    // return (((long) letter) << 48) | number;
    return new IdKey(letter, number);
  }

  private static class IdKey {
    private final char letter;
    private final long number;

    public IdKey(char letter, long number) {
      this.letter = letter;
      this.number = number;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + letter;
      // See Long.hashCode() for where this comes from...
      result = prime * result + (int) (number ^ (number >>> 32));
      return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      final IdKey other = (IdKey) obj;
      if (letter != other.letter) return false;
      if (number != other.number) return false;
      return true;
    }
  }
}

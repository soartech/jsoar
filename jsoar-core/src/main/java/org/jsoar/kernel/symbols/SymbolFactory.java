/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 14, 2008
 */
package org.jsoar.kernel.symbols;

import org.jsoar.util.ByRef;

/**
 * Interface for an object that constructs and manages symbols in a Soar agent.
 *
 * @author ray
 * @see Symbols
 */
public interface SymbolFactory {
  /**
   * Find an existing identifier
   *
   * <p>symtab.cpp:207:find_identifier
   *
   * @param nameLetter The name letter of the desired id
   * @param nameNumber The name number of the desired id
   * @return The identifier, or <code>null</code> if not found
   */
  public Identifier findIdentifier(char nameLetter, long nameNumber);

  /**
   * Create a new identifier
   *
   * <p>symbol.cpp::make_new_identifier
   *
   * <p>io.cpp::get_new_io_identifier
   *
   * @param nameLetter The letter of the id
   * @return A new identifier
   */
  public Identifier createIdentifier(char nameLetter);

  /**
   * Find an identifier by letter and number, and create a new one with the given letter if none was
   * found. This is simply a compound of {@link #findIdentifier(char, long)} and {@link
   * #createIdentifier(char)}
   *
   * <p>io.cpp::get_io_identifier
   *
   * @param nameLetter The name letter of the id
   * @param nameNumber The name number of the id
   * @return The identifier
   */
  public Identifier findOrCreateIdentifier(char nameLetter, long nameNumber);

  /**
   * Find an existing string symbol
   *
   * <p>symtab.cpp::find_sym_constant
   *
   * @param value The string value of the symbol
   * @return The symbol, or <code>null</code> if not found
   */
  public StringSymbol findString(String value);

  /**
   * Create a new string symbol.
   *
   * <p>symtab.cpp:328:make_sym_constant
   *
   * <p>io.cpp::get_io_sym_constant
   *
   * @param value The string value of the symbol
   * @return A symbol. Subsequent calls with the same value will return the same object.
   */
  public StringSymbol createString(String value);

  /**
   * symtab.cpp:546:generate_new_sym_constant
   *
   * @param prefix Prefix for the constant
   * @param number Starting index for search. Receives one more than final value of postfix index.
   * @return New string
   */
  public StringSymbol generateUniqueString(String prefix, ByRef<Integer> number);

  /**
   * Create a new integer symbol
   *
   * <p>symtab.cpp:346:make_int_constant
   *
   * <p>io.cpp::get_io_int_constant
   *
   * @param value The integer value of the symbol
   * @return A symbol. Subsequent calls with the same value will return the same object.
   */
  public IntegerSymbol createInteger(long value);

  /**
   * Find an existing integer symbol
   *
   * <p>symtab.cpp::find_int_constant
   *
   * @param value The integer value of the symbol
   * @return The symbol, or <code>null</code> if not found
   */
  public IntegerSymbol findInteger(long value);

  /**
   * Create a new double symbol
   *
   * <p>symtab.cpp:363:make_float_constant
   *
   * <p>io.cpp::get_io_float_constant
   *
   * @param value The double value of the symbol
   * @return The symbol
   */
  public DoubleSymbol createDouble(double value);

  /**
   * Find an existing double symbol
   *
   * <p>symtab.cpp::find_float_constant
   *
   * @param value The double value of the symbol
   * @return A symbol. Subsequent calls with the same value will return the same object.
   */
  public DoubleSymbol findDouble(double value);

  /**
   * Create a Java symbol for the given value.
   *
   * @param value The Java object value
   * @return The symbol
   */
  public JavaSymbol createJavaSymbol(Object value);

  /**
   * Look up a Java symbol for the given value.
   *
   * @param value The Java object value
   * @return The symbol, or <code>null</code> if not found
   */
  public JavaSymbol findJavaSymbol(Object value);

  /**
   * Import a symbol, possibly from another symbol factory into this factory.
   *
   * <p>Returns a new symbol, owned by this factory, whose value is a copy of the input symbol. If
   * the symbol is already owned by this factory, it is returned unchanged. If the symbol is an
   * identifier, an exception is thrown. If the symbol is a {@link JavaSymbol}, the value is shared.
   *
   * @param s the symbol to import
   * @return imported symbol
   * @throws IllegalArgumentException if the symbol is an identifier
   */
  public Symbol importSymbol(Symbol s);
}

/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 14, 2008
 */
package org.jsoar.kernel.symbols;

/**
 * Interface for an object that constructs and manages symbols in a Soar
 * agent.
 * 
 * @author ray
 */
public interface SymbolFactory
{
    /**
     * Find an existing identifier
     * 
     * <p>symtab.cpp:207:find_identifier
     * 
     * @param nameLetter The name letter of the desired id
     * @param nameNumber The name number of the desired id
     * @return The identifier, or <code>null</code> if not found
     */
    public Identifier findIdentifier(char nameLetter, int nameNumber);

    /**
     * Create a new identifier
     * 
     * <p>symbol.cpp::make_new_identifier
     * 
     * @param nameLetter The letter of the id
     * @return A new identifier
     */
    public Identifier createIdentifier(char nameLetter);

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
     * @param value The string value of the symbol
     * @return A symbol. Subsequent calls with the same value will return the
     *      same object.
     */
    public StringSymbol createString(String value);

    /**
     * Create a new integer symbol
     * 
     * <p>symtab.cpp:346:make_int_constant
     * 
     * @param value The integer value of the symbol
     * @return A symbol. Subsequent calls with the same value will return the
     *      same object.
     */
    public IntegerSymbol createInteger(int value);

    /**
     * Find an existing integer symbol
     * 
     * <p>symtab.cpp::find_int_constant
     * 
     * @param value The integer value of the symbol
     * @return The symbol, or <code>null</code> if not found
     */
    public IntegerSymbol findInteger(int value);

    /**
     * Create a new double symbol
     * 
     * <p>symtab.cpp:363:make_float_constant
     * 
     * @param value The double value of the symbol
     * @return The symbol, or <code>null</code> if not found
     */
    public DoubleSymbol createDouble(double value);

    /**
     * Find an existing double symbol
     * 
     * <p>symtab.cpp::find_float_constant
     * 
     * @param value The double value of the symbol
     * @return A symbol. Subsequent calls with the same value will return the
     *      same object.
     */
    public DoubleSymbol findDouble(double value);

}

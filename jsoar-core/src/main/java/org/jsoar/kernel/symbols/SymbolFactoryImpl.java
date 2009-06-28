/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.SoarConstants;
import org.jsoar.util.ByRef;

import com.google.common.collect.MapMaker;

/**
 * This is the internal implementation class for the symbol factory. It should
 * only be used in the kernel. External code (I/O and RHS functions) should use
 * {@link SymbolFactory}

 * <p>Primary symbol management class. This class maintains the symbol "cache"
 * for an agent. When a symbol is created, it is cached for reuse the next
 * time a symbol with the same value is requested. We use 
 * <a href="http://google-collections.googlecode.com/svn/trunk/javadoc/com/google/common/collect/ReferenceMap.html">Google Collections ReferenceMap</a>
 * to correctly manage the maps. Without this, the memory would grow larger over 
 * time because symbols can't be garbage collected as long as they're in the
 * cache.
 * 
 * <p>The following symtab.cpp functions have been dropped because they're not
 * needed in Java:
 * <ul>
 * <li>deallocate_symbol
 * <li>production.cpp:128:copy_symbol_list_adding_references - not needed
 * </ul>
 * 
 * <p>symtab.cpp
 * 
 * @author ray
 */
public class SymbolFactoryImpl implements SymbolFactory
{
    /**
     * A helper method to make the initializations below a little less ugly.
     * 
     * @param <K> Map key type
     * @param <V> Map value type
     * @return Reference map with strong key references and weak value references
     */
    private static <K, V> Map<K, V> newReferenceMap()
    {
        return new MapMaker().weakValues().makeMap();
    }
    
    private final int id_counter[] = new int[26];
    private final Map<String, StringSymbolImpl> symConstants = newReferenceMap();
    private final Map<Integer, IntegerSymbolImpl> intConstants = newReferenceMap();
    private final Map<Double, DoubleSymbolImpl> floatConstants = newReferenceMap();
    private final Map<IdKey, IdentifierImpl> identifiers = newReferenceMap();
    private final Map<String, Variable> variables = newReferenceMap();
    private final Map<Object, JavaSymbolImpl> javaSyms = newReferenceMap();
    private final JavaSymbolImpl nullJavaSym;
    private int current_symbol_hash_id = 0; 
    
    public SymbolFactoryImpl()
    {
        nullJavaSym = new JavaSymbolImpl(get_next_hash_id(), null);
        reset();
    }
    
    /**
     * Returns a list of all known symbols for use with the "symbols" command.
     * 
     * @return a list of all known symbols. 
     */
    public List<Symbol> getAllSymbols()
    {
        final List<Symbol> result = new ArrayList<Symbol>();
        result.addAll(symConstants.values());
        result.addAll(intConstants.values());
        result.addAll(floatConstants.values());
        result.addAll(javaSyms.values());
        return result;
    }
    
    /**
     * <p>symtab.cpp:474:reset_id_counters
     */
    public void reset()
    {
        // Note: In csoar, a warning was printed if any identifiers remained in
        // the cache. This was an indication of a memory leak since ids should
        // have been cleaned up during re-initialization. In Java, the garbage
        // collectors picks up symbols when it gets a chance. So, if we're 
        // reinitializing, it should be fine to throw out all the existing ids
        // and start over.
        identifiers.clear();
                
        for(int i = 0; i < id_counter.length; ++i)
        {
            id_counter[i] = 1;
        }
    }
    
    /**
     * <p>symtab.cpp:510:reset_id_and_variable_tc_numbers
     */
    public void reset_id_and_variable_tc_numbers()
    {
        for(IdentifierImpl id : identifiers.values())
        {
            id.tc_number = null;
        }
        for(Variable v : variables.values())
        {
            v.tc_number = null;
        }
    }
    
    /**
     * <p>symtab.cpp:523:reset_variable_gensym_numbers
     */
    public void reset_variable_gensym_numbers()
    {
        for(Variable v : variables.values())
        {
            v.gensym_number = 0;
        }
    }
    
    /**
     * <p>symtab.cpp:195:find_variable
     * 
     * @param name
     * @return the variable, or {@code null} if not found
     */
    public Variable find_variable(String name)
    {
        return variables.get(name);
    }
    
    public Variable make_variable(String name)
    {
        Variable v = find_variable(name);
        if(v == null)
        {
            v = new Variable(get_next_hash_id(), name);
            variables.put(v.name, v);
        }
        return v;
    }
    
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolFactory#find_identifier(char, int)
     */
    public IdentifierImpl findIdentifier(char name_letter, int name_number)
    {
        return identifiers.get(new IdKey(name_letter, name_number));
    }
    
    /**
     * <p>symtab.cpp:280:make_new_identifier
     * 
     * @param name_letter the name letter
     * @param level the goal stack level of the id
     * @return the new identifier
     */
    public IdentifierImpl make_new_identifier(char name_letter, int /*goal_stack_level*/ level)
    {
        name_letter = Character.isLetter(name_letter) ? Character.toUpperCase(name_letter) : 'I';
        int name_number = id_counter[name_letter - 'A']++;
        
        IdentifierImpl id = new IdentifierImpl(get_next_hash_id(), name_letter, name_number);
        
        id.level = level;
        id.promotion_level = level;
        
        identifiers.put(new IdKey(id.getNameLetter(), id.getNameNumber()), id);
        return id;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolFactory#createIdentifier(char)
     */
    public IdentifierImpl createIdentifier(char name_letter)
    {
        return make_new_identifier(name_letter, SoarConstants.TOP_GOAL_LEVEL);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolFactory#findOrcreateIdentifier(char, int)
     */
    @Override
    public Identifier findOrCreateIdentifier(char nameLetter, int nameNumber)
    {
        IdentifierImpl id = findIdentifier(nameLetter, nameNumber);

        if (id == null)
        {
            id = createIdentifier(nameLetter);
        }

        return id;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolFactory#find_sym_constant(java.lang.String)
     */
    public StringSymbolImpl findString(String name)
    {
        return symConstants.get(name);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolFactory#make_sym_constant(java.lang.String)
     */
    public StringSymbolImpl createString(String name)
    {
        StringSymbolImpl sym = findString(name);
        if(sym == null)
        {
            sym = new StringSymbolImpl(get_next_hash_id(), name);
            symConstants.put(name, sym);
        }
        return sym;
    }
    
    /**
     * symtab.cpp:546:generate_new_sym_constant
     * 
     * @param prefix Prefix for the constant
     * @param number Starting index for search. Receives one more than final value 
     *               of postfix index.
     * @return New StringSymbolImpl
     */
    public StringSymbol generateUniqueString(String prefix, ByRef<Integer> number)
    {
        String name = prefix + number.value++;
        StringSymbolImpl sym = findString(name);
        while(sym != null)
        {
            name = prefix + number.value++;
            sym = findString(name);
        }
        return createString(name);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolFactory#make_int_constant(int)
     */
    public IntegerSymbolImpl createInteger(int value)
    {
        IntegerSymbolImpl sym = findInteger(value);
        if(sym == null)
        {
            sym = new IntegerSymbolImpl(get_next_hash_id(), value);
            intConstants.put(value, sym);
        }
        return sym;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolFactory#find_int_constant(int)
     */
    public IntegerSymbolImpl findInteger(int value)
    {
        return intConstants.get(value);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolFactory#make_float_constant(double)
     */
    public DoubleSymbolImpl createDouble(double value)
    {
        DoubleSymbolImpl sym = findDouble(value);
        if(sym == null)
        {
            sym = new DoubleSymbolImpl(get_next_hash_id(), value);
            floatConstants.put(value, sym);
        }
        return sym;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolFactory#find_float_constant(double)
     */
    public DoubleSymbolImpl findDouble(double value)
    {
        return floatConstants.get(value);
    }
        
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolFactory#createJavaSymbol(java.lang.Object)
     */
    @Override
    public JavaSymbolImpl createJavaSymbol(Object value)
    {
        JavaSymbolImpl sym = findJavaSymbol(value);
        if(sym == null)
        {
            sym = new JavaSymbolImpl(get_next_hash_id(), value);
            javaSyms.put(value, sym);
        }
        return sym;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolFactory#findJavaSymbol(java.lang.Object)
     */
    @Override
    public JavaSymbolImpl findJavaSymbol(Object value)
    {
        return value != null ? javaSyms.get(value) : nullJavaSym;
    }

    /**
     * symtab.cpp:153:get_next_hash_id
     * 
     * @return
     */
    private int get_next_hash_id()
    {
        return current_symbol_hash_id += 137;
    }
    
    private static class IdKey
    {
        char name_letter;
        int name_number;
        
        /**
         * @param name_letter
         * @param name_number
         */
        public IdKey(char name_letter, int name_number)
        {
            this.name_letter = name_letter;
            this.name_number = name_number;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + name_letter;
            result = prime * result + name_number;
            return result;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final IdKey other = (IdKey) obj;
            if (name_letter != other.name_letter)
                return false;
            if (name_number != other.name_number)
                return false;
            return true;
        }
        
        
    }
}

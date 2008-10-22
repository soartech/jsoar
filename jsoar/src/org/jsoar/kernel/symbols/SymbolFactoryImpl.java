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

import com.google.common.base.ReferenceType;
import com.google.common.collect.ReferenceMap;

/**
 * Primary symbol management class. This class maintains the symbol "cache"
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
        return new ReferenceMap<K, V>(ReferenceType.STRONG, ReferenceType.WEAK);
    }
    
    private int id_counter[] = new int[26];
    private Map<String, StringSymbolImpl> symConstants = newReferenceMap();
    private Map<Integer, IntegerSymbolImpl> intConstants = newReferenceMap();
    private Map<Double, DoubleSymbolImpl> floatConstants = newReferenceMap();
    private Map<IdKey, IdentifierImpl> identifiers = newReferenceMap();
    private Map<String, Variable> variables = newReferenceMap();

    private int current_tc_number = 0;
    private int current_symbol_hash_id = 0; 
    
    public SymbolFactoryImpl()
    {
        reset();
    }
    
    /**
     * Get_new_tc_number() is called from lots of places.  Any time we need
     * to mark a set of identifiers and/or variables, we get a new tc_number
     * by calling this routine, then proceed to mark various ids or vars
     * by setting the sym->id.tc_num or sym->var.tc_num fields.
     * 
     * <p>A global tc number counter is maintained and incremented by this
     * routine in order to generate a different tc_number each time.  If
     * the counter ever wraps around back to 0, we bump it up to 1 and
     * reset the the tc_num fields on all existing identifiers and variables
     * to 0.
     * 
     * @return
     */
    public int get_new_tc_number()
    {
        current_tc_number++;
        if (current_tc_number==Integer.MAX_VALUE) {
          reset_id_and_variable_tc_numbers ();
          current_tc_number = 1;
        }
        return current_tc_number;
    }
    
    /**
     * symtab.cpp:474:reset_id_counters
     * 
     * @return
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
     * symtab.cpp:510:reset_id_and_variable_tc_numbers
     */
    public void reset_id_and_variable_tc_numbers()
    {
        for(IdentifierImpl id : identifiers.values())
        {
            id.tc_number = 0;
        }
        for(Variable v : variables.values())
        {
            v.tc_number = 0;
        }
    }
    
    /**
     * symtab.cpp:523:reset_variable_gensym_numbers
     */
    public void reset_variable_gensym_numbers()
    {
        for(Variable v : variables.values())
        {
            v.gensym_number = 0;
        }
    }
    
    /**
     * symtab.cpp:195:find_variable
     * 
     * @param name
     * @return
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
     * 
     * symtab.cpp:280:make_new_identifier
     * 
     * @param name_letter
     * @param level
     * @return
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
    public Identifier findOrcreateIdentifier(char nameLetter, int nameNumber)
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
    public StringSymbolImpl generate_new_sym_constant(String prefix, ByRef<Integer> number)
    {
//        Arguments.checkNotNull(prefix, "prefix");
//        Arguments.checkNotNull(number, "number");
        
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
    
    /**
     * Converts a list of arguments into a list of symbols of the appropriate type.
     * First tries basic numeric types. If those fail, just creates string symbols
     * using toString().
     * 
     * @param args List of objects
     * @return List of symbols
     */
    public List<Symbol> makeList(Object... args)
    {
        List<Symbol> result = new ArrayList<Symbol>(args.length);
        for(Object arg : args)
        {
            if(arg instanceof Double)
            {
                result.add(createDouble(((Double) arg).doubleValue()));
            }
            else if(arg instanceof Float)
            {
                result.add(createDouble(((Float) arg).doubleValue()));
            }
            else if(arg instanceof Integer)
            {
                result.add(createInteger(((Integer) arg).intValue()));
            }
            else
            {
                result.add(createString(arg.toString()));
            }
        }
        return result;
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

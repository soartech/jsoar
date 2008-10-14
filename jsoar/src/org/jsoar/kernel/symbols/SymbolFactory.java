/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
 * </ul>
 * 
 * <p>symtab.cpp
 * 
 * @author ray
 */
public class SymbolFactory
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
    private Map<String, SymConstant> symConstants = newReferenceMap();
    private Map<Integer, IntConstant> intConstants = newReferenceMap();
    private Map<Double, FloatConstant> floatConstants = newReferenceMap();
    private Map<IdKey, Identifier> identifiers = newReferenceMap();
    private Map<String, Variable> variables = newReferenceMap();

    private int current_tc_number = 0;
    private int current_symbol_hash_id = 0; 
    
    public SymbolFactory()
    {
        reset_id_counters();
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
    public boolean reset_id_counters()
    {
        if(!identifiers.isEmpty())
        {
            // TODO: caller should print this warning on false return
//            print (thisAgent, "Internal warning:  wanted to reset identifier generator numbers, but\n");
//            print (thisAgent, "there are still some identifiers allocated.  (Probably a memory leak.)\n");
//            print (thisAgent, "(Leaving identifier numbers alone.)\n");
//            xml_generate_warning(thisAgent, "Internal warning:  wanted to reset identifier generator numbers, but\nthere are still some identifiers allocated.  (Probably a memory leak.)\n(Leaving identifier numbers alone.)");
            return false;
        }
        
        for(int i = 0; i < id_counter.length; ++i)
        {
            id_counter[i] = 1;
        }
        return true;
    }
    
    /**
     * symtab.cpp:510:reset_id_and_variable_tc_numbers
     */
    public void reset_id_and_variable_tc_numbers()
    {
        for(Identifier id : identifiers.values())
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
    
    
    /**
     * 
     * symtab.cpp:207:find_identifier
     * 
     * @param name_letter
     * @param name_number
     * @return
     */
    public Identifier find_identifier(char name_letter, int name_number)
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
    public Identifier make_new_identifier(char name_letter, int /*goal_stack_level*/ level)
    {
        name_letter = Character.isLetter(name_letter) ? Character.toUpperCase(name_letter) : 'I';
        int name_number = id_counter[name_letter - 'A']++;
        
        Identifier id = new Identifier(get_next_hash_id(), name_letter, name_number);
        
        id.level = level;
        id.promotion_level = level;
        
        identifiers.put(new IdKey(id.name_letter, id.name_number), id);
        return id;
    }
    
    
    public SymConstant find_sym_constant(String name)
    {
        return symConstants.get(name);
    }
    
    /**
     * symtab.cpp:328:make_sym_constant
     * 
     * @param name
     * @return
     */
    public SymConstant make_sym_constant(String name)
    {
        SymConstant sym = find_sym_constant(name);
        if(sym == null)
        {
            sym = new SymConstant(get_next_hash_id(), name);
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
     * @return New SymConstant
     */
    public SymConstant generate_new_sym_constant(String prefix, ByRef<Integer> number)
    {
//        Arguments.checkNotNull(prefix, "prefix");
//        Arguments.checkNotNull(number, "number");
        
        String name = prefix + number.value++;
        SymConstant sym = find_sym_constant(name);
        while(sym != null)
        {
            name = prefix + number.value++;
            sym = find_sym_constant(name);
        }
        return make_sym_constant(name);
    }
    
    /**
     * symtab.cpp:346:make_int_constant
     * 
     * @param value
     * @return
     */
    public IntConstant make_int_constant(int value)
    {
        IntConstant sym = find_int_constant(value);
        if(sym == null)
        {
            sym = new IntConstant(get_next_hash_id(), value);
            intConstants.put(value, sym);
        }
        return sym;
    }
    
    public IntConstant find_int_constant(int value)
    {
        return intConstants.get(value);
    }
    
    /**
     * symtab.cpp:363:make_float_constant
     * 
     * @param value
     * @return
     */
    public FloatConstant make_float_constant(double value)
    {
        FloatConstant sym = find_float_constant(value);
        if(sym == null)
        {
            sym = new FloatConstant(get_next_hash_id(), value);
            floatConstants.put(value, sym);
        }
        return sym;
    }
    
    public FloatConstant find_float_constant(double value)
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
                result.add(make_float_constant(((Double) arg).doubleValue()));
            }
            else if(arg instanceof Float)
            {
                result.add(make_float_constant(((Float) arg).doubleValue()));
            }
            else if(arg instanceof Integer)
            {
                result.add(make_int_constant(((Integer) arg).intValue()));
            }
            else
            {
                result.add(make_sym_constant(arg.toString()));
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

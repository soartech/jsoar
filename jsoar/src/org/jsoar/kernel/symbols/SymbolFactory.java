/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.util.HashMap;
import java.util.Map;

import org.jsoar.util.Arguments;
import org.jsoar.util.ByRef;

/**
 * @author ray
 */
public class SymbolFactory
{
    private int id_counter[] = new int[26];
    private Map<String, SymConstant> symConstants = new HashMap<String, SymConstant>();
    private Map<Integer, IntConstant> intConstants = new HashMap<Integer, IntConstant>();
    private Map<Double, FloatConstant> floatConstants = new HashMap<Double, FloatConstant>();
    private Map<IdKey, Identifier> identifiers = new HashMap<IdKey, Identifier>();
    private Map<String, Variable> variables = new HashMap<String, Variable>();

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
     * A global tc number counter is maintained and incremented by this
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
            // TODO
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
        Arguments.checkNotNull(prefix, "prefix");
        Arguments.checkNotNull(number, "number");
        
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
    
    
    public void deallocate_symbol(Symbol sym)
    {
        Variable var = sym.asVariable();
        if(var != null)
        {
            variables.remove(var.name);
            return;
        }
        Identifier id = sym.asIdentifier();
        if(id != null)
        {
            identifiers.remove(new IdKey(id.name_letter, id.name_number));
            return;
        }
        SymConstant sc = sym.asSymConstant();
        if(sc != null)
        {
            symConstants.remove(sc.name);
            return;
        }
        IntConstant ic = sym.asIntConstant();
        if(ic != null)
        {
            intConstants.remove(ic.value);
            return;
        }
        FloatConstant fc = sym.asFloatConstant();
        if(fc != null)
        {
            floatConstants.remove(fc.value);
            return;
        }
        throw new IllegalArgumentException("Unkonwn symbol type!");
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

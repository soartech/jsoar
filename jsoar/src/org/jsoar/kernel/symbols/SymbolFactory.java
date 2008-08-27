/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.util.HashMap;
import java.util.Map;

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
    
    public SymbolFactory()
    {
        reset_id_counters();
    }
    
    /* =====================================================================

    Increment TC Counter and Return New TC Number

Get_new_tc_number() is called from lots of places.  Any time we need
to mark a set of identifiers and/or variables, we get a new tc_number
by calling this routine, then proceed to mark various ids or vars
by setting the sym->id.tc_num or sym->var.tc_num fields.

A global tc number counter is maintained and incremented by this
routine in order to generate a different tc_number each time.  If
the counter ever wraps around back to 0, we bump it up to 1 and
reset the the tc_num fields on all existing identifiers and variables
to 0.
===================================================================== */
    public int get_new_tc_number()
    {
        current_tc_number++;
        if (current_tc_number==Integer.MAX_VALUE) {
          reset_id_and_variable_tc_numbers ();
          current_tc_number = 1;
        }
        return current_tc_number;
    }
    
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
    
    public void reset_variable_gensym_numbers()
    {
        for(Variable v : variables.values())
        {
            v.gensym_number = 0;
        }
    }
    
    public Variable find_variable(String name)
    {
        return variables.get(name);
    }
    public Variable make_variable(String name)
    {
        Variable v = find_variable(name);
        if(v == null)
        {
            v = new Variable();
            v.name = name;
            variables.put(v.name, v);
        }
        return v;
    }
    
    
    public Identifier find_identifier(char name_letter, int name_number)
    {
        return identifiers.get(new IdKey(name_letter, name_number));
    }
    
    public Identifier make_new_identifier(char name_letter, short /*goal_stack_level*/ level)
    {
        Identifier id = new Identifier();
        
        name_letter = Character.isLetter(name_letter) ? Character.toUpperCase(name_letter) : 'I';
        
        id.name_letter = name_letter;
        id.name_number = id_counter[name_letter - 'A']++;
        id.level = level;
        id.promotion_level = level;
        
        identifiers.put(new IdKey(id.name_letter, id.name_number), id);
        return id;
    }
    
    
    public SymConstant find_sym_constant(String name)
    {
        return symConstants.get(name);
    }
    
    public SymConstant make_sym_constant(String name)
    {
        SymConstant sym = find_sym_constant(name);
        if(sym == null)
        {
            sym = new SymConstant();
            sym.name = name;
            symConstants.put(name, sym);
        }
        return sym;
    }
    
    public SymConstant generate_new_sym_constant(String prefix, int[] number)
    {
        if(number.length != 1)
        {
            throw new IllegalArgumentException("number must be an array of size 1");
        }
        
        String name = prefix + number[0]++;
        SymConstant sym = find_sym_constant(name);
        while(sym != null)
        {
            name = prefix + number[0]++;
            sym = find_sym_constant(name);
        }
        return make_sym_constant(name);
    }
    
    public IntConstant make_int_constant(int value)
    {
        IntConstant sym = find_int_constant(value);
        if(sym == null)
        {
            sym = new IntConstant();
            sym.value = value;
            intConstants.put(value, sym);
        }
        return sym;
    }
    
    public IntConstant find_int_constant(int value)
    {
        return intConstants.get(value);
    }
    
    public FloatConstant make_float_constant(double value)
    {
        FloatConstant sym = find_float_constant(value);
        if(sym == null)
        {
            sym = new FloatConstant();
            sym.value = value;
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

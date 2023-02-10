/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.util.Formatter;
import java.util.LinkedList;

import org.jsoar.util.ListHead;
import org.jsoar.util.ListItem;
import org.jsoar.util.markers.Marker;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 * 
 * @author ray
 */
public class Variable extends SymbolImpl
{
    public final String name;
    public Marker tc_number;
    public SymbolImpl current_binding_value;
    /**
     * In C, this just gets shoved in the current_binding_value pointer. Can't do that in
     * Java, so it's maintained separately.
     */
    public int unbound_variable_index;
    
    public int gensym_number;
    
    /**
     * See rete.cpp:2285 for why this is a stack.
     */
    public final LinkedList<Integer> rete_binding_locations = new LinkedList<Integer>();
    
    /**
     * @param hash_id the variable's hash id
     * @param name the variable name
     */
    /* package */ Variable(SymbolFactory factory, int hash_id, String name)
    {
        super(factory, hash_id);
        
        this.name = name;
    }
    
    /**
     * <p>production.cpp:1058:mark_variable_if_unmarked
     * 
     * @param tc_number
     * @param var_list
     */
    public void markIfUnmarked(Marker tc_number, ListHead<Variable> var_list)
    {
        if(this.tc_number != tc_number)
        {
            this.tc_number = tc_number;
            if(var_list != null)
            {
                var_list.push(this);
            }
        }
    }
    
    /**
     * <p>production.cpp:1081:unmark_variables_and_free_list
     * 
     * @param vars list of vars to unmark
     */
    public static void unmark(ListHead<Variable> vars)
    {
        for(ListItem<Variable> it = vars.first; it != null; it = it.next)
        {
            it.item.unmark();
        }
    }
    
    public void unmark()
    {
        this.tc_number = null;
    }
    
    /**
     * <p>rete.cpp:2312:var_is_bound
     * 
     * @return true if this variable is bound
     */
    public boolean var_is_bound()
    {
        return !rete_binding_locations.isEmpty();
    }
    
    /**
     * <p>rete.cpp:2323:varloc_to_dummy
     * 
     * @param depth
     * @param field_num
     * @return dummy version of variable location
     */
    private static int varloc_to_dummy(/* rete_node_level */ int depth, int field_num)
    {
        return ((depth) << 2) + field_num;
    }
    
    /**
     * rete.cpp:2328:dummy_to_varloc_depth
     * 
     * @param d
     * @return the depth part of a dummy
     */
    public static int dummy_to_varloc_depth(int d)
    {
        return d >> 2;
    }
    
    /**
     * rete.cpp:2333:dummy_to_varloc_field_num
     * 
     * @param d
     * @return the field part of a dummy
     */
    public static int dummy_to_varloc_field_num(int d)
    {
        return d & 3;
    }
    
    /**
     * rete.cpp:2342:push_var_binding
     * 
     * @param depth
     * @param field_num
     */
    public void push_var_binding(/* rete_node_level */ int depth, int field_num)
    {
        int dummy_xy312 = varloc_to_dummy((depth), (field_num));
        rete_binding_locations.push(dummy_xy312);
    }
    
    /**
     * rete.cpp:2355:pop_var_binding
     */
    public void pop_var_binding()
    {
        rete_binding_locations.pop();
    }
    
    /**
     * This routine takes a list of variables; for each item {@code <v>} on the
     * list, it pops a binding of {@code <v>}. It also deallocates the list.
     * This is often used for un-binding a group of variables which got
     * bound in some procedure.
     * 
     * <p>rete.cpp:2430:pop_bindings_and_deallocate_list_of_variables
     * 
     * @param vars
     */
    public static void pop_bindings_and_deallocate_list_of_variables(ListHead<Variable> vars)
    {
        for(ListItem<Variable> v = vars.first; v != null; v = v.next)
        {
            v.item.pop_var_binding();
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.Symbol#asVariable()
     */
    @Override
    public Variable asVariable()
    {
        return this;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.symbols.SymbolImpl#importInto(org.jsoar.kernel.symbols.SymbolFactory)
     */
    @Override
    Symbol importInto(SymbolFactory factory)
    {
        throw new IllegalStateException("Cannot import variable symbols");
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.symbols.SymbolImpl#isSameTypeAs(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean isSameTypeAs(SymbolImpl other)
    {
        return other.asVariable() != null;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.symbols.SymbolImpl#getFirstLetter()
     */
    @Override
    public char getFirstLetter()
    {
        return name.charAt(1);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.symbols.SymbolImpl#add_symbol_to_tc(int, java.util.LinkedList, java.util.LinkedList)
     */
    @Override
    public void add_symbol_to_tc(Marker tc, ListHead<IdentifierImpl> id_list, ListHead<Variable> var_list)
    {
        markIfUnmarked(tc, var_list);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.symbols.SymbolImpl#symbol_is_in_tc(int)
     */
    @Override
    public boolean symbol_is_in_tc(Marker tc)
    {
        return tc_number == tc;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return name;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
     */
    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision)
    {
        formatter.format(name);
    }
    
}

/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import java.util.List;

import org.jsoar.kernel.symbols.Symbol;

/**
 * rete.cpp:279
 * 
 * @author ray
 */
public class ReteTest
{
    /* --- types of tests found at beta nodes --- */
    public static final int CONSTANT_RELATIONAL_RETE_TEST = 0x00;
    public static final int VARIABLE_RELATIONAL_RETE_TEST = 0x10;
    public static final int DISJUNCTION_RETE_TEST         = 0x20;
    public static final int ID_IS_GOAL_RETE_TEST          = 0x30;
    public static final int ID_IS_IMPASSE_RETE_TEST       = 0x31;
    
    /* --- for the last two (i.e., the relational tests), we add in one of
    the following, to specifiy the kind of relation --- */
    public static final int RELATIONAL_EQUAL_RETE_TEST            = 0x00;
    public static final int RELATIONAL_NOT_EQUAL_RETE_TEST        = 0x01;
    public static final int RELATIONAL_LESS_RETE_TEST             = 0x02;
    public static final int RELATIONAL_GREATER_RETE_TEST          = 0x03;
    public static final int RELATIONAL_LESS_OR_EQUAL_RETE_TEST    = 0x04;
    public static final int RELATIONAL_GREATER_OR_EQUAL_RETE_TEST = 0x05;
    public static final int RELATIONAL_SAME_TYPE_RETE_TEST        = 0x06;
    
    int right_field_num;          /* field (0, 1, or 2) from wme */
    int type;                     /* test type (ID_IS_GOAL_RETE_TEST, etc.) */
    // TODO union rete_test_data_union {
      VarLocation variable_referent;   /* for relational tests to a variable */
      Symbol constant_referent;        /* for relational tests to a constant */
      List<Symbol> disjunction_list;           /* list of symbols in disjunction test */
    // TODO } data;
    ReteTest next; /* next in list of tests at the node */

    
    public static boolean test_is_constant_relational_test(int x)
    {
      return (((x) & 0xF0)==0x00);
    }

    public static boolean test_is_variable_relational_test(int x)
    {
      return (((x) & 0xF0)==0x10);
    }
    
    public static int kind_of_relational_test(int x)
    {
      return ((x) & 0x0F);
    }

    public static boolean test_is_not_equal_test(int x)
    {
      return (((x)==0x01) || ((x)==0x11));
    }
    
    /**
     * Deallocate_rete_test_list() deallocates a list of rete test structures,
     * removing references to symbols within them.
     *   
     * rete.cpp:2201
     * 
     * @param rt
     */
    static void deallocate_rete_test_list(ReteTest rt)
    {
        ReteTest next_rt = null;

        while (rt != null)
        {
            next_rt = rt.next;

            if (test_is_constant_relational_test(rt.type))
            {
                // symbol_remove_ref (thisAgent, rt->data.constant_referent);
            }
            else if (rt.type == DISJUNCTION_RETE_TEST)
            {
                // deallocate_symbol_list_removing_references (thisAgent,
                // rt->data.disjunction_list);
            }

            // free_with_pool (&thisAgent->rete_test_pool, rt);
            rt = next_rt;
        }
    }
    

}

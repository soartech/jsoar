/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 6, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.symbols.SymbolImpl;

/**
 * Implementations of XXX_rete_test_routine functions in rete.cpp. Separated out
 * for sanity.
 * 
 * <p>TODO: These should probably be polymorphic methods on ReteTest
 * 
 * @author ray
 */
class ReteTestRoutines
{
    /**
     * rete.cpp:4485:disjunction_rete_test_routine
     */
    private static boolean disjunction_rete_test_routine(ReteTest rt, LeftToken left, WmeImpl w)
    {
        final SymbolImpl sym = w.getField(rt.right_field_num);
        return rt.disjunction_list.contains(sym);
    }
        
    /**
     * rete.cpp:4477:id_is_goal_rete_test_routine
     */
    private static boolean id_is_goal_rete_test_routine(ReteTest rt, LeftToken left, WmeImpl w)
    {
        return w.id.isGoal();
    }

    private static boolean id_is_impasse_rete_test_routine(ReteTest rt, LeftToken left, WmeImpl w)
    {
        return false; // removed when attribute impasses were removed.
    }

    /**
     * rete.cpp:4495:constant_equal_rete_test_routine
     */
    private static boolean constant_equal_rete_test_routine(ReteTest rt, LeftToken left, WmeImpl w)
    {
        return equalTest(rt, w, rt.constant_referent);
    }

    /**
     * rete.cpp:4503:constant_not_equal_rete_test_routine
     */
    private static boolean constant_not_equal_rete_test_routine(ReteTest rt, LeftToken left, WmeImpl w)
    {
        return !equalTest(rt, w, rt.constant_referent);
    }


    /**
     * rete.cpp:4546:constant_same_type_rete_test_routine
     */
    private static boolean constant_same_type_rete_test_routine(ReteTest rt, LeftToken left, WmeImpl w)
    {
        return sameTypeTest(rt, w, rt.constant_referent);
    }

    /**
     * rete.cpp:4512:constant_less_rete_test_routine
     */
    private static boolean constant_less_rete_test_routine(ReteTest rt, LeftToken left, WmeImpl w)
    {
        return lessTest(rt, w, rt.constant_referent);
    }
        
    /**
     * rete.cpp:4520:constant_greater_rete_test_routine
     */
    private static boolean constant_greater_rete_test_routine(ReteTest rt, LeftToken left, WmeImpl w)
    {
        return greaterTest(rt, w, rt.constant_referent);
    }

    /**
     * rete.cpp:4528:constant_less_or_equal_rete_test_routine
     */
    private static boolean constant_less_or_equal_rete_test_routine(ReteTest rt, LeftToken left, WmeImpl w)
    {
        return lessEqualTest(rt, w, rt.constant_referent);
    }

    /**
     * rete.cpp:4537:constant_greater_or_equal_rete_test_routine
     */
    private static boolean constant_greater_or_equal_rete_test_routine(ReteTest rt, LeftToken left, WmeImpl w)
    {
        return greaterEqualTest(rt, w, rt.constant_referent);
    }

    /**
     * rete.cpp:4555:variable_equal_rete_test_routine
     */
    private static boolean variable_equal_rete_test_routine(ReteTest rt, LeftToken left, WmeImpl w)
    {
        return equalTest(rt, w, getVariableSymbol(rt, left, w));
    }

    /**
     * rete.cpp:4474:variable_not_equal_rete_test_routine
     */
    private static boolean variable_not_equal_rete_test_routine(ReteTest rt, LeftToken left, WmeImpl w)
    {
        return !equalTest(rt, w, getVariableSymbol(rt, left, w));
    }

    /**
     * rete.cpp:4594:variable_less_rete_test_routine
     */
    private static boolean variable_less_rete_test_routine(ReteTest rt, LeftToken left, WmeImpl w)
    {
        return lessTest(rt, w, getVariableSymbol(rt, left, w));
    }

    /**
     * rete.cpp:4613:variable_greater_rete_test_routine
     */
    private static boolean variable_greater_rete_test_routine(ReteTest rt, LeftToken left, WmeImpl w)
    {
        return greaterTest(rt, w, getVariableSymbol(rt, left, w));
    }

    /**
     * rete.cpp:4632:variable_less_or_equal_rete_test_routine
     */
    private static boolean variable_less_or_equal_rete_test_routine(ReteTest rt, LeftToken left, WmeImpl w)
    {
        return lessEqualTest(rt, w, getVariableSymbol(rt, left, w));
    }

    /**
     * rete.cpp:4652:variable_greater_or_equal_rete_test_routine
     */
    private static boolean variable_greater_or_equal_rete_test_routine(ReteTest rt, LeftToken left, WmeImpl w)
    {
        return greaterEqualTest(rt, w, getVariableSymbol(rt, left, w));
    }

    /**
     * rete.cpp:4672:variable_same_type_rete_test_routine
     */
    private static boolean variable_same_type_rete_test_routine(ReteTest rt, LeftToken left, WmeImpl w)
    {
        return sameTypeTest(rt, w, getVariableSymbol(rt, left, w));
    }
    

    private static boolean equalTest(ReteTest rt, WmeImpl w, SymbolImpl s2)
    {
        final SymbolImpl s1 = w.getField(rt.right_field_num);

        return s1 == s2;
    }
    private static boolean lessTest(ReteTest rt, WmeImpl w, SymbolImpl s2)
    {
        final SymbolImpl s1 = w.getField(rt.right_field_num);

        return s1.numericLess(s2);
    }
    private static boolean lessEqualTest(ReteTest rt, WmeImpl w, SymbolImpl s2)
    {
        final SymbolImpl s1 = w.getField(rt.right_field_num);

        return s1.numericLessOrEqual(s2);
    }
    private static boolean greaterTest(ReteTest rt, WmeImpl w, SymbolImpl s2)
    {
        final SymbolImpl s1 = w.getField(rt.right_field_num);

        return s1.numericGreater(s2);
    }
    private static boolean greaterEqualTest(ReteTest rt, WmeImpl w, SymbolImpl s2)
    {
        final SymbolImpl s1 = w.getField(rt.right_field_num);

        return s1.numericGreaterOrEqual(s2);
    }
    private static boolean sameTypeTest(ReteTest rt, WmeImpl w, SymbolImpl s2)
    {
        final SymbolImpl s1 = w.getField(rt.right_field_num);

        return s1.isSameTypeAs(s2);
    }

    /**
     * Retrieves the symbol for a variable referent in a rete test.
     * 
     * <p>Method extracted from code duplicated in all variable_XXX_rete_test_routine
     * functions in rete.cpp
     * 
     * @param rt The test
     * @param left The token
     * @param w The wme
     * @return The symbol for the test's variable_referent
     */
    private static SymbolImpl getVariableSymbol(ReteTest rt, Token left, WmeImpl w)
    {
        if (rt.variable_referent.levels_up!=0) {
            int i = rt.variable_referent.levels_up - 1;
            while (i!=0) {
              left = left.parent;
              i--;
            }
            w = left.w;
          }
          return w.getField(rt.variable_referent.field_num);
    }
    
    /**
     * <p>rete.cpp:4441:match_left_and_right
     * 
     * @param test
     * @param left
     * @param w
     * @return
     */
    static boolean match_left_and_right(ReteTest test, LeftToken left, WmeImpl w)
    {
        // rete.cpp:4417:rete_test_routines
        switch(test.type)
        {
        case ReteTest.DISJUNCTION: return disjunction_rete_test_routine(test, left, w);
        case ReteTest.ID_IS_GOAL: return id_is_goal_rete_test_routine(test, left, w);            
        case ReteTest.ID_IS_IMPASSE: return id_is_impasse_rete_test_routine(test, left, w);            
        case ReteTest.CONSTANT_RELATIONAL + ReteTest.RELATIONAL_EQUAL: return constant_equal_rete_test_routine(test, left, w);            
        case ReteTest.CONSTANT_RELATIONAL + ReteTest.RELATIONAL_NOT_EQUAL: return constant_not_equal_rete_test_routine(test, left, w);           
        case ReteTest.CONSTANT_RELATIONAL + ReteTest.RELATIONAL_LESS: return constant_less_rete_test_routine(test, left, w);
        case ReteTest.CONSTANT_RELATIONAL + ReteTest.RELATIONAL_GREATER: return constant_greater_rete_test_routine(test, left, w);
        case ReteTest.CONSTANT_RELATIONAL + ReteTest.RELATIONAL_LESS_OR_EQUAL: return constant_less_or_equal_rete_test_routine(test, left, w);
        case ReteTest.CONSTANT_RELATIONAL + ReteTest.RELATIONAL_GREATER_OR_EQUAL: return constant_greater_or_equal_rete_test_routine(test, left, w);
        case ReteTest.CONSTANT_RELATIONAL + ReteTest.RELATIONAL_SAME_TYPE: return constant_same_type_rete_test_routine(test, left, w);
        case ReteTest.VARIABLE_RELATIONAL + ReteTest.RELATIONAL_EQUAL: return variable_equal_rete_test_routine(test, left, w);
        case ReteTest.VARIABLE_RELATIONAL + ReteTest.RELATIONAL_NOT_EQUAL: return variable_not_equal_rete_test_routine(test, left, w);
        case ReteTest.VARIABLE_RELATIONAL + ReteTest.RELATIONAL_LESS: return variable_less_rete_test_routine(test, left, w);
        case ReteTest.VARIABLE_RELATIONAL + ReteTest.RELATIONAL_GREATER: return variable_greater_rete_test_routine(test, left, w);
        case ReteTest.VARIABLE_RELATIONAL + ReteTest.RELATIONAL_LESS_OR_EQUAL: return variable_less_or_equal_rete_test_routine(test, left, w);
        case ReteTest.VARIABLE_RELATIONAL + ReteTest.RELATIONAL_GREATER_OR_EQUAL: return variable_greater_or_equal_rete_test_routine(test, left, w);
        case ReteTest.VARIABLE_RELATIONAL + ReteTest.RELATIONAL_SAME_TYPE: return variable_same_type_rete_test_routine(test, left, w);
        default:
            throw new IllegalStateException("Unknown Rete test type: " + test.type);
        }
    }

}

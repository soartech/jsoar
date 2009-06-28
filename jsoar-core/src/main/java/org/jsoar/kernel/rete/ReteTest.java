/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar.kernel.rete;

import java.util.List;

import org.jsoar.kernel.symbols.SymbolImpl;

/**
 * Implements a simple beta node test. Besides the <code>nex</code> pointer, this
 * object is immutable.
 * 
 * <p>rete.cpp:279
 * <p>rete.cpp:2201:deallocate_rete_test_list - not needed
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
    
    /* --- for the first two (i.e., the relational tests), we add in one of
    the following, to specifiy the kind of relation --- */
    public static final int RELATIONAL_EQUAL_RETE_TEST            = 0x00;
    public static final int RELATIONAL_NOT_EQUAL_RETE_TEST        = 0x01;
    public static final int RELATIONAL_LESS_RETE_TEST             = 0x02;
    public static final int RELATIONAL_GREATER_RETE_TEST          = 0x03;
    public static final int RELATIONAL_LESS_OR_EQUAL_RETE_TEST    = 0x04;
    public static final int RELATIONAL_GREATER_OR_EQUAL_RETE_TEST = 0x05;
    public static final int RELATIONAL_SAME_TYPE_RETE_TEST        = 0x06;
    
    final int type;                     /* test type (ID_IS_GOAL_RETE_TEST, etc.) */
    final int right_field_num;          /* field (0, 1, or 2) from wme */
    // TODO union rete_test_data_union {
      final VarLocation variable_referent;   /* for relational tests to a variable */
      final SymbolImpl constant_referent;        /* for relational tests to a constant */
      final List<SymbolImpl> disjunction_list;           // immutable list of symbols in disjunction test
    // TODO } data;
      
    ReteTest next; /* next in list of tests at the node */
    
    /**
     * Constructs a new disjunction rete test.
     * 
     * @param fieldNum The field number
     * @param disjuncts List of disjuncts. This list is <b>not</b> copied. It 
     *  is assumed to be an immutable list provided by the caller
     * @return New disjunction rete test.
     */
    public static ReteTest createDisjunctionTest(int fieldNum, List<SymbolImpl> disjuncts)
    {
        return new ReteTest(fieldNum, disjuncts);
    }
    
    /**
     * Constructs a new variable test
     * 
     * @param type The type of test
     * @param fieldNum The field number
     * @param variableReferent The variable referent of the test
     * @return The new test
     */
    public static ReteTest createVariableTest(int type, int fieldNum, VarLocation variableReferent)
    {
        return new ReteTest(type, fieldNum, variableReferent);
    }
    
    /**
     * Constructs a new constant test
     * 
     * @param type The type of test
     * @param fieldNum The field number
     * @param constant The constant
     * @return The new test
     */
    public static ReteTest createConstantTest(int type, int fieldNum, SymbolImpl constant)
    {
        return new ReteTest(type, fieldNum, constant);
    }
    
    /**
     * Constructs a new goal id test
     * 
     * @return new test
     */
    public static ReteTest createGoalIdTest()
    {
        return new ReteTest(ID_IS_GOAL_RETE_TEST);
    }
    
    /**
     * Constructs a new impasse id test
     * @return new test
     */
    public static ReteTest createImpasseIdTest()
    {
        return new ReteTest(ID_IS_IMPASSE_RETE_TEST);
    }
    
    private static boolean isRelationType(int r)
    {
        return r == RELATIONAL_EQUAL_RETE_TEST ||
               r == RELATIONAL_NOT_EQUAL_RETE_TEST ||
               r == RELATIONAL_LESS_RETE_TEST ||
               r == RELATIONAL_GREATER_RETE_TEST ||
               r == RELATIONAL_LESS_OR_EQUAL_RETE_TEST ||
               r == RELATIONAL_GREATER_OR_EQUAL_RETE_TEST ||
               r == RELATIONAL_SAME_TYPE_RETE_TEST;
    }
    
    private ReteTest(int type)
    {
        this.type = type;
        this.right_field_num = 0;
        this.variable_referent = null;
        this.disjunction_list = null;
        this.constant_referent = null;
    }
    
    private ReteTest(int fieldNum, List<SymbolImpl> disjunction)
    {
        this.type = DISJUNCTION_RETE_TEST;
        this.right_field_num = fieldNum;
        this.disjunction_list = disjunction;
        this.variable_referent = null;
        this.constant_referent = null;
    }
    
    private ReteTest(int relation, int fieldNum, VarLocation variableReferent)
    {
        assert isRelationType(relation);
        this.type = VARIABLE_RELATIONAL_RETE_TEST + relation;
        this.right_field_num = fieldNum;
        this.disjunction_list = null;
        this.variable_referent = variableReferent;
        this.constant_referent = null;
    }
    
    private ReteTest(int relation, int fieldNum, SymbolImpl constantReferent)
    {
        assert isRelationType(relation);
        this.type = CONSTANT_RELATIONAL_RETE_TEST + relation;
        this.right_field_num = fieldNum;
        this.constant_referent = constantReferent;
        this.disjunction_list = null;
        this.variable_referent = null;
    }
    
    /**
     * <p>rete.cpp:220:test_is_constant_relational_test
     * 
     * @param x
     * @return true if is a constant relational test
     */
    public static boolean test_is_constant_relational_test(int x)
    {
      return (((x) & 0xF0)==0x00);
    }

    /**
     * <p>rete.cpp:225:test_is_variable_relational_test
     * 
     * @param x
     * @return true if x is a variable relational test
     */
    public static boolean test_is_variable_relational_test(int x)
    {
      return (((x) & 0xF0)==0x10);
    }
    
    /**
     * <p>rete.cpp:242:kind_of_relational_test
     * 
     * @param x
     * @return if x is any kind of relational test
     */
    public static int kind_of_relational_test(int x)
    {
      return ((x) & 0x0F);
    }

    /**
     * <p>rete.cpp:247:test_is_not_equal_test
     * 
     * @param x
     * @return trie if x is a not-equal test
     */
    public static boolean test_is_not_equal_test(int x)
    {
      return (((x)==0x01) || ((x)==0x11));
    }


}

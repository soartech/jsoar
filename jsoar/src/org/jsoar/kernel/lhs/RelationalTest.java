/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.List;

import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Variable;

/**
 * @author ray
 */
public class RelationalTest extends ComplexTest
{
    public static final int NOT_EQUAL_TEST = 1;         /* various relational tests */
    public static final int LESS_TEST = 2;
    public static final int GREATER_TEST = 3;
    public static final int LESS_OR_EQUAL_TEST = 4;
    public static final int GREATER_OR_EQUAL_TEST = 5;
    public static final int SAME_TYPE_TEST = 6;
    
    public int type;
    public Symbol referent;
    
    /**
     * 
     * reorder.cpp:320
     * 
     * @param type
     * @return
     */
    public static int reverse_direction_of_relational_test(int type)
    {
        switch (type)
        {
        case RelationalTest.NOT_EQUAL_TEST:        return RelationalTest.NOT_EQUAL_TEST;
        case RelationalTest.LESS_TEST:             return RelationalTest.GREATER_TEST;
        case RelationalTest.GREATER_TEST:          return RelationalTest.LESS_TEST;
        case RelationalTest.LESS_OR_EQUAL_TEST:    return RelationalTest.GREATER_OR_EQUAL_TEST;
        case RelationalTest.GREATER_OR_EQUAL_TEST: return RelationalTest.LESS_OR_EQUAL_TEST;
        case RelationalTest.SAME_TYPE_TEST:        return RelationalTest.SAME_TYPE_TEST;
        default:
            throw new IllegalArgumentException("Unknown RelationalTest type " + type);
        }
    }

    
    /**
     * @param type
     * @param referent
     */
    public RelationalTest(int type, Symbol referent)
    {
        this.type = type;
        this.referent = referent;
    }


    private RelationalTest(RelationalTest other)
    {
        this.type = other.type;
        this.referent = other.referent;
    }


    public RelationalTest asRelationalTest()
    {
        return this;
    }


    /* (non-Javadoc)
     * @see org.jsoar.kernel.Test#copy()
     */
    @Override
    public Test copy()
    {
        return new RelationalTest(this);
    }


    /* (non-Javadoc)
     * @see org.jsoar.kernel.Test#addAllVariables(int, java.util.List)
     */
    @Override
    public void addAllVariables(int tc_number, List<Variable> var_list)
    {
        Variable var = referent.asVariable();
        if(var != null)
        {
            var.markIfUnmarked(tc_number, var_list);
        }
    }

    
}

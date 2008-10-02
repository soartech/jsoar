/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.lhs;

/**
 * @author ray
 */
public class GoalIdTest extends ComplexTest
{
    public static GoalIdTest INSTANCE = new GoalIdTest();
    
    private GoalIdTest()
    {
        
    }

    public GoalIdTest asGoalIdTest()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.Test#copy()
     */
    @Override
    public Test copy()
    {
        return this;
    }

    
}

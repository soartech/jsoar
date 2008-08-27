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
    public GoalIdTest()
    {
        
    }
    private GoalIdTest(GoalIdTest goalIdTest)
    {
        // TODO Auto-generated constructor stub
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
        return new GoalIdTest(this);
    }

    
}

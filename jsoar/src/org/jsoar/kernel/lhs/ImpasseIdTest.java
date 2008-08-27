/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.lhs;

/**
 * @author ray
 */
public class ImpasseIdTest extends ComplexTest
{
    public ImpasseIdTest()
    {
        
    }
    private ImpasseIdTest(ImpasseIdTest impasseIdTest)
    {
        // TODO Auto-generated constructor stub
    }

    public ImpasseIdTest asImpasseIdTest()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.Test#copy()
     */
    @Override
    public Test copy()
    {
        return new ImpasseIdTest(this);
    }

    
}

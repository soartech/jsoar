/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 26, 2008
 */
package org.jsoar.kernel.memory;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author ray
 */
public class PreferenceTypeTest
{

    /**
     * Test method for {@link org.jsoar.kernel.memory.PreferenceType#getDisplayName()}.
     */
    @Test
    public void testGetDisplayName()
    {
        assertEquals("acceptable", PreferenceType.ACCEPTABLE.getDisplayName());
        assertEquals("require", PreferenceType.REQUIRE.getDisplayName());
        assertEquals("reject", PreferenceType.REJECT.getDisplayName());
        assertEquals("prohibit", PreferenceType.PROHIBIT.getDisplayName());
        assertEquals("reconsider", PreferenceType.RECONSIDER.getDisplayName());
        assertEquals("unary indifferent", PreferenceType.UNARY_INDIFFERENT.getDisplayName());
        assertEquals("unary parallel", PreferenceType.UNARY_PARALLEL.getDisplayName());
        assertEquals("best", PreferenceType.BEST.getDisplayName());
        assertEquals("worst", PreferenceType.WORST.getDisplayName());
        assertEquals("binary indifferent", PreferenceType.BINARY_INDIFFERENT.getDisplayName());
        assertEquals("binary parallel", PreferenceType.BINARY_PARALLEL.getDisplayName());
        assertEquals("better", PreferenceType.BETTER.getDisplayName());
        assertEquals("worse", PreferenceType.WORSE.getDisplayName());
        assertEquals("numeric indifferent",  PreferenceType.NUMERIC_INDIFFERENT.getDisplayName());
        
    }

}

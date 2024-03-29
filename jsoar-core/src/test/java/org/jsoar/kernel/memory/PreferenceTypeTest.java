/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 26, 2008
 */
package org.jsoar.kernel.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class PreferenceTypeTest
{
    
    /**
     * Test method for {@link org.jsoar.kernel.memory.PreferenceType#getDisplayName()}.
     */
    @Test
    void testGetDisplayName()
    {
        assertEquals("acceptable", PreferenceType.ACCEPTABLE.getDisplayName());
        assertEquals("require", PreferenceType.REQUIRE.getDisplayName());
        assertEquals("reject", PreferenceType.REJECT.getDisplayName());
        assertEquals("prohibit", PreferenceType.PROHIBIT.getDisplayName());
        assertEquals("unary indifferent", PreferenceType.UNARY_INDIFFERENT.getDisplayName());
        assertEquals("best", PreferenceType.BEST.getDisplayName());
        assertEquals("worst", PreferenceType.WORST.getDisplayName());
        assertEquals("binary indifferent", PreferenceType.BINARY_INDIFFERENT.getDisplayName());
        assertEquals("better", PreferenceType.BETTER.getDisplayName());
        assertEquals("worse", PreferenceType.WORSE.getDisplayName());
        assertEquals("numeric indifferent", PreferenceType.NUMERIC_INDIFFERENT.getDisplayName());
        
    }
    
}

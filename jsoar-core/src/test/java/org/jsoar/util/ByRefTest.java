/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 2, 2008
 */
package org.jsoar.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
public class ByRefTest
{
    
    @Test
    public void testCreateFromNull()
    {
        ByRef<ByRefTest> ref = ByRef.create(null);
        assertNotNull(ref);
        assertNull(ref.value);
    }
}

/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 2, 2008
 */
package org.jsoar.util;

import android.test.AndroidTestCase;


/**
 * @author ray
 */
public class ByRefTest extends AndroidTestCase
{

    public void testCreateFromNull()
    {
        ByRef<ByRefTest> ref = ByRef.create(null);
        assertNotNull(ref);
        assertNull(ref.value);
    }
}

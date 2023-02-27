/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 20, 2009
 */
package org.jsoar.debugger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class DefaultDebuggerProviderTest
{
    
    @Test
    void testThatThisclassIsInTheRightPackageToAvoidBreakingReflection()
    {
        assertEquals(
                org.jsoar.kernel.DefaultDebuggerProvider.DEFAULT_CLASS, DefaultDebuggerProvider.class.getCanonicalName(),
                "The debugger provider's name or package has chanaged, but is referenced reflectively.");
    }
    
}

/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 30, 2008
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class ConcatTest extends JSoarTest
{
    
    /**
     * Test method for {@link org.jsoar.kernel.rhs.functions.Concat#execute(RhsFunctionContext, List)}.
     */
    @Test
    void testExecute() throws Exception
    {
        Concat c = new Concat();
        assertEquals("abc  D  e><>123", c.execute(rhsFuncContext, Symbols.asList(syms, "a", "b", "c  D  e>", "<>", 1, 2, 3)).asString().getValue());
    }
    
}

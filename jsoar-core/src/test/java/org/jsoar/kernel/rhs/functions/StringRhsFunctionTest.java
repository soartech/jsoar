/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 20, 2009
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.jupiter.api.Test;

/**
 * @author ray
 */
class StringRhsFunctionTest extends JSoarTest
{
    @Test
    void testReturnsEmptyStringWhenGivenNoArgs() throws Exception
    {
        final StringRhsFunction f = new StringRhsFunction();
        final Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms));
        assertEquals("", result.asString().getValue());
    }
    
    @Test
    void testConvertsDoubleToString() throws Exception
    {
        final StringRhsFunction f = new StringRhsFunction();
        final Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, 3.14159));
        assertEquals(Double.toString(3.14159), result.asString().getValue());
    }
    
    @Test
    void testConvertsIntToString() throws Exception
    {
        final StringRhsFunction f = new StringRhsFunction();
        final Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, -98));
        assertEquals(Integer.toString(-98), result.asString().getValue());
    }
    
    @Test
    void testConvertsJavaObjectToString() throws Exception
    {
        final StringRhsFunction f = new StringRhsFunction();
        final File o = new File("/path/to/something");
        final Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, o));
        assertEquals(o.toString() + " (" + o.getClass().getName() + ")", result.asString().getValue());
    }
    
    @Test
    void testConvertsIdentifierToString() throws Exception
    {
        final StringRhsFunction f = new StringRhsFunction();
        final Identifier id = syms.createIdentifier('T');
        final Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, id));
        assertEquals(id.toString(), result.asString().getValue());
    }
}

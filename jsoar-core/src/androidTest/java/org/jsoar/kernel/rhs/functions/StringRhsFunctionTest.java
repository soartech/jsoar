/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 20, 2009
 */
package org.jsoar.kernel.rhs.functions;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;

import java.io.File;

/**
 * @author ray
 */
public class StringRhsFunctionTest extends JSoarTest
{
    public void testReturnsEmptyStringWhenGivenNoArgs() throws Exception
    {
        final StringRhsFunction f = new StringRhsFunction();
        final Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms));
        assertEquals("", result.asString().getValue());
    }

    public void testConvertsDoubleToString() throws Exception
    {
        final StringRhsFunction f = new StringRhsFunction();
        final Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, 3.14159));
        assertEquals(Double.toString(3.14159), result.asString().getValue());
    }
    
    public void testConvertsIntToString() throws Exception
    {
        final StringRhsFunction f = new StringRhsFunction();
        final Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, -98));
        assertEquals(Integer.toString(-98), result.asString().getValue());
    }
    
    public void testConvertsJavaObjectToString() throws Exception
    {
        final StringRhsFunction f = new StringRhsFunction();
        final File o = new File("/path/to/something");
        final Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, o));
        assertEquals(o.toString(), result.asString().getValue());
    }
    
    public void testConvertsIdentifierToString() throws Exception
    {
        final StringRhsFunction f = new StringRhsFunction();
        final Identifier id = syms.createIdentifier('T');
        final Symbol result = f.execute(rhsFuncContext, Symbols.asList(syms, id));
        assertEquals(id.toString(), result.asString().getValue());
    }
}

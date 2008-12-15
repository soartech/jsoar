/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 12, 2008
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.Assert.*;

import java.awt.Point;
import java.io.File;
import java.util.List;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.util.ByRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author ray
 */
public class JavaRhsFunctionTest extends JSoarTest
{
    private JavaRhsFunction func;
    
    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        this.func = new JavaRhsFunction();
    }
    
    @After
    public void tearDown()
    {
    }
    
    /**
     * Test method for {@link org.jsoar.kernel.rhs.functions.JavaRhsFunction#execute(org.jsoar.kernel.symbols.SymbolFactory, java.util.List)}.
     */
    @Test
    public void testExecuteStaticFunction() throws Exception
    {
        Symbol result = func.execute(rhsFuncContext, Symbols.asList(syms, "static", "java.lang.Math.max", 35, 45));
        assertEquals(45.0, result.asDouble().getValue(), 0.0001);
    }
    
    @Test
    public void testGetStaticMember() throws Exception
    {
        Symbol result = func.execute(rhsFuncContext, Symbols.asList(syms, "get", "java.lang.Math.PI"));
        assertEquals(Math.PI, result.asDouble().getValue(), 0.0001);
    }

    @Test
    public void testNull() throws Exception
    {
        Symbol result = func.execute(rhsFuncContext, Symbols.asList(syms, "null"));
        assertNull(result.asJava().getValue());
    }
    
    @Test
    public void testGetMember() throws Exception
    {
        Point p = new Point(123, 456);
        Symbol result = func.execute(rhsFuncContext, Symbols.asList(syms, "get", p, "x"));
        assertEquals(p.x, result.asInteger().getValue());
    }
    
    @Test
    public void testSetMember() throws Exception
    {
        Point p = new Point(123, 456);
        Symbol result = func.execute(rhsFuncContext, Symbols.asList(syms, "set", p, "x", 99));
        assertEquals(99, result.asInteger().getValue());
        assertEquals(99, p.x);
    }
    
    @Test
    public void testConstructor() throws Exception
    {
        Symbol result = func.execute(rhsFuncContext, Symbols.asList(syms, "new", "java.io.File", "path"));
        assertNotNull(result);
        File f = (File) result.asJava().getValue();
        assertEquals(new File("path"), f);
    }
    
    @Test
    public void testExecuteMethod() throws Exception
    {
        File file = new File(System.getProperty("user.dir"));
        Symbol result = func.execute(rhsFuncContext, Symbols.asList(syms, "method", file, "getAbsolutePath"));
        assertEquals(file.getAbsolutePath(), result.asString().getValue());
    }
    
    @Test
    public void testExecuteMethodWithComplexReturnType() throws Exception
    {
        File file = new File(System.getProperty("user.dir"));
        Symbol result = func.execute(rhsFuncContext, Symbols.asList(syms, "method", file, "getAbsoluteFile"));
        assertEquals(file, result.asJava().getValue());
    }
    
    @Test(expected=RhsFunctionException.class)
    public void testInvalidMode() throws Exception
    {
        func.execute(rhsFuncContext, Symbols.asList(syms, "asdf"));
    }
    
    @Test
    public void testBasicAgentUsage() throws Exception
    {
        Agent agent = new Agent();
        agent.initialize();
        
        final RhsFunctionHandler oldSucceeded = agent.getRhsFunctions().getHandler("succeeded");
        final ByRef<String> result = ByRef.create(null);
        agent.getRhsFunctions().registerHandler(new StandaloneRhsFunctionHandler("succeeded") {

            @Override
            public Symbol execute(RhsFunctionContext rhsContext, List<Symbol> arguments) throws RhsFunctionException
            {
                result.value = arguments.get(0).toString();
                return oldSucceeded.execute(rhsContext, arguments);
            }});
        
        agent.decider.setWaitsnc(true);
        
        // Create a new StringBuilder and store in (<s> ^builder)
        agent.getProductions().loadProduction(
                "init*builder\n" +
                "(state <s> ^superstate nil)\n" +
        		"-->\n" +
        		"(<s> ^builder (java new |java.lang.StringBuilder| |hello|))");
        // Append to the builder and store the result in (<s> ^result)
        agent.getProductions().loadProduction(
                "append*to*builder\n" +
        		"(state <s> ^superstate nil ^builder <b>)\n" +
        		"-->\n" +
        		"(java method <b> append |-goodbye|)\n" +
        		"(<s> ^result (java method <b> toString))");
        // Get result and pass it to the succeeded RHS function defined above.
        agent.getProductions().loadProduction(
                "report*result\n" +
                "(state <s> ^superstate nil ^result <r>)\n" +
                "-->\n" +
                "(succeeded <r>)");
        
        agent.runFor(1, RunType.DECISIONS);
        
        assertEquals("hello-goodbye", result.value);
    }
}

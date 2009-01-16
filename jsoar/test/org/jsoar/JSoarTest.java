/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.log4j.BasicConfigurator;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.VariableGenerator;
import org.jsoar.kernel.parser.Lexer;
import org.jsoar.kernel.parser.Parser;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.tracing.Printer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * @author ray
 */
public class JSoarTest
{
    protected SymbolFactoryImpl syms;
    protected RhsFunctionContext rhsFuncContext = new RhsFunctionContext() {

        @Override
        public SymbolFactory getSymbols()
        {
            return syms;
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rhs.functions.RhsFunctionContext#addWme(org.jsoar.kernel.symbols.Identifier, org.jsoar.kernel.symbols.Symbol, org.jsoar.kernel.symbols.Symbol)
         */
        @Override
        public void addWme(Identifier id, Symbol attr, Symbol value)
        {
            throw new UnsupportedOperationException("This test implementation of RhsFunctionContext doesn't support addWme");
        }

        /* (non-Javadoc)
         * @see org.jsoar.kernel.rhs.functions.RhsFunctionContext#getProductionBeingFired()
         */
        @Override
        public Production getProductionBeingFired()
        {
            throw new UnsupportedOperationException("This test implementation of RhsFunctionContext doesn't support getProductionBeingFired");
        }
        
    };
    protected VariableGenerator varGen;
    
    @Before
    public void setUp() throws Exception
    {
        this.syms = new SymbolFactoryImpl();
        this.varGen = new VariableGenerator(this.syms);
    }
    
    @BeforeClass
    public static void configureLogging()
    {
        BasicConfigurator.configure();
    }
    
    @AfterClass
    public static void unconfigureLogging()
    {
        BasicConfigurator.resetConfiguration();
    }
    
    protected Parser createParser(String input) throws IOException
    {
        Lexer lexer = new Lexer(Printer.createStdOutPrinter(), new StringReader(input));
        
        Parser parser = new Parser(new VariableGenerator(syms), lexer);
        lexer.getNextLexeme();
        return parser;
    }

    public static void verifyProduction(Agent agent, String name, ProductionType type, String body, boolean internal)
    {
        Production j = agent.getProductions().getProduction(name);
        assertNotNull(j);
        assertEquals(type, j.getType());
        StringWriter writer = new StringWriter();
        j.print(new Printer(writer, false), internal);
        assertEquals(body, writer.toString());
        
    }

}

/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 22, 2008
 */
package org.jsoar;

import java.io.IOException;
import java.io.StringReader;

import org.apache.log4j.BasicConfigurator;
import org.jsoar.kernel.VariableGenerator;
import org.jsoar.kernel.parser.Lexer;
import org.jsoar.kernel.parser.Parser;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * @author ray
 */
public class JSoarTest
{
    protected SymbolFactory syms;
    protected VariableGenerator varGen;
    
    @Before
    public void setUp() throws Exception
    {
        this.syms = new SymbolFactory();
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
        Lexer lexer = new Lexer(new StringReader(input));
        
        Parser parser = new Parser(new VariableGenerator(syms), lexer);
        lexer.getNextLexeme();
        return parser;
    }

}

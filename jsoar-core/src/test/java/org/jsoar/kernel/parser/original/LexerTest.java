/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 14, 2008
 */
package org.jsoar.kernel.parser.original;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import org.jsoar.kernel.parser.PossibleSymbolTypes;
import org.jsoar.kernel.tracing.Printer;
import org.junit.jupiter.api.Test;

public class LexerTest
{
    private Lexer createLexer(String contents) throws IOException
    {
        return new Lexer(Printer.createStdOutPrinter(), new StringReader(contents));
    }
    
    @Test
    public void testLexInteger() throws Exception
    {
        Lexer lexer = createLexer("123456");
        
        lexer.getNextLexeme();
        Lexeme lexeme = lexer.getCurrentLexeme();
        assertNotNull(lexeme);
        
        assertEquals(LexemeType.INTEGER, lexeme.type);
        assertEquals(123456L, lexeme.int_val);
        
    }
    
    @Test
    public void testCanLexLargeInteger() throws Exception
    {
        Lexer lexer = createLexer("12345678910");
        
        lexer.getNextLexeme();
        Lexeme lexeme = lexer.getCurrentLexeme();
        assertNotNull(lexeme);
        
        assertEquals(LexemeType.INTEGER, lexeme.type);
        assertEquals(12345678910L, lexeme.int_val);
        
    }
    
    @Test
    public void testLexIntegerThatStartsWithPlus() throws Exception
    {
        Lexer lexer = createLexer("+123456");
        
        lexer.getNextLexeme();
        Lexeme lexeme = lexer.getCurrentLexeme();
        assertNotNull(lexeme);
        
        assertEquals(LexemeType.INTEGER, lexeme.type);
        assertEquals(+123456L, lexeme.int_val);
        
    }
    
    @Test
    public void testLexFloat() throws Exception
    {
        Lexer lexer = createLexer("123.456");
        
        lexer.getNextLexeme();
        Lexeme lexeme = lexer.getCurrentLexeme();
        assertNotNull(lexeme);
        
        assertEquals(LexemeType.FLOAT, lexeme.type);
        assertEquals(123.456f, lexeme.float_val, 0.001);
        
    }
    
    @Test
    public void testLexString() throws Exception
    {
        Lexer lexer = createLexer("|This is a string|");
        
        lexer.getNextLexeme();
        Lexeme lexeme = lexer.getCurrentLexeme();
        assertNotNull(lexeme);
        
        assertEquals(LexemeType.SYM_CONSTANT, lexeme.type);
        assertEquals("This is a string", lexeme.string);
        
    }
    
    @Test
    public void testIdentifier() throws Exception
    {
        Lexer lexer = createLexer("S123");
        
        lexer.setAllowIds(true);
        lexer.getNextLexeme();
        Lexeme lexeme = lexer.getCurrentLexeme();
        assertNotNull(lexeme);
        
        assertEquals(LexemeType.IDENTIFIER, lexeme.type);
        assertEquals("S123", lexeme.string);
        assertEquals('S', lexeme.id_letter);
        assertEquals(123, lexeme.id_number);
    }
    
    @Test
    public void testLexerCanParseIdentifiersWithLongNumbers() throws Exception
    {
        Lexer lexer = createLexer("S1000000000000"); // > size of integer
        
        lexer.setAllowIds(true);
        lexer.getNextLexeme();
        Lexeme lexeme = lexer.getCurrentLexeme();
        assertNotNull(lexeme);
        
        assertEquals(LexemeType.IDENTIFIER, lexeme.type);
        assertEquals("S1000000000000", lexeme.string);
        assertEquals('S', lexeme.id_letter);
        assertEquals(1000000000000L, lexeme.id_number);
    }
    
    @Test
    public void testThatAnOutOfBoundsIntegerIsStillAPossibleInteger()
    {
        final PossibleSymbolTypes pst = Lexer.determine_possible_symbol_types_for_string("5000000000");
        assertNotNull(pst);
        assertTrue(pst.possible_ic);
    }
    
    @Test
    public void testThatAnOutOfBoundsIntegerCausesALexerError() throws Exception
    {
        Lexer lexer = createLexer("5000000000000000000000000000000000000000000000000000000000000000000");
        lexer.getNextLexeme();
        final Lexeme lexeme = lexer.getCurrentLexeme();
        assertEquals(0L, lexeme.int_val);
    }
}

/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 14, 2008
 */
package org.jsoar.kernel.parser;

import static org.junit.Assert.*;

import java.io.StringReader;

import org.jsoar.kernel.parser.Lexeme;
import org.jsoar.kernel.parser.LexemeTypes;
import org.jsoar.kernel.parser.Lexer;
import org.junit.Test;


public class LexerTest
{
    @Test
    public void testLexInteger() throws Exception
    {
        Lexer lexer = new Lexer(new StringReader("123456"));
        
        lexer.getNextLexeme();
        Lexeme lexeme = lexer.getCurrentLexeme();
        assertNotNull(lexeme);
        
        assertEquals(LexemeTypes.INT_CONSTANT_LEXEME, lexeme.type);
        assertEquals(123456, lexeme.int_val);
        
    }
    
    @Test
    public void testLexFloat() throws Exception
    {
        Lexer lexer = new Lexer(new StringReader("123.456"));
        
        lexer.getNextLexeme();
        Lexeme lexeme = lexer.getCurrentLexeme();
        assertNotNull(lexeme);
        
        assertEquals(LexemeTypes.FLOAT_CONSTANT_LEXEME, lexeme.type);
        assertEquals(123.456f, lexeme.float_val);
        
    }
    
    @Test
    public void testLexString() throws Exception
    {
        Lexer lexer = new Lexer(new StringReader("|This is a string|"));
        
        lexer.getNextLexeme();
        Lexeme lexeme = lexer.getCurrentLexeme();
        assertNotNull(lexeme);
        
        assertEquals(LexemeTypes.SYM_CONSTANT_LEXEME, lexeme.type);
        assertEquals("This is a string", lexeme.string);
        
    }
    
    @Test
    public void testIdentifier() throws Exception
    {
        Lexer lexer = new Lexer(new StringReader("S123"));
        
        lexer.setAllowIds(true);
        lexer.getNextLexeme();
        Lexeme lexeme = lexer.getCurrentLexeme();
        assertNotNull(lexeme);
        
        assertEquals(LexemeTypes.IDENTIFIER_LEXEME, lexeme.type);
        assertEquals("S123", lexeme.string);
        assertEquals('S', lexeme.id_letter);
        assertEquals(123, lexeme.id_number);
        
    }
}

/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 15, 2010
 */
package org.jsoar.util.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.util.Arrays;

import org.jsoar.kernel.SoarException;
import org.junit.Before;
import org.junit.Test;


/**
 * @author ray
 */
public class DefaultInterpreterParserTest
{
    private DefaultInterpreterParser parser;
    
    @Before
    public void setUp()
    {
        parser = new DefaultInterpreterParser();
    }
    
    @Test
    public void testParseCommandTerminatedByEndOfFile() throws Exception
    {
        final PushbackReader reader = reader("    watch {5}");
        assertEquals(Arrays.asList("watch", "5"), parser.parseCommand(reader));
        checkRemainder("", reader);
    }
    
    @Test
    public void testParseCommandTerminatedByCarriageReturn() throws Exception
    {
        final PushbackReader reader = reader("    sp {test\n(state <s> ^superstate nil)\n-->\r\n(write (crlf))\n}\r\nx");
        assertEquals(Arrays.asList("sp", "test\n(state <s> ^superstate nil)\n-->\r\n(write (crlf))\n"), parser.parseCommand(reader));
        checkRemainder("\r\nx", reader);
    }
    
    @Test
    public void testParseCommandTerminatedByLineFeed() throws Exception
    {
        final PushbackReader reader = reader("    watch {5}\nx");
        assertEquals(Arrays.asList("watch", "5"), parser.parseCommand(reader));
        checkRemainder("\nx", reader);
    }
    
    @Test
    public void testParseCommandTerminatedByComment() throws Exception
    {
        final PushbackReader reader = reader("    watch {5} #comment");
        assertEquals(Arrays.asList("watch", "5"), parser.parseCommand(reader));
        checkRemainder("#comment", reader);
    }
    
    @Test
    public void testParseCommandReturnsEmptyListAtEof() throws Exception
    {
        final PushbackReader reader = reader("   #comment    \n");
        assertTrue(parser.parseCommand(reader).isEmpty());
        checkRemainder("", reader);
    }
    
    @Test
    public void testCanSkipWhitespace() throws Exception
    {
        final PushbackReader reader = reader(" \n\t\t    xxx");
        
        parser.skipWhitespace(reader);
        checkRemainder("xxx", reader);
    }
    
    @Test
    public void testCanSkipComments() throws Exception
    {
        final PushbackReader reader = reader(" \n" +
        		"# this is a comment\n" +
        		"   # and another\r\n" +
        		"   # and one more\n" +
        		"   word");
        
        parser.skipWhitespaceAndComments(reader);
        checkRemainder("word", reader);        
    }
    
    @Test
    public void testCanParseAnUnquotedWord() throws Exception
    {
        final PushbackReader reader = reader("   1-word_with*no+quotes  x ");
        
        final String result = parser.parseWord(reader);
        
        assertEquals("1-word_with*no+quotes", result);
        checkRemainder("  x ", reader);
    }
    
    @Test
    public void testCanParseAQuotedWord() throws Exception
    {
        final PushbackReader reader = reader("   \"word with\\n \\\"quotes\"  x ");
        
        final String result = parser.parseWord(reader);
        
        assertEquals("word with\n \"quotes", result);
        checkRemainder("  x ", reader);        
    }

    @Test
    public void testCanParseBracedWord() throws Exception
    {
        final PushbackReader reader = reader("    { words {* words\\n\n \"} words }   end");
        final String result = parser.parseWord(reader);
        assertEquals(" words {* words\\n\n \"} words ", result);
        checkRemainder("   end", reader);        
    }
    
    @Test(expected=SoarException.class)
    public void testErrorOnUnclosedBrace() throws Exception
    {
        final PushbackReader reader = reader("    { words { ");
        parser.parseWord(reader);
        
    }
    
    @Test(expected=SoarException.class)
    public void testErrorOnUnclosedQuote() throws Exception
    {
        final PushbackReader reader = reader("    \" words { ");
        parser.parseWord(reader);
        
    }

    private PushbackReader reader(String input)
    {
        return new PushbackReader(new StringReader(input));
    }
    
    private void checkRemainder(String expected, PushbackReader reader) throws IOException
    {
        final StringBuilder b = new StringBuilder();
        int c = 0;
        while((c = reader.read()) != -1)
        {
            b.append((char) c);
        }
        assertEquals(expected, b.toString());
    }
}

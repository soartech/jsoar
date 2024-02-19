package org.jsoar.debugger;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * This is to test that tokens are correctly parsed when the trace is clicked on
 * The test names follow this naming scheme:
 * parse[Id|Attr|Val -- the kind of thing that was clicked on]_[Spaces|NoSpaces in previous token]_[Spaces|NoSpaces in current token]_[Spaces|NoSpaces in next token]
 */
class ParseSelectedTextTest
{
    //
    // attr tests -- click happened on attr
    //
    
    @Test
    void parseAttr_NoSpaces_NoSpaces_NoSpaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" nospaces ^nospaces nospaces ", 16);
        assertEquals("nospaces", parseSelectedText.m_Tokens[0]);
        assertEquals("^nospaces", parseSelectedText.m_Tokens[1]);
        assertEquals("nospaces", parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseAttr_NoSpaces_NoSpaces_Spaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" nospaces ^nospaces |s p a c e s| ", 16);
        assertEquals("nospaces", parseSelectedText.m_Tokens[0]);
        assertEquals("^nospaces", parseSelectedText.m_Tokens[1]);
        assertEquals("|s p a c e s|", parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseAttr_NoSpaces_Spaces_NoSpaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" nospaces ^|s p a c e s| nospaces ", 16);
        assertEquals("nospaces", parseSelectedText.m_Tokens[0]);
        assertEquals("^|s p a c e s|", parseSelectedText.m_Tokens[1]);
        assertEquals("nospaces", parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseAttr_NoSpaces_Spaces_Spaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" nospaces ^|s p a c e s| |s p a c e s| ", 16);
        assertEquals("nospaces", parseSelectedText.m_Tokens[0]);
        assertEquals("^|s p a c e s|", parseSelectedText.m_Tokens[1]);
        assertEquals("|s p a c e s|", parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseAttr_Spaces_NoSpaces_NoSpaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" |s p a c e s| ^nospaces nospaces ", 20);
        assertEquals("|s p a c e s|", parseSelectedText.m_Tokens[0]);
        assertEquals("^nospaces", parseSelectedText.m_Tokens[1]);
        assertEquals("nospaces", parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseAttr_Spaces_NoSpaces_Spaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" |s p a c e s| ^nospaces |s p a c e s| ", 20);
        assertEquals("|s p a c e s|", parseSelectedText.m_Tokens[0]);
        assertEquals("^nospaces", parseSelectedText.m_Tokens[1]);
        assertEquals("|s p a c e s|", parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseAttr_Spaces_Spaces_NoSpaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" |s p a c e s| ^|s p a c e s| nospaces ", 20);
        assertEquals("|s p a c e s|", parseSelectedText.m_Tokens[0]);
        assertEquals("^|s p a c e s|", parseSelectedText.m_Tokens[1]);
        assertEquals("nospaces", parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseAttr_Spaces_Spaces_Spaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText("|s p a c e s| ^|s p a c e s| |s p a c e s|", 20);
        assertEquals("|s p a c e s|", parseSelectedText.m_Tokens[0]);
        assertEquals("^|s p a c e s|", parseSelectedText.m_Tokens[1]);
        assertEquals("|s p a c e s|", parseSelectedText.m_Tokens[2]);
    }
    
    //
    // value tests -- click happened on a value
    //
    
    @Test
    void parseVal_NoSpaces_NoSpaces_NoSpaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" ^nospaces nospaces ^nospaces ", 16);
        assertEquals("^nospaces", parseSelectedText.m_Tokens[0]);
        assertEquals("nospaces", parseSelectedText.m_Tokens[1]);
        assertEquals("^nospaces", parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseVal_NoSpaces_NoSpaces_Spaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" ^nospaces nospaces ^|s p a c e s| ", 16);
        assertEquals("^nospaces", parseSelectedText.m_Tokens[0]);
        assertEquals("nospaces", parseSelectedText.m_Tokens[1]);
        assertEquals("^|s p a c e s|", parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseVal_NoSpaces_Spaces_NoSpaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText("^nospaces |s p a c e s| ^nospaces", 16);
        assertEquals("^nospaces", parseSelectedText.m_Tokens[0]);
        assertEquals("|s p a c e s|", parseSelectedText.m_Tokens[1]);
        assertEquals("^nospaces", parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseVal_NoSpaces_Spaces_Spaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" ^nospaces |s p a c e s| ^|s p a c e s| ", 16);
        assertEquals("^nospaces", parseSelectedText.m_Tokens[0]);
        assertEquals("|s p a c e s|", parseSelectedText.m_Tokens[1]);
        assertEquals("^|s p a c e s|", parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseVal_Spaces_NoSpaces_NoSpaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" ^|s p a c e s| nospaces ^nospaces ", 20);
        assertEquals("^|s p a c e s|", parseSelectedText.m_Tokens[0]);
        assertEquals("nospaces", parseSelectedText.m_Tokens[1]);
        assertEquals("^nospaces", parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseVal_Spaces_NoSpaces_Spaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" ^|s p a c e s| nospaces ^|s p a c e s| ", 20);
        assertEquals("^|s p a c e s|", parseSelectedText.m_Tokens[0]);
        assertEquals("nospaces", parseSelectedText.m_Tokens[1]);
        assertEquals("^|s p a c e s|", parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseVal_Spaces_Spaces_NoSpaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" ^|s p a c e s| |s p a c e s| ^nospaces ", 20);
        assertEquals("^|s p a c e s|", parseSelectedText.m_Tokens[0]);
        assertEquals("|s p a c e s|", parseSelectedText.m_Tokens[1]);
        assertEquals("^nospaces", parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseVal_Spaces_Spaces_Spaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" ^|s p a c e s| |s p a c e s| ^|s p a c e s| ", 20);
        assertEquals("^|s p a c e s|", parseSelectedText.m_Tokens[0]);
        assertEquals("|s p a c e s|", parseSelectedText.m_Tokens[1]);
        assertEquals("^|s p a c e s|", parseSelectedText.m_Tokens[2]);
    }
    
    //
    // id tests -- click happened on an id
    // there are fewer of these because can't have spaces in ids
    //
    
    @Test
    void parseId_NoSpaces_NoSpaces_NoSpaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" ^nospaces X1111111 ^nospaces ", 16);
        assertEquals("^nospaces", parseSelectedText.m_Tokens[0]);
        assertEquals("X1111111", parseSelectedText.m_Tokens[1]);
        assertEquals("^nospaces", parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseId_NoSpaces_NoSpaces_Spaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" ^nospaces X1111111 ^|s p a c e s| ", 16);
        assertEquals("^nospaces", parseSelectedText.m_Tokens[0]);
        assertEquals("X1111111", parseSelectedText.m_Tokens[1]);
        assertEquals("^|s p a c e s|", parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseId_Spaces_NoSpaces_NoSpaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" ^|s p a c e s| X1111111 ^nospaces ", 20);
        assertEquals("^|s p a c e s|", parseSelectedText.m_Tokens[0]);
        assertEquals("X1111111", parseSelectedText.m_Tokens[1]);
        assertEquals("^nospaces", parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseId_Spaces_NoSpaces_Spaces() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" ^|s p a c e s| X1111111 ^|s p a c e s| ", 20);
        assertEquals("^|s p a c e s|", parseSelectedText.m_Tokens[0]);
        assertEquals("X1111111", parseSelectedText.m_Tokens[1]);
        assertEquals("^|s p a c e s|", parseSelectedText.m_Tokens[2]);
    }
    
    
    //
    // edge case tests
    //
    
    @Test
    void parseFirstToken() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" (S1 ^superstate nil ", 2);
        assertEquals(null, parseSelectedText.m_Tokens[0]);
        assertEquals("S1", parseSelectedText.m_Tokens[1]);
        assertEquals("^superstate", parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseLastToken() {
        ParseSelectedText parseSelectedText = new ParseSelectedText(" ^superstate nil)", 13);
        assertEquals("^superstate", parseSelectedText.m_Tokens[0]);
        assertEquals("nil", parseSelectedText.m_Tokens[1]);
        assertEquals(null, parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseNoTokens() {
        ParseSelectedText parseSelectedText = new ParseSelectedText("       ", 5);
        assertEquals(null, parseSelectedText.m_Tokens[0]);
        assertEquals(null, parseSelectedText.m_Tokens[1]);
        assertEquals(null, parseSelectedText.m_Tokens[2]);
    }
    
    @Test
    void parseSelectedEnd() {
        ParseSelectedText parseSelectedText = new ParseSelectedText("(S1 ^superstate nil)", 19);
        assertEquals(null, parseSelectedText.m_Tokens[0]);
        assertEquals(null, parseSelectedText.m_Tokens[1]);
        assertEquals(null, parseSelectedText.m_Tokens[2]);
    }
}

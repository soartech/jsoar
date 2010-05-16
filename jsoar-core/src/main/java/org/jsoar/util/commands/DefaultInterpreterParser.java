/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 15, 2010
 */
package org.jsoar.util.commands;

import java.io.IOException;
import java.io.PushbackReader;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.SoarException;

/**
 * @author ray
 */
public class DefaultInterpreterParser
{
    public List<String> parseCommand(PushbackReader reader) throws IOException, SoarException
    {
        final List<String> result = new ArrayList<String>();
        skipWhitespaceAndComments(reader);
        String word = parseWord(reader);
        while(word != null)
        {
            result.add(word);
            if(atEndOfCommand(reader))
            {
                break;
            }
            skipWhitespaceAndComments(reader);
            word = parseWord(reader);
        }
        return result;
    }
    
    private boolean atEndOfCommand(PushbackReader reader) throws IOException
    {
        int c = 0;
        while((c = reader.read()) != -1) 
        {
            switch(c)
            {
            // EOL or comment marks end of command
            case '\r':
            case '\n':
            case '#': 
                reader.unread(c); 
                return true;
            // Skip whitespace
            case ' ':
            case '\t':
            case '\f':
                break;
            // Anything else means there's more to the command
            default: 
                reader.unread(c); 
                return false;
            }
        }
        // EOF marks end of command
        return true;
    }
    
    public void skipWhitespace(PushbackReader reader) throws IOException
    {
        int c = 0;
        while((c = reader.read()) != -1 && Character.isWhitespace(c)) {}
        if(c != -1)
        {
            reader.unread(c);
        }
    }
    
    private void skipToEndOfLine(PushbackReader reader) throws IOException
    {
        int c = 0;
        while((c = reader.read()) != -1 && c != '\n' && c != '\r') {}
        if(c != -1 && c != '\n' && c != '\r')
        {
            reader.unread(c);
        }
    }
    
    public void skipWhitespaceAndComments(PushbackReader reader) throws IOException
    {
        skipWhitespace(reader);
        int c = reader.read();
        if(c == -1) return;
        if(c == '#')
        {
            skipToEndOfLine(reader);
            skipWhitespaceAndComments(reader);
        }
        else
        {
            reader.unread(c);
        }
    }

    public String parseWord(PushbackReader reader) throws SoarException, IOException
    {
        skipWhitespace(reader);
        final StringBuilder result = new StringBuilder();
        int c = reader.read();
        if(c == -1)
        {
            return null;
        }
        if(c == '"')
        {
            while((c = reader.read()) != -1 && c != '"') 
            {
                switch(c)
                {
                case '\\':
                    c = parseEscapeSequence(reader);
                    if(c == -1)
                    {
                        throw new SoarException("Unexpected end of file");
                    }
                default:
                    result.append((char) c);
                }
            }
            if(c == -1)
            {
                throw new SoarException("Unexpected end of input. Unmatched quote.");
            }
        }
        else if(c == '{')
        {
            int braces = 1;
            while(braces > 0 && (c = reader.read()) != -1) 
            {
                switch(c)
                {
                case '}':
                    braces--;
                    if(braces > 0)
                    {
                        result.append((char) c);
                    }
                    break;
                case '{':
                    braces++;
                default:
                    result.append((char) c);
                }
            }
            if(braces > 0)
            {
                throw new SoarException("Unexpected end of input. Unmatched opening brace");
            }
        }
        else
        {
            result.append((char) c);
            while((c = reader.read()) != -1 && !Character.isWhitespace(c)) 
            {
                result.append((char) c);
            }
            if(c != -1)
            {
                reader.unread(c);
            }
        }
        return result.toString();
    }
    
    private int parseEscapeSequence(PushbackReader reader) throws IOException
    {
        int c = reader.read();
        switch(c)
        {
        case 'n': c = '\n'; break;
        case 'r': c = '\r'; break;
        case 't': c = '\t'; break;
        case 'f': c = '\f'; break;
        case 'b': c = '\b'; break;
        }
        return c;
    }

}

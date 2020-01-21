/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 15, 2010
 */
package org.jsoar.util.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.exceptions.SoarParserException;
import org.jsoar.util.DefaultSourceLocation;
import org.jsoar.util.SourceLocation;

/**
 * @author ray
 */
public class DefaultInterpreterParser
{
    private int startOfCommand;
    private int lineOfCommand;
    
    public ParsedCommand parseCommand(ParserBuffer reader) throws IOException, SoarException
    {
        startOfCommand = -1;
        lineOfCommand = -1;
        
        final List<String> result = new ArrayList<String>();
        skipWhitespaceAndComments(reader);
        
        if(!atEndOfCommand(reader))
        {
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
        }
        final SourceLocation loc = DefaultSourceLocation.newBuilder().
                                        file(reader.getFile()).
                                        offset(startOfCommand).
                                        length(reader.getCurrentOffset() - startOfCommand).
                                        line(lineOfCommand).
                                        build();
        return new ParsedCommand(loc, result);
    }
    
    private boolean atEndOfCommand(ParserBuffer reader) throws IOException
    {
        int c = 0;
        while((c = read(reader)) != -1) 
        {
            switch(c)
            {
            // EOL, comment, or semicolon marks end of command
            case '\r':
            case '\n':
            case '#': 
                unread(reader, c); 
                return true;
            // Consume in the case of a semicolon: it's lexically a part of the command we just processed.
            // (E.g., consider the case of the empty command ";": we would expect that parsing this would
            // leave nothing on the buffer.)
            case ';':
                return true;
                
            // Skip whitespace
            case ' ':
            case '\t':
            case '\f':
                break;
                
            // Anything else means there's more to the command
            default: 
                unread(reader, c); 
                return false;
            }
        }
        // EOF marks end of command
        return true;
    }
    
    public void skipWhitespace(ParserBuffer reader) throws IOException
    {
        int c = 0;
        while((c = read(reader)) != -1 && Character.isWhitespace(c)) {}
        if(c != -1)
        {
            unread(reader, c);
        }
    }
    
    private void skipToEndOfLine(ParserBuffer reader) throws IOException
    {
        int c = 0;
        while((c = read(reader)) != -1 && c != '\n' && c != '\r') {}
        if(c != -1 && c != '\n' && c != '\r')
        {
            unread(reader, c);
        }
    }
    
    public void skipWhitespaceAndComments(ParserBuffer reader) throws IOException
    {
        skipWhitespace(reader);
        int c = read(reader);
        if(c == -1) return;
        if(c == '#')
        {
            skipToEndOfLine(reader);
            skipWhitespaceAndComments(reader);
        }
        else
        {
            unread(reader, c);
        }
    }

    public String parseWord(ParserBuffer reader) throws SoarException, IOException
    {
        skipWhitespace(reader);
        final StringBuilder result = new StringBuilder();
        int c = read(reader);
        if(c == -1)
        {
            return null;
        }
        
        if(startOfCommand == -1)
        {
            startOfCommand = reader.getCurrentOffset();
            lineOfCommand = reader.getCurrentLine();
        }
        
        if(c == '"')
        {
            while((c = read(reader)) != -1 && c != '"') 
            {
                switch(c)
                {
                case '\\':
                    c = parseEscapeSequence(reader);
                    if(c == -1)
                    {
                        throw new SoarParserException("Unexpected end of file", startOfCommand);
                        //throw error(reader, "Unexpected end of file");
                    }
                default:
                    result.append((char) c);
                }
            }
            if(c == -1)
            {
                throw new SoarParserException("Unexpected end of input. Unmatched quote.", startOfCommand);
                //throw error(reader, "Unexpected end of input. Unmatched quote.");
            }
        }
        else if(c == '{')
        {
            int braces = 1;
            while(braces > 0 && (c = read(reader)) != -1) 
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
                throw new SoarParserException("Unexpected end of input. Unmatched opening brace", startOfCommand);
                //throw error(reader, "Unexpected end of input. Unmatched opening brace");
            }
        }
        else
        {
            result.append((char) c);
            while((c = read(reader)) != -1 && !Character.isWhitespace(c)) 
            {
                result.append((char) c);
            }
            if(c != -1)
            {
                unread(reader, c);
            }
        }
        return result.toString();
    }
    
    private int parseEscapeSequence(ParserBuffer reader) throws IOException
    {
        int c = read(reader);
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
    
    private int read(ParserBuffer reader) throws IOException
    {
        return reader.read();
    }
    
    private void unread(ParserBuffer reader, int c) throws IOException
    {
        reader.unread(c);
    }

}

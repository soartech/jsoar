/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Jan 17, 2009
 */
package org.jsoar.kernel.parser;

import java.io.Reader;

import org.jsoar.kernel.Production;

/**
 * Interface for a Soar production rule parser
 * 
 * @author ray
 */
public interface Parser
{
    /**
     * Parses a single production. What this means in terms of input will vary
     * with the parser, but typically it means to parse the name, documentation,
     * options, conditions and actions of a production.
     * 
     * <p>For example, in the "original" parser, this method will parse the
     * body of an <b>sp</b> command, i.e. sp { body }. The "sp" and enclosing
     * braces are omitted.
     * 
     * <p>This method may safely be called multiple times but should not be
     * assumed to be thread-safe.
     * 
     * @param context the context for the parser
     * @param reader the reader to read data from
     * @return a parsed production
     * @throws ParserException on a parsing error
     * @throws IllegalStateException if a required resource cannot be obtained
     *      from the parser context.
     */
    Production parseProduction(ParserContext context, Reader reader) throws ParserException;
    
}

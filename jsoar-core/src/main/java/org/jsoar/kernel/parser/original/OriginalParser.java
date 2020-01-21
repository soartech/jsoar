/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Jan 17, 2009
 */
package org.jsoar.kernel.parser.original;

import java.io.IOException;
import java.io.Reader;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.parser.Parser;
import org.jsoar.kernel.parser.ParserContext;
import org.jsoar.kernel.parser.ParserException;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.functions.RhsFunctionManager;
import org.jsoar.kernel.symbols.LongTermIdentifierSource;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.util.DefaultSourceLocation;
import org.jsoar.util.SourceLocation;
import org.jsoar.util.adaptables.AbstractAdaptable;
import org.jsoar.util.adaptables.Adaptables;

/**
 * @author ray
 */
public class OriginalParser extends AbstractAdaptable implements Parser
{

    /* (non-Javadoc)
     * @see org.jsoar.kernel.parser.Parser#parseProduction(org.jsoar.kernel.parser.ParserContext, java.io.Reader)
     */
    @Override
    public Production parseProduction(ParserContext context, Reader reader) throws ParserException
    {
        final Printer printer = require(context, Printer.class);
        final SymbolFactoryImpl syms = require(context, SymbolFactoryImpl.class);
        final RhsFunctionManager rhsFunctions = require(context, RhsFunctionManager.class);
        final SourceLocation source = Adaptables.adapt(context, SourceLocation.class);
        
        try
        {
            Lexer lexer = new Lexer(printer, reader);
            OriginalParserImpl parser = new OriginalParserImpl(syms.getVariableGenerator(), lexer);
            parser.setRhsFunctions(rhsFunctions);
            parser.setSourceLocation(source != null ? source : DefaultSourceLocation.UNKNOWN);
            
            lexer.getNextLexeme();
            return parser.parseProduction();
        }
        catch (IOException e)
        {
            throw new ParserException(e);
        }
    }
    
    public Condition parseLeftHandSide(ParserContext context, Reader reader) throws ParserException
    {
        final Printer printer = require(context, Printer.class);
        final SymbolFactoryImpl syms = require(context, SymbolFactoryImpl.class);
        final RhsFunctionManager rhsFunctions = require(context, RhsFunctionManager.class);
        final SourceLocation source = Adaptables.adapt(context, SourceLocation.class);
        
        try
        {
            Lexer lexer = new Lexer(printer, reader);
            OriginalParserImpl parser = new OriginalParserImpl(syms.getVariableGenerator(), lexer);
            parser.setRhsFunctions(rhsFunctions);
            parser.setSourceLocation(source != null ? source : DefaultSourceLocation.UNKNOWN);
            
            lexer.getNextLexeme();
            return parser.parse_lhs();
        }
        catch (IOException e)
        {
            throw new ParserException(e);
        }
        
    }
    
    public Action parseRightHandSide(ParserContext context, Reader reader) throws ParserException
    {
        final Printer printer = require(context, Printer.class);
        final SymbolFactoryImpl syms = require(context, SymbolFactoryImpl.class);
        final RhsFunctionManager rhsFunctions = require(context, RhsFunctionManager.class);
        final SourceLocation source = Adaptables.adapt(context, SourceLocation.class);
        final LongTermIdentifierSource ltis = Adaptables.adapt(context, LongTermIdentifierSource.class);
        
        try
        {
            Lexer lexer = new Lexer(printer, reader);
            OriginalParserImpl parser = new OriginalParserImpl(syms.getVariableGenerator(), lexer);
            parser.setRhsFunctions(rhsFunctions);
            parser.setSourceLocation(source != null ? source : DefaultSourceLocation.UNKNOWN);
            parser.setLongTermIdSource(ltis);
            
            lexer.getNextLexeme();
            return parser.parse_rhs();
        }
        catch (IOException e)
        {
            throw new ParserException(e);
        }
        
    }    
    private <T> T require(ParserContext context, Class<T> klass)
    {
        return Adaptables.require(getClass(), context, klass);
    } 
}

/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 14, 2008
 */
package org.jsoar.kernel.parser;

import java.io.IOException;

/**
 * @author ray
 */
class BasicLexerRoutine implements LexerRoutine
{
    private LexemeType type;

    
    /**
     * @param type
     */
    public BasicLexerRoutine(LexemeType type)
    {
        this.type = type;
    }


    /* (non-Javadoc)
     * @see org.jsoar.kernel.LexerRoutine#lex(org.jsoar.kernel.Lexer)
     */
    @Override
    public void lex(Lexer lexer) throws IOException
    {
        lexer.store_and_advance();
        lexer.setLexemeType(type);
    }

}

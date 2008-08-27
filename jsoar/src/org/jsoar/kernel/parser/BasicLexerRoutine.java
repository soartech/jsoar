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
public class BasicLexerRoutine implements LexerRoutine
{
    private int type;

    
    /**
     * @param type
     */
    public BasicLexerRoutine(int type)
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
        lexer.finish();
        lexer.setLexemeType(type);
    }

}

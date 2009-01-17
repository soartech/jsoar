/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 14, 2008
 */
package org.jsoar.kernel.parser.original;

import java.io.IOException;

/**
 * @author ray
 */
interface LexerRoutine
{
    void lex(Lexer lexer) throws IOException;
}

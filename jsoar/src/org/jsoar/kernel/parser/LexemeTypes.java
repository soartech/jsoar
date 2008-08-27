/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 14, 2008
 */
package org.jsoar.kernel.parser;

/**
 * @author ray
 */
public interface LexemeTypes
{
    public static final int EOF_LEXEME = 0;
    public static final int VARIABLE_LEXEME = 1;
    public static final int INT_CONSTANT_LEXEME = 2;
    public static final int FLOAT_CONSTANT_LEXEME = 3;
    public static final int IDENTIFIER_LEXEME = 4;
    public static final int SYM_CONSTANT_LEXEME = 5;
    public static final int QUOTED_STRING_LEXEME = 6;
    public static final int R_PAREN_LEXEME = 7;
    public static final int AT_LEXEME = 8;
    public static final int TILDE_LEXEME = 9;
    public static final int UP_ARROW_LEXEME = 10;
    public static final int L_BRACE_LEXEME = 11;
    public static final int R_BRACE_LEXEME = 12;
    public static final int EXCLAMATION_POINT_LEXEME = 13;
    public static final int COMMA_LEXEME = 14;
    public static final int GREATER_LEXEME = 15;
    public static final int GREATER_GREATER_LEXEME = 16;
    public static final int GREATER_EQUAL_LEXEME = 17;
    public static final int L_PAREN_LEXEME = 18;
    public static final int EQUAL_LEXEME = 19;
    public static final int NOT_EQUAL_LEXEME = 20;
    public static final int LESS_LEXEME = 21;
    public static final int LESS_EQUAL_LEXEME = 22;
    public static final int LESS_LESS_LEXEME = 23;
    public static final int LESS_EQUAL_GREATER_LEXEME = 24;
    public static final int PERIOD_LEXEME = 25;
    public static final int PLUS_LEXEME = 26;
    public static final int MINUS_LEXEME = 27;
    public static final int RIGHT_ARROW_LEXEME = 28;
    public static final int DOLLAR_STRING_LEXEME = 29;
    public static final int AMPERSAND_LEXEME = 30;
}

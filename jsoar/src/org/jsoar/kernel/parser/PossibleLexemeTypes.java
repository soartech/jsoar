/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 13, 2008
 */
package org.jsoar.kernel.parser;

public class PossibleLexemeTypes
{
    /**
     * <p>lexer.h:95:LENGTH_OF_LONGEST_SPECIAL_LEXEME
     */
    private static final int LENGTH_OF_LONGEST_SPECIAL_LEXEME = 3;
    
    public boolean possible_id, possible_var, possible_sc, possible_ic,
            possible_fc, rereadable;
    
    /**
     * This is a utility routine which figures out what kind(s) of symbol a
     * given string could represent. At entry: s, length_of_s represent the
     * string. At exit: possible_xxx is set to TRUE/FALSE to indicate whether
     * the given string could represent that kind of symbol; rereadable is set
     * to TRUE indicating whether the lexer would read the given string as a
     * symbol with exactly the same name (as opposed to treating it as a special
     * lexeme like "+", changing upper to lower case, etc.
     * 
     * <p>lexer.cpp:1225:determine_possible_symbol_types_for_string
     */
    public static PossibleLexemeTypes determine_possible_symbol_types_for_string(String s)
    {

        PossibleLexemeTypes p = new PossibleLexemeTypes();

        if (s.length() == 0)
        {
            return p;
        }

        /* --- check if it's an integer or floating point number --- */
        if (Lexer.number_starters[s.charAt(0)])
        {
            try
            {
                Integer.parseInt(s);
                p.possible_ic = true;
            }
            catch (NumberFormatException e)
            {
            }
            try
            {
                Float.parseFloat(s);
                p.possible_fc = s.indexOf('.') != -1;
            }
            catch (NumberFormatException e)
            {
            }
        }

        /* --- make sure it's entirely constituent characters --- */
        for (int i = 0; i < s.length(); ++i)
        {
            if (!Lexer.constituent_char[s.charAt(i)])
            {
                return p;
            }
        }

        /* --- any string of constituents could be a sym constant --- */
        p.possible_sc = true;

        /* --- check whether it's a variable --- */
        if ((s.charAt(0) == '<') && (s.charAt(s.length() - 1) == '>'))
        {
            p.possible_var = true;
        }
        
        /* --- check for rereadability --- */
        boolean rereadability_questionable = false;
        boolean rereadability_dead = false;
        for (int i = 0; i < s.length(); i++)
        {
            char ch = s.charAt(i);
            if (Character.isLowerCase(ch) || Character.isDigit(ch))
                continue; /* these guys are fine */
            if (Character.isUpperCase(ch))
            {
                rereadability_dead = true;
                break;
            }
            rereadability_questionable = true;
        }
        if (!rereadability_dead)
        {
            if ((!rereadability_questionable) || (s.length() >= LENGTH_OF_LONGEST_SPECIAL_LEXEME)
                    || ((s.length() == 1) && (s.charAt(0) == '*')))
                p.rereadable = true;
        }

        /* --- check if it's an identifier --- */
        if (Character.isLetter(s.charAt(0)))
        {
            /* --- is the rest of the string an integer? --- */
            int i = 1;
            while (i < s.length() && Character.isDigit(s.charAt(i)))
            {
                ++i;
            }
            p.possible_id = i == s.length();
        }
        return p;
    }

}
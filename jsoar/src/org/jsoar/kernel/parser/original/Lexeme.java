/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 14, 2008
 */
package org.jsoar.kernel.parser.original;

/**
 * @author ray
 */
public class Lexeme
{
    public String string = "";
    public LexemeType type = null;
    public int int_val;
    public double float_val;
    public char id_letter;
    public int id_number;
    
    public void append(char c)
    {
        string += c;
    }
    
    public char at(int index)
    {
        return string.charAt(index);
    }
    
    public int length()
    {
        return string.length();
    }
    
    
    public String toString()
    {
        return string;
    }
}

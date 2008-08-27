/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 14, 2008
 */
package org.jsoar.kernel.parser;

/**
 * @author ray
 */
public class Lexeme
{
    public String string = "";
    public int type = 0;
    public int int_val;
    public float float_val;
    public char id_letter;
    public int id_number;
    
    public void append(char c)
    {
        string += c;
    }
    
    
    public String toString()
    {
        return string;
    }
}

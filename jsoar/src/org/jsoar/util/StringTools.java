/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 22, 2008
 */
package org.jsoar.util;

/**
 * @author ray
 */
public class StringTools
{
    /**
     * String_to_escaped_string() takes a string and a first/last char, and
     * produces an "escaped string" representation of the string; i.e., a string
     * that uses '\' escapes to include special characters. For example, input
     * 'ab"c' with first/last character '"' yields '"ab\"c"'. This is used for
     * printing quoted strings and for printing symbols using |vbar| notation.
     * 
     * print.cpp:181:string_to_escaped_string
     * 
     * @param s
     * @param first_and_last_char
     * @return
     */
    public static String string_to_escaped_string(String s, char first_and_last_char)
    {
        StringBuilder dest = new StringBuilder();
        dest.append(first_and_last_char);
        for (int i = 0; i < s.length(); ++i)
        {
            char c = s.charAt(i);
            if (c == first_and_last_char || c == '\\')
            {
                dest.append('\\');
            }
            dest.append(c);
        }
        dest.append(first_and_last_char);
        return dest.toString();
    }
}

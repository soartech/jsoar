/**
 * 
 */
package org.jsoar.util;

import com.google.common.base.Strings;

/**
 * @author ALT
 *
 */
public class PrintHelper
{
    public static String generateItem(String prefixString, Object printObject, int column_width)
    {
        int left_width, right_width, middle_width;
        String sep_string = null;
        String printString = printObject.toString();
        
        left_width = prefixString.length();
        right_width = printString.length();
        middle_width = column_width - left_width - right_width;
        
        if (middle_width < 0)
            middle_width = 1;
        
        sep_string = Strings.repeat(" ", middle_width);
        
        return prefixString + sep_string + printString + "\n";
    }

    public static String generateHeader(String headerString, int column_width)
    {
        int left_width, right_width, header_width;
        String left_string, right_string, sep_string = null;
        
        header_width = headerString.length() + 2;
        left_width = (column_width - header_width) / 2;
        right_width = column_width - left_width - header_width;
        left_string = Strings.repeat(" ", left_width);
        right_string = Strings.repeat(" ", right_width);
        sep_string = Strings.repeat("=", column_width);
        
        String temp_string = sep_string + "\n" +
                             left_string + " " + headerString + " " + right_string + "\n" +
                             sep_string + "\n";
        
        return temp_string;
    }
    
    public static String generateSection(String headerString, int column_width)
    {
        int left_width, right_width, header_width;
        String left_string, right_string = null;
        
        header_width = headerString.length() + 2;
        left_width = (column_width - header_width) / 2;
        right_width = column_width - left_width - header_width;
        left_string = Strings.repeat("-", left_width);
        right_string = Strings.repeat("-", right_width);
        
        String temp_string = left_string + " " + headerString + " " + right_string + "\n";
        
        return temp_string;
    }
}

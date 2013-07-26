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
    /**
     * cli_CommandLineInterface.cpp:569
     * 
     * The original name is PrintCLIMessage_Item.  Renamed because this port doesn't
     * include the print part of the CSoar version.
     * 
     * @param prefixString
     * @param printObject
     * @param column_width
     * @return A justified and formated item string.
     */
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

    /**
     * cli_CommandLineInterface.cpp:578
     * 
     * The original name is PrintCLIMessage_Header.  Renamed because this port doesn't
     * include the print part of the CSoar version.
     * 
     * @param headerString
     * @param column_width
     * @return A justified and formated header string.
     */
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
    
    /**
     * cli_CommandLineInterface.cpp:598
     * 
     * The original name is PrintCLIMessage_Section.  Renamed because this doesn't
     * include the print part of the CSoar version.
     * 
     * @param headerString
     * @param column_width
     * @return A justified and formated section string.
     */
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

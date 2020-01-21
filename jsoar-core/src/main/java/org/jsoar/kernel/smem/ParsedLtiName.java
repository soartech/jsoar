/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 26, 2010
 */
package org.jsoar.kernel.smem;

/**
 * Return value for {@code smem_parse_lti_name}
 * 
 * @author ray
 */
class ParsedLtiName
{
    public final String value;
    public final char id_letter;
    public final long id_number;
    
    public ParsedLtiName(String value, char idLetter, long idNumber)
    {
        this.value = value;
        id_letter = idLetter;
        id_number = idNumber;
    }

}

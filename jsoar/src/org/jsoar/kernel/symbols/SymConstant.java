/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.util.FormattableFlags;
import java.util.Formatter;

import org.jsoar.kernel.parser.PossibleLexemeTypes;
import org.jsoar.util.StringTools;

/**
 * @author ray
 */
public class SymConstant extends Symbol
{
    public final String name;
    
    /**
     * @param hash_id
     */
    /*package*/ SymConstant(int hash_id, String name)
    {
        super(hash_id);
        this.name = name;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.Symbol#asSymConstant()
     */
    @Override
    public SymConstant asSymConstant()
    {
        return this;
    }
    
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#isSameTypeAs(org.jsoar.kernel.symbols.Symbol)
     */
    @Override
    public boolean isSameTypeAs(Symbol other)
    {
        return other.asSymConstant() != null;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.Symbol#getFirstLetter()
     */
    @Override
    public char getFirstLetter()
    {
        return name.charAt(0);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return name;
    }

    /* (non-Javadoc)
     * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
     */
    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision)
    {
        // print.cpp:216:symbol_to_string / SYM_CONSTANT_SYMBOL_TYPE
        
        // Since the csoar kernel uses rereadable==true in 90% of calls to 
        // symbol_to_string, we'll make that the default format. To get the
        // raw format, use the alternate, i.e. "%#s".
        final boolean rereadable = (FormattableFlags.ALTERNATE & flags) == 0;
        if(!rereadable)
        {
            formatter.format(name);
            return;
        }
        
        final PossibleLexemeTypes possible = PossibleLexemeTypes.determine_possible_symbol_types_for_string(name);
        final boolean hasAngleBracket = name.startsWith("<") || name.startsWith(">");
        
        if(!possible.possible_sc || possible.possible_var || possible.possible_ic || possible.possible_fc ||
           !possible.rereadable || hasAngleBracket)
        {
            // BUGBUG if in context where id's could occur, should check
            // possible_id flag here also
            formatter.format(StringTools.string_to_escaped_string(name, '|'));
        }
        else
        {
            formatter.format(name);
        }
    }
    
    
}

/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.util.FormattableFlags;
import java.util.Formatter;

import org.jsoar.kernel.parser.original.PossibleLexemeTypes;
import org.jsoar.util.StringTools;

/**
 * @author ray
 */
public class StringSymbolImpl extends SymbolImpl implements StringSymbol
{
    private final String value;
    
    /**
     * @param hash_id
     */
    StringSymbolImpl(int hash_id, String value)
    {
        super(hash_id);
        this.value = value;
    }

    /**
     * @return the string value of this constant
     */
    public String getValue()
    {
        return value;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.Symbol#asSymConstant()
     */
    @Override
    public StringSymbolImpl asString()
    {
        return this;
    }
    
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#isSameTypeAs(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean isSameTypeAs(SymbolImpl other)
    {
        return other.asString() != null;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#getFirstLetter()
     */
    @Override
    public char getFirstLetter()
    {
        return getValue().charAt(0);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return getValue();
    }

    /**
     * Formattable implementation for symbolic constants. Use in conjunction
     * with the %s format. Since the csoar kernel uses rereadable==true in 90% 
     * of calls to symbol_to_string(), we'll make that the default format. To 
     * get the raw format, use the alternate, i.e. "%#s".
     * 
     * <p>print.cpp:216:symbol_to_string / SYM_CONSTANT_SYMBOL_TYPE
     * 
     * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
     */
    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision)
    {
        // 
        
        // Since the csoar kernel uses rereadable==true in 90% of calls to 
        // symbol_to_string, we'll make that the default format. To get the
        // raw format, use the alternate, i.e. "%#s".
        final boolean rereadable = (FormattableFlags.ALTERNATE & flags) == 0;
        if(!rereadable)
        {
            formatter.format(getValue());
            return;
        }
        
        final PossibleLexemeTypes possible = PossibleLexemeTypes.determine_possible_symbol_types_for_string(getValue());
        final boolean hasAngleBracket = getValue().startsWith("<") || getValue().startsWith(">");
        
        if(!possible.possible_sc || possible.possible_var || possible.possible_ic || possible.possible_fc ||
           !possible.rereadable || hasAngleBracket)
        {
            // BUGBUG if in context where id's could occur, should check
            // possible_id flag here also
            formatter.format(StringTools.string_to_escaped_string(getValue(), '|'));
        }
        else
        {
            formatter.format(getValue());
        }
    }
    
    
}

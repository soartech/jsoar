/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel.symbols;

import java.io.IOException;
import java.util.FormattableFlags;
import java.util.Formatter;

import org.jsoar.kernel.parser.PossibleSymbolTypes;
import org.jsoar.kernel.parser.original.Lexer;
import org.jsoar.util.StringTools;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 * 
 * @author ray
 */
public class StringSymbolImpl extends SymbolImpl implements StringSymbol
{
    private final String value;
    
    /**
     * @param hash_id
     */
    StringSymbolImpl(SymbolFactory factory, int hash_id, String value)
    {
        super(factory, hash_id);
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
     * @see org.jsoar.kernel.symbols.SymbolImpl#importInto(org.jsoar.kernel.symbols.SymbolFactory)
     */
    @Override
    Symbol importInto(SymbolFactory factory)
    {
        return factory.createString(value);
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
     * @see org.jsoar.kernel.symbols.SymbolImpl#numericLess(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean numericLess(SymbolImpl other)
    {
        StringSymbolImpl s = other.asString();
        if(s != null)
        {
        	return (this.value.compareTo(s.value) < 0);
        }
        
        return super.numericLess(other);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#numericLessOrEqual(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean numericLessOrEqual(SymbolImpl other)
    {
    	StringSymbolImpl s = other.asString();
        if(s != null)
        {
        	return (this.value.compareTo(s.value) <= 0);
        }
        
        return super.numericLessOrEqual(other);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#numericGreater(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean numericGreater(SymbolImpl other)
    {
    	StringSymbolImpl s = other.asString();
        if(s != null)
        {
        	return (this.value.compareTo(s.value) > 0);
        }
        
        return super.numericGreater(other);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.symbols.SymbolImpl#numericGreaterOrEqual(org.jsoar.kernel.symbols.SymbolImpl)
     */
    @Override
    public boolean numericGreaterOrEqual(SymbolImpl other)
    {
    	StringSymbolImpl s = other.asString();
        if(s != null)
        {
        	return (this.value.compareTo(s.value) >= 0);
        }
        
        return super.numericGreaterOrEqual(other);
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
        // Since the csoar kernel uses rereadable==true in 90% of calls to 
        // symbol_to_string, we'll make that the default format. To get the
        // raw format, use the alternate, i.e. "%#s".
        final boolean rereadable = (FormattableFlags.ALTERNATE & flags) == 0;
        final String stringToWrite;
        if(rereadable)
        {
            final PossibleSymbolTypes possible = Lexer.determine_possible_symbol_types_for_string(getValue());
            
            // If for any reason, the value could be interpreted as something other than
            // a string, escape it.
            if(!possible.possible_sc  || 
               (possible.possible_id && Character.isUpperCase(getValue().charAt(0))) || 
                possible.possible_var || 
                possible.possible_ic  || 
                possible.possible_fc  ||
               !possible.rereadable)
            {
                stringToWrite = StringTools.string_to_escaped_string(getValue(), '|');
            }
            else
            {
                stringToWrite = getValue();
            }
        }
        else
        {
            stringToWrite = getValue();
        }

        try
        {
            formatter.out().append(stringToWrite);
        }
        catch (IOException e)
        {
        }
    }
    
    
}

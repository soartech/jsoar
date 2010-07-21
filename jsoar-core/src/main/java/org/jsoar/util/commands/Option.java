/**
 * 
 */
package org.jsoar.util.commands;


/**
 * @author voigtjr
 *
 * @param <E> Type of long option object for keying, usually a string.
 */
class Option <E>
{
    static <T> Option<T> newInstance(T longOption, ArgType type)
    {
        return new Option<T>(longOption, type);
    }
    
    /**
     * <p>Argument type, denotes whether or not the option takes an argument or
     * not, or if it is optional.
     * 
     * @author voigtjr
     * 
     */
    enum ArgType
    {
        NONE(' '), REQUIRED('+'), OPTIONAL('?'),
        ;
        
        private char symbol;
        
        private ArgType(char symbol)
        {
            this.symbol = symbol;
        }
        
        public char getSymbol()
        {
            return symbol;
        }
    }
    
    private final E longOption;

    private final ArgType type;

    private Option(E longOption, ArgType type)
    {
        this.longOption = longOption;
        this.type = type;
    }

    String getLongOption()
    {
        return longOption.toString().toLowerCase();
    }
    
    ArgType getType()
    {
        return type;
    }

    @Override
    public String toString()
    {
        return getLongOption() + "(" + type.toString() + ")" + type.getSymbol();
    }
}

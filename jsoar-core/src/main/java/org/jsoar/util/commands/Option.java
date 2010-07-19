/**
 * 
 */
package org.jsoar.util.commands;


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
        NONE, REQUIRED, OPTIONAL,
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
        return getLongOption() + "(" + type.toString() + ")";
    }
}

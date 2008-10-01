/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 17, 2008
 */
package org.jsoar.kernel.memory;

/**
 * @author ray
 */
public enum PreferenceType
{
    ACCEPTABLE_PREFERENCE_TYPE('+', false), 
    REQUIRE_PREFERENCE_TYPE('!', false), 
    REJECT_PREFERENCE_TYPE('-', false), 
    PROHIBIT_PREFERENCE_TYPE('~', false), 
    RECONSIDER_PREFERENCE_TYPE('@', false), 
    UNARY_INDIFFERENT_PREFERENCE_TYPE('=', false), 
    UNARY_PARALLEL_PREFERENCE_TYPE('&', false), 
    BEST_PREFERENCE_TYPE('>', false), 
    WORST_PREFERENCE_TYPE('<', false), 
    BINARY_INDIFFERENT_PREFERENCE_TYPE('=', true), 
    BINARY_PARALLEL_PREFERENCE_TYPE('&', true), 
    BETTER_PREFERENCE_TYPE('>', true), 
    WORSE_PREFERENCE_TYPE('<', true), 
    NUMERIC_INDIFFERENT_PREFERENCE_TYPE('=', true);

    /**
     * print.cpp:892:preference_type_indicator
     */
    private final char indicator;
    private final boolean binary;
    
    private PreferenceType(char indicator, boolean binary)
    {
        this.indicator = indicator;
        this.binary = binary;
    }
    
    /**
     * <p>gdatastructs.h:174:preference_is_binary
     * 
     * @return true if this preference type is binary
     */
    public boolean isBinary()
    {
        return binary;
    }

    /**
     * <p>gdatastructs.h:169:preference_is_unary
     * 
     * @return true if this preference type is unary
     */
    public boolean isUnary()
    {
        return !binary;
    }
    
    /**
     * print.cpp:892:preference_type_indicator
     */
    public char getIndicator()
    {
        return indicator;
    }
}

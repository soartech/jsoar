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
    // NOTE: ORDER IS IMPORTANT HERE!!!!
    ACCEPTABLE_PREFERENCE_TYPE('+'), 
    REQUIRE_PREFERENCE_TYPE('!'), 
    REJECT_PREFERENCE_TYPE('-'), 
    PROHIBIT_PREFERENCE_TYPE('~'), 
    RECONSIDER_PREFERENCE_TYPE('@'), 
    UNARY_INDIFFERENT_PREFERENCE_TYPE('='), 
    UNARY_PARALLEL_PREFERENCE_TYPE('&'), 
    BEST_PREFERENCE_TYPE('>'), 
    WORST_PREFERENCE_TYPE('<'), 
    BINARY_INDIFFERENT_PREFERENCE_TYPE('='), 
    BINARY_PARALLEL_PREFERENCE_TYPE('&'), 
    BETTER_PREFERENCE_TYPE('>'), 
    WORSE_PREFERENCE_TYPE('<'), 
    NUMERIC_INDIFFERENT_PREFERENCE_TYPE('=');

    /**
     * print.cpp:892:preference_type_indicator
     */
    private final char indicator;
    
    private PreferenceType(char indicator)
    {
        this.indicator = indicator;
    }
    
    public boolean isBinary()
    {
        // Add a field for binary rather than testing against the ordinal
        return ordinal() >= BINARY_INDIFFERENT_PREFERENCE_TYPE.ordinal();
    }

    public boolean isUnary()
    {
        return ordinal() <= WORSE_PREFERENCE_TYPE.ordinal();
    }
    
    /**
     * print.cpp:892:preference_type_indicator
     */
    public char getIndicator()
    {
        return indicator;
    }
}

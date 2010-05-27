/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 17, 2008
 */
package org.jsoar.kernel.memory;

/**
 * gdatastructs.h:146:_PREFERENCE_TYPE
 * 
 * @author ray
 */
public enum PreferenceType
{
    /**
     * gdatastructs.h:146:ACCEPTABLE_PREFERENCE_TYPE
     */
    ACCEPTABLE('+', false), 
    
    /**
     * gdatastructs.h:147:REQUIRE_PREFERENCE_TYPE
     */
    REQUIRE('!', false), 
    
    /**
     * gdatastructs.h:148:REJECT_PREFERENCE_TYPE
     */
    REJECT('-', false), 
    
    /**
     * gdatastructs.h:149:PROHIBIT_PREFERENCE_TYPE
     */
    PROHIBIT('~', false), 
    
    /**
     * gdatastructs.h:150:RECONSIDER_PREFERENCE_TYPE
     */
    RECONSIDER('@', false), 
    
    /**
     * gdatastructs.h:151:UNARY_INDIFFERENT_PREFERENCE_TYPE
     */
    UNARY_INDIFFERENT('=', false), 
    
    /**
     * gdatastructs.h:153:BEST_PREFERENCE_TYPE
     */
    BEST('>', false), 
    
    /**
     * gdatastructs.h:154:WORST_PREFERENCE_TYPE
     */
    WORST('<', false), 
    
    /**
     * gdatastructs.h:155:BINARY_INDIFFERENT_PREFERENCE_TYPE
     */
    BINARY_INDIFFERENT('=', true), 
      
    /**
     * gdatastructs.h:157:BETTER_PREFERENCE_TYPE
     */
    BETTER('>', true), 
    
    /**
     * gdatastructs.h:158:WORSE_PREFERENCE_TYPE
     */
    WORSE('<', true), 
    
    /**
     * gdatastructs.h:159:NUMERIC_INDIFFERENT_PREFERENCE_TYPE
     */
    NUMERIC_INDIFFERENT('=', true);

    /**
     * sml_KernelHelpers.cpp:889:pref_names
     */
    private final String displayName;
    
    /**
     * print.cpp:892:preference_type_indicator
     */
    private final char indicator;
    private final boolean binary;
    
    private PreferenceType(char indicator, boolean binary)
    {
        this.displayName = name().replace('_', ' ').toLowerCase();
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
     * sml_KernelHelpers.cpp:889:pref_names
     * 
     * @return Human-friendly name of this preference type
     */
    public String getDisplayName()
    {
        return displayName;
    }
    
    /**
     * print.cpp:892:preference_type_indicator
     * 
     * @return Preference type indicator character
     */
    public char getIndicator()
    {
        return indicator;
    }
}

/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 14, 2008
 */
package org.jsoar.kernel.parser.original;

import org.jsoar.kernel.lhs.RelationalTest;
import org.jsoar.kernel.memory.PreferenceType;

/**
 * @author ray
 */
public enum LexemeType
{
    EOF("eof"),
    VARIABLE("variable"),
    INTEGER("integer"),
    FLOAT("float"),
    IDENTIFIER("identifier"),
    SYM_CONSTANT("symbolic constant"),
    QUOTED_STRING("quoted string"),
    R_PAREN(")"),
    AT("@"),
    TILDE("~")
    {
        
        @Override
        public boolean isPreference()
        {
            return true;
        }
        
        @Override
        public PreferenceType getPreferenceType()
        {
            return PreferenceType.PROHIBIT;
        }
    },
    UP_ARROW("^"),
    L_BRACE("{"),
    R_BRACE("}"),
    EXCLAMATION_POINT("!")
    {
        
        @Override
        public boolean isPreference()
        {
            return true;
        }
        
        @Override
        public PreferenceType getPreferenceType()
        {
            return PreferenceType.REQUIRE;
        }
    },
    COMMA(","),
    GREATER(">")
    {
        
        @Override
        public boolean isPreference()
        {
            return true;
        }
        
        @Override
        public int getRelationalTestType()
        {
            return RelationalTest.GREATER_TEST;
        }
    },
    GREATER_GREATER(">>"),
    GREATER_EQUAL(">=")
    {
        
        @Override
        public int getRelationalTestType()
        {
            return RelationalTest.GREATER_OR_EQUAL_TEST;
        }
    },
    L_PAREN("("),
    EQUAL("=")
    {
        
        @Override
        public boolean isPreference()
        {
            return true;
        }
    },
    NOT_EQUAL("<>")
    {
        
        @Override
        public int getRelationalTestType()
        {
            return RelationalTest.NOT_EQUAL_TEST;
        }
    },
    LESS("<")
    {
        
        @Override
        public boolean isPreference()
        {
            return true;
        }
        
        @Override
        public int getRelationalTestType()
        {
            return RelationalTest.LESS_TEST;
        }
    },
    LESS_EQUAL("<=")
    {
        
        @Override
        public int getRelationalTestType()
        {
            return RelationalTest.LESS_OR_EQUAL_TEST;
        }
    },
    LESS_LESS("<<"),
    LESS_EQUAL_GREATER("<=>")
    {
        
        @Override
        public int getRelationalTestType()
        {
            return RelationalTest.SAME_TYPE_TEST;
        }
    },
    PERIOD("."),
    PLUS("+")
    {
        
        @Override
        public boolean isPreference()
        {
            return true;
        }
        
        @Override
        public PreferenceType getPreferenceType()
        {
            return PreferenceType.ACCEPTABLE;
        }
    },
    MINUS("-")
    {
        
        @Override
        public boolean isPreference()
        {
            return true;
        }
        
        @Override
        public PreferenceType getPreferenceType()
        {
            return PreferenceType.REJECT;
        }
    },
    RIGHT_ARROW("-->");
    
    private final String repr;
    
    LexemeType(String repr)
    {
        this.repr = repr;
    }
    
    /**
     * parser.cpp::is_preference_lexeme
     * 
     * @return true if this is a preference-related lexeme
     */
    public boolean isPreference()
    {
        return false;
    }
    
    public PreferenceType getPreferenceType()
    {
        throw new UnsupportedOperationException(this + " is not appropriate for preference type");
    }
    
    public int getRelationalTestType()
    {
        throw new UnsupportedOperationException(this + " is not appropriate for a relation type");
    }
    
    public String repr()
    {
        return repr;
    }
}

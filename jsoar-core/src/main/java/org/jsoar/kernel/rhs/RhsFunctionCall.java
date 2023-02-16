/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 17, 2008
 */
package org.jsoar.kernel.rhs;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ListHead;
import org.jsoar.util.markers.Marker;

/**
 * Represents a RHS function call. Instances of this class are mutable.
 * 
 * @author ray
 */
public class RhsFunctionCall extends AbstractRhsValue
{
    private final StringSymbol name;
    private final boolean standalone;
    private final List<RhsValue> arguments = new ArrayList<>();
    
    /**
     * Construct a new RHS function call value
     * 
     * @param name the name of the function
     * @param standalone true if it's in a standalone context
     */
    public RhsFunctionCall(StringSymbol name, boolean standalone)
    {
        this.name = name;
        this.standalone = standalone;
    }
    
    private RhsFunctionCall(RhsFunctionCall other)
    {
        this.name = other.name;
        this.standalone = other.standalone;
        for(RhsValue arg : other.arguments)
        {
            this.arguments.add(arg.copy());
        }
    }
    
    /**
     * @return the name of the RHS function
     */
    public StringSymbol getName()
    {
        return name;
    }
    
    /**
     * @return true if the function is in a standalone context
     */
    public boolean isStandalone()
    {
        return standalone;
    }
    
    public void addArgument(RhsValue arg)
    {
        arguments.add(arg);
    }
    
    /**
     * rhsfun.h:rhs_value_to_funcall_list
     * 
     * @return the arguments of the function call
     */
    public List<RhsValue> getArguments()
    {
        return arguments;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.RhsValue#asFunctionCall()
     */
    @Override
    public RhsFunctionCall asFunctionCall()
    {
        return this;
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.RhsValue#copy()
     */
    @Override
    public RhsValue copy()
    {
        return new RhsFunctionCall(this);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.RhsValue#addAllVariables(int, java.util.List)
     */
    @Override
    public void addAllVariables(Marker tc_number, ListHead<Variable> var_list)
    {
        for(RhsValue arg : arguments)
        {
            arg.addAllVariables(tc_number, var_list);
        }
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "(" + name + " " + arguments + ")";
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see java.util.Formattable#formatTo(java.util.Formatter, int, int, int)
     */
    @Override
    public void formatTo(Formatter formatter, int flags, int width, int precision)
    {
        // + and - are treated specially as RHS function names. They are not
        // put in pipes even though they have to be in pipes in most other places
        // they might appear in a production
        // TODO is this special handling for + and - correct, or is there a better way? What about "/" ??
        final StringSymbol name = getName();
        final String nameString = name.getValue();
        if("+".equals(nameString) || "-".equals(nameString) || "/".equals(nameString))
        {
            formatter.format("(%s", nameString);
        }
        else
        {
            formatter.format("(%s", name);
        }
        for(RhsValue v : arguments)
        {
            formatter.format(" %s", v);
        }
        formatter.format(")");
    }
}

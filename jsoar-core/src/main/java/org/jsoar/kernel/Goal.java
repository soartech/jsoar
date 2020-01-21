/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 21, 2010
 */
package org.jsoar.kernel;

import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

/**
 * Public interface for a goal.
 * 
 * <p>This interface can be retrieved from an {@link Identifier}
 * using adaptables.
 * 
 * @author ray
 * @see Identifier
 */
public interface Goal
{
    /**
     * Get the identifier associated with the goal, e.g. {@code S1}.
     * 
     * @return the identifier associated with the goal. 
     */
    Identifier getIdentifier();
    /**
     * Get the goal's currently selected operator.
     * 
     * @return the operator, or {@code null} if none.
     */
    Identifier getOperator();
    
    /**
     * Get the name of the currently selected operator. This is a convenience
     * version of digging through the result of {@link #getOperator()}.
     * 
     * @return the name of the selected operator, or {@code null} if none.
     */
    Symbol getOperatorName();
}

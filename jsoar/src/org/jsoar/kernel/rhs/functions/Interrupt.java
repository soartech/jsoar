/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 19, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.DecisionCycle;
import org.jsoar.kernel.memory.RecognitionMemory;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.util.Arguments;

/**
 * rhsfun.cpp:223:interrupt_rhs_function_code
 * 
 * @author ray
 */
public class Interrupt extends AbstractRhsFunctionHandler
{
    private final RecognitionMemory recMemory;
    private final DecisionCycle decisionCycle;
    
    /**
     * @param name
     */
    public Interrupt(RecognitionMemory recMemory, DecisionCycle decisionCycle)
    {
        super("interrupt", 0, 0);
        
        Arguments.checkNotNull(recMemory, "recMemory");
        Arguments.checkNotNull(decisionCycle, "decisionCycle");
        
        this.recMemory = recMemory;
        this.decisionCycle = decisionCycle;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.symbols.SymbolFactory, java.util.List)
     */
    @Override
    public Symbol execute(SymbolFactory syms, List<Symbol> arguments) throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        decisionCycle.interrupt(recMemory.getProductionBeingFired().getName().getValue());
        return null;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler#mayBeStandalone()
     */
    @Override
    public boolean mayBeStandalone()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler#mayBeValue()
     */
    @Override
    public boolean mayBeValue()
    {
        return false;
    }

}

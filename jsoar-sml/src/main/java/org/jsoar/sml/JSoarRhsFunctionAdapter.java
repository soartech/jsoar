/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 29, 2009
 */
package org.jsoar.sml;

import java.util.List;

import org.jsoar.kernel.rhs.functions.AbstractRhsFunctionHandler;
import org.jsoar.kernel.rhs.functions.Concat;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.jsoar.kernel.rhs.functions.RhsFunctionException;
import org.jsoar.kernel.symbols.Symbol;

import sml.smlRhsEventId;
import sml.Kernel.RhsFunctionInterface;

/**
 * Adapt a SML-style RHS function to a JSoar-style RHS function.
 * 
 * @author ray
 */
public class JSoarRhsFunctionAdapter extends AbstractRhsFunctionHandler
{
    private final String agentName;
    private final RhsFunctionInterface handlerObject;
    private final Object callbackData;
    
    public static JSoarRhsFunctionAdapter create(String agentName, String functionName, 
                                            RhsFunctionInterface handlerObject, Object callbackData)
    {
        return new JSoarRhsFunctionAdapter(agentName, functionName, handlerObject, callbackData);
    }

    private JSoarRhsFunctionAdapter(String agentName, String functionName, RhsFunctionInterface handlerObject, Object callbackData)
    {
        super(functionName);
        
        this.agentName = agentName;
        this.handlerObject = handlerObject;
        this.callbackData = callbackData;
    }

    public JSoarRhsFunctionAdapter copy(String newAgentName)
    {
        return create(newAgentName, getName(), handlerObject, callbackData);
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
            throws RhsFunctionException
    {
        final String result = handlerObject.rhsFunctionHandler(smlRhsEventId.smlEVENT_RHS_USER_FUNCTION.ordinal(), 
                callbackData, agentName, getName(), Concat.concat(arguments));
        return result != null ? context.getSymbols().createString(result) : context.getSymbols().createString("");
    }

}

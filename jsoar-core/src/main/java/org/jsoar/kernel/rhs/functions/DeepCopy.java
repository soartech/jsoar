/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 14, 2008
 */
package org.jsoar.kernel.rhs.functions;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeType;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

/**
 * Implementation of <b>deep-copy</b> RHS function in Soar 9.0.0 originally
 * implemented by SoarTech.
 * 
 * <p><b>deep-copy</b> takes a single identifier argument and returns a copy
 * of the working memory structure reachable from that identifier. Impasse
 * WMEs (those WMEs in the <code>impasse_wmes</code> list of
 * <code>IdentifierImpl</code> are not copied. The created WMEs have the same
 * support as normal WMEs created by the production instantiation that is
 * firing. That is, <b>deep-copy</b> can be thought of as a macro that generates
 * the RHS actions to copy the working memory reachable from a particular id.
 * 
 * <p>rhsfun.cpp:681:deep_copy_rhs_function_code
 * 
 * @author ray
 */
public class DeepCopy extends AbstractRhsFunctionHandler
{
    // Filter out impasse WMEs from the copy operation. Otherwise, there's a weird
    // crash. This is the behavior of the original SoarTech implementation anyway.
    private static final EnumSet<WmeType> FILTER = EnumSet.complementOf(EnumSet.of(WmeType.IMPASSE));
    
    /**
     */
    public DeepCopy()
    {
        super("deep-copy", 1, 1);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
     */
    @Override
    public Symbol execute(RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        
        Identifier startId = arguments.get(0).asIdentifier();
        if(startId == null)
        {
            throw new RhsFunctionException("Only argument to '" + getName() + "' RHS function must be an identifier.");
        }
        
        Map<Identifier, Identifier> idMap = new HashMap<Identifier, Identifier>();
        
        return copy(context, startId, idMap);
    }
    
    /**
     * Depth first copy of the WM graph starting at the given identifier.
     * 
     * @param context RHS function context used to generate WMEs
     * @param id The identifier to copy
     * @param idMap Map from existing identifiers to new identifiers in the copy
     * @return New identifier for id. That is, the identifier in the copy that id
     * maps to in idMap.
     */
    private Identifier copy(RhsFunctionContext context, Identifier id, Map<Identifier, Identifier> idMap)
    {
        Identifier targetId = idMap.get(id);
        if(targetId != null)
        {
            return targetId;
        }
        
        targetId = context.getSymbols().createIdentifier(id.getNameLetter());
        idMap.put(id, targetId);
        
        Iterator<Wme> it = id.getWmes(FILTER); // Don't copy impasse WMEs.
        while(it.hasNext())
        {
            final Wme w = it.next();
            
            final Symbol value = w.getValue();
            Identifier valueId = value.asIdentifier();
            if(valueId == null)
            {
                // Terminal WME. Just add and move on.
                context.addWme(targetId, w.getAttribute(), value);
            }
            else
            {
                // Non-terminal WME. Copy sub-structure, then add the WME.
                valueId = copy(context, valueId, idMap);
                context.addWme(targetId, w.getAttribute(), valueId);
            }
        }
        
        return targetId;
    }
}

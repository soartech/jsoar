/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.rhs;

import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.ListHead;

/**
 * @author ray
 */
public class MakeAction extends Action
{
    /**
     * TODO Looking at the usage of this field in the kernel, it seems like it
     * should always be RhsSymbol value, but {@link ReteBuilder#fixup_rhs_value_variable_references}
     * seems like it can return a {@link ReteLocation} and I'm getting that in a simple
     * unit test, so...
     */
    public RhsValue id;
    public RhsValue attr;
    public RhsValue value;
    public RhsValue referent;
    
    public MakeAction asMakeAction()
    {
        return this;
    }

    private Variable getIdAsVariable()
    {
        RhsSymbolValue symVal = id.asSymbolValue();
        
        return symVal != null ? symVal.getSym().asVariable() : null;
    }
    
    /* (non-Javadoc)
     * @see org.jsoar.kernel.Action#addAllVariables(int, java.util.List)
     */
    @Override
    public void addAllVariables(int tc_number, ListHead<Variable> var_list)
    {
        Variable idVar = getIdAsVariable();
        if(idVar != null)
        {
            idVar.markIfUnmarked(tc_number, var_list);
        }
        attr.addAllVariables(tc_number, var_list);
        value.addAllVariables(tc_number, var_list);
        if(preference_type.isBinary())
        {
            referent.addAllVariables(tc_number, var_list);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        // For debugging only.
        return "(" + id + " ^" + attr + " " + value + " (" + referent + "))";
    }
    
    
}

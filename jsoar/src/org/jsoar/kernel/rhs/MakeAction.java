/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.rhs;

import java.util.List;

import org.jsoar.kernel.symbols.Variable;

/**
 * @author ray
 */
public class MakeAction extends Action
{
    public RhsSymbolValue id;
    public RhsValue attr;
    public RhsValue value;
    public RhsValue referent;
    
    public MakeAction asMakeAction()
    {
        return this;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.Action#addAllVariables(int, java.util.List)
     */
    @Override
    public void addAllVariables(int tc_number, List<Variable> var_list)
    {
        Variable idVar = id.getSym().asVariable();
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
    
    
}

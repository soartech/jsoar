/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 16, 2008
 */
package org.jsoar.kernel.rhs;

import java.util.List;

import org.jsoar.kernel.PreferenceType;
import org.jsoar.kernel.symbols.Variable;

/**
 * @author ray
 */
public abstract class Action
{
    public Action next;
    public PreferenceType preference_type;
    public ActionSupport support = ActionSupport.UNKNOWN_SUPPORT;
    boolean already_in_tc;  /* used only by compile-time o-support calcs */
    
    public static void addAllVariables(Action head, int tc_number, List<Variable> var_list)
    {
        for(Action a = head; a != null; a = a.next)
        {
            a.addAllVariables(tc_number, var_list);
        }
    }
    
    public abstract void addAllVariables(int tc_number, List<Variable> var_list);

    public MakeAction asMakeAction()
    {
        return null;
    }
    public FunctionAction asFunctionAction()
    {
        return null;
    }

}

/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 20, 2008
 */
package org.jsoar.kernel.lhs;

import java.util.ArrayList;
import java.util.List;

import org.jsoar.kernel.symbols.Variable;

/**
 * @author ray
 */
public class ReorderInfo
{
    public List<Variable> vars_requiring_bindings = new ArrayList<Variable>();
    public Condition next_min_cost = null;
}

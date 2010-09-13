/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 3, 2010
 */
package org.jsoar.kernel.learning.rl;

import java.util.Map;
import java.util.Set;

import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.symbols.SymbolImpl;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 * 
 * <p>Container for RL template rule-specific fields.
 * 
 * @author ray
 */
public class RLTemplateInfo
{
    /**
     * production.h:rl_template_conds
     */
    public Condition rl_template_conds = null;  // RL-9.3.0
    /**
     * production.h:rl_template_instantiations
     */
    public Set<Map<SymbolImpl, SymbolImpl>> rl_template_instantiations; // RL-9.3.0
}

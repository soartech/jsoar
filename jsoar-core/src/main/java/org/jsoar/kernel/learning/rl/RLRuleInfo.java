/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 3, 2010
 */
package org.jsoar.kernel.learning.rl;


/**
 * @author ray
 */
public class RLRuleInfo
{
    public double rl_update_count = 0.0;       /* number of (potentially fractional) updates to this rule */
    public double rl_ecr; // RL-9.3.0
    public double rl_efr; // RL-9.3.0

}

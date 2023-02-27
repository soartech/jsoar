/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 3, 2010
 */
package org.jsoar.kernel.learning.rl;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 * 
 * <p>Fields specific to RL rules.
 * 
 * @author ray
 */
public class RLRuleInfo
{
    public double rl_update_count = 0.0; /* number of (potentially fractional) updates to this rule */
    
    // Per-input memory parameters for delta bar delta algorithm
    public double rl_delta_bar_delta_beta = -3.0;
    public double rl_delta_bar_delta_h = 0.0;
    
    public double rl_ecr = 0.0; // RL-9.3.0
    public double rl_efr = 0.0; // RL-9.3.0
    
}

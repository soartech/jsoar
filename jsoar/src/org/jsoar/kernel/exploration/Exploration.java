/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 13, 2008
 */
package org.jsoar.kernel.exploration;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.jsoar.kernel.exploration.ExplorationParameter.ReductionPolicy;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.Slot;

/**
 * exploration.cpp
 * 
 * @author ray
 */
public class Exploration
{
    /**
     * Ways to Do User-Select
     * 
     * gsysparam.h:78:USER_SELECT_
     * 
     * @author ray
     */
    public static enum Policy
    {
        USER_SELECT_BOLTZMANN("boltzmann"),       /* boltzmann algorithm, with respect to temperature */
        USER_SELECT_E_GREEDY("epsilon-greedy"),       /* with probability epsilon choose random, otherwise greedy */
        USER_SELECT_FIRST("first"),       /* just choose the first candidate item */
        USER_SELECT_LAST("last"),       /* choose the last item   AGR 615 */    
        USER_SELECT_RANDOM("random-uniform"),       /* pick one at random */
        USER_SELECT_SOFTMAX("softmax");       /* pick one at random, probabalistically biased by numeric preferences */
        
        private final String policyName;
        
        private Policy(String policyName)
        {
            this.policyName = policyName;
        }
        
        /**
         * exploration.cpp:50:exploration_convert_policy
         * 
         * @return
         */
        public String getPolicyName()
        {
            return policyName;
        }
        
        /**
         * exploration.cpp:68:exploration_convert_policy
         * 
         * @param policyName
         * @return
         */
        public static Policy findPolicy(String policyName)
        {
            for(Policy p : values())
            {
                if(p.policyName.equals(policyName))
                {
                    return p;
                }
            }
            return null;
        }
    }
    
    /**
     * USER_SELECT_MODE_SYSPARAM
     */
    private Policy userSelectMode = Policy.USER_SELECT_SOFTMAX;
    /**
     * USER_SELECT_REDUCE_SYSPARAM
     */
    private boolean autoUpdate = false;
    
    /**
     * Changed from array to map indexed by name
     * 
     * agent.h:752:exploration_params
     */
    private Map<String, ExplorationParameter> parameters = new HashMap<String, ExplorationParameter>();
    
    /**
     * Temporary until exploration is fully implemented and I figure out how I want to deal with
     * random number generation throughout the kernel (DR)
     */
    private Random random = new Random(); // TODO centralize random numbers
    
    /**
     * exploration.cpp:89:exploration_set_policy
     * 
     * @param policy
     * @return
     */
    public boolean exploration_set_policy(String policy_name)
    {   
        Policy policy = Policy.findPolicy( policy_name );
        
        if ( policy != null )
            return exploration_set_policy( policy );
        
        return false;
    }

    /**
     * exploration.cpp:99:exploration_set_policy
     * 
     * @param policy
     * @return
     */
    public boolean exploration_set_policy( Policy policy )
    {
        // TODO throw exception?
        if(policy != null)
        {
            userSelectMode = policy;
        }
        
        return false;
    }
    
    /**
     * exploration.cpp:113:exploration_get_policy
     * 
     * @return
     */
    public Policy exploration_get_policy()
    {
        return userSelectMode;
    }
    
    /**
     * exploration.cpp:121:exploration_add_parameter
     * 
     * @param value
     * @param val_func
     * @param name
     * @return
     */
    public ExplorationParameter exploration_add_parameter( double value, ExplorationValueFunction val_func, String name )
    {
        // new parameter entry
        ExplorationParameter newbie = new ExplorationParameter();
        newbie.value = value;
        newbie.name = name;
        newbie.reduction_policy = ReductionPolicy.EXPLORATION_REDUCTION_EXPONENTIAL;
        newbie.val_func = val_func;
        newbie.rates[ ReductionPolicy.EXPLORATION_REDUCTION_EXPONENTIAL.ordinal() ] = 1;
        newbie.rates[ ReductionPolicy.EXPLORATION_REDUCTION_LINEAR.ordinal() ] = 0;
        
        parameters.put(name, newbie);
        
        return newbie;
    } 
    
    /**
     * exploration.cpp:168:exploration_get_parameter_value
     * 
     * @param parameter
     * @return
     */
    double exploration_get_parameter_value(String parameter )
    {   
        ExplorationParameter param = parameters.get(parameter);
        return param != null ? param.value : 0.0;
    }  
    
    /**
     * exploration.cpp:204:exploration_valid_parameter_value
     * 
     * @param name
     * @param value
     * @return
     */
    boolean exploration_valid_parameter_value( String name, double value )
    {
        ExplorationParameter param = parameters.get( name );
        if ( param == null )
            return false;
        
        return param.val_func.call( value );
    }

    /**
     * exploration.cpp:213:exploration_valid_parameter_value
     * 
     * @param parameter
     * @param value
     * @return
     */
    boolean exploration_valid_parameter_value( ExplorationParameter parameter, double value )
    {
        if(parameter != null)
        {
            return parameter.val_func.call(value);
        }

        return false;
    }
    
    /**
     * exploration.cpp:224:exploration_set_parameter_value
     * 
     * @param name
     * @param value
     * @return
     */
    boolean exploration_set_parameter_value(String name, double value )
    {
        ExplorationParameter param = parameters.get( name );
        if ( param == null )
            return false;
        
        param.value = value;
        
        return true;
    }

    /**
     * exploration.cpp:235:exploration_set_parameter_value
     * 
     * @param parameter
     * @param value
     * @return
     */
    boolean exploration_set_parameter_value(ExplorationParameter parameter, double value )
    {
        if(parameter != null)
        {
            parameter.value = value;
            return true;
        }
        return false;
    } 
    
    /**
     * exploration.cpp:249:exploration_get_auto_update
     * 
     * @return
     */
    boolean exploration_get_auto_update()
    {
        return autoUpdate;
    }

    /**
     * exploration.cpp:257:exploration_set_auto_update
     * 
     * @param setting
     * @return
     */
    boolean exploration_set_auto_update( boolean setting )
    {
        this.autoUpdate = setting;
        
        return true;
    }
    
    /**
     * exploration.cpp:267:exploration_update_parameters
     */
    public void exploration_update_parameters()
    {   
        if ( exploration_get_auto_update( ) )
        {         
            for(ExplorationParameter p : parameters.values())
            {
                p.update();
            }
        }
    }
    
    /**
     * exploration.cpp:322:exploration_get_reduction_policy
     * 
     * @param parameter
     * @return
     */
    ReductionPolicy exploration_get_reduction_policy( String parameter )
    {
        ExplorationParameter param = parameters.get(parameter);
        
        return param != null ? param.reduction_policy : null;
    }

    /**
     * exploration.cpp:331:exploration_get_reduction_policy
     * 
     * @param parameter
     * @return
     */
    ReductionPolicy exploration_get_reduction_policy( ExplorationParameter parameter )
    {
        return parameter != null ? parameter.reduction_policy : null;
    }
    
    /**
     * exploration:375:exploration_set_reduction_policy
     * 
     * @param parameter
     * @param policy_name
     * @return
     */
    boolean exploration_set_reduction_policy( String parameter, String policy_name )
    {
        ExplorationParameter param = parameters.get(parameter);
        if(param == null)
        {
            return false;
        }
        ReductionPolicy policy = ReductionPolicy.findPolicy(policy_name);
        
        if(policy == null)
        {
            return false;
        }
        
        param.reduction_policy = policy;
        
        return true;
    }

    // TODO keep porting exploration.cpp. Stopped at line 390.
    
    /**
     * @param s
     * @param candidates
     * @return
     */
    public Preference exploration_choose_according_to_policy(Slot s, Preference candidates)
    {
        // TODO This is just a temporary implementation...
        int count = Preference.countCandidates(candidates);
        int selection = random.nextInt(count);
        return Preference.getCandidate(candidates, selection);
        
        // TODO implement exploration_choose_according_to_policy
        //throw new UnsupportedOperationException("exploration_choose_according_to_policy not implemented");
    }

}

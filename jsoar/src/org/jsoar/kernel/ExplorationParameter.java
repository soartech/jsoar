/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 17, 2008
 */
package org.jsoar.kernel;



/**
 * exploration.h:32:exploration_parameter
 * 
 * @author ray
 */
public class ExplorationParameter
{
    /**
     * exploration.h:21:EXPLORATION_REDUCTION_
     * 
     * @author ray
     */
    public static enum ReductionPolicy
    {
        EXPLORATION_REDUCTION_EXPONENTIAL("expontential"),
        EXPLORATION_REDUCTION_LINEAR("linear");
        
        private final String policyName;
        
        private ReductionPolicy(String policyName)
        {
            this.policyName = policyName;
        }
        
        /**
         * exploration.cpp:299:exploration_convert_reduction_policy
         * 
         * @return
         */
        public String getPolicyName()
        {
            return policyName;
        }
        
        /**
         * exploration.cpp:309:exploration_convert_reduction_policy
         * 
         * @param policyName
         * @return
         */
        public static ReductionPolicy findPolicy(String policyName)
        {
            for(ReductionPolicy p : values())
            {
                if(p.policyName.equals(policyName))
                {
                    return p;
                }
            }
            return null;
        }
    }
    
    double value;
    String name;
    ReductionPolicy reduction_policy;
    ExplorationValueFunction val_func;
    double rates[] = new double[ReductionPolicy.values().length];
    
    /**
     * exploration.cpp::exploration_update_parameters 
     */
    void update()
    {
        double reduction_rate = rates[reduction_policy.ordinal()];            

        if ( reduction_policy == ReductionPolicy.EXPLORATION_REDUCTION_EXPONENTIAL )
        {
            if ( reduction_rate != 1 )
            {
                this.value = value * reduction_rate;
            }
        }
        else if ( reduction_policy == ReductionPolicy.EXPLORATION_REDUCTION_LINEAR )
        {
            double current_value = this.value;
            
            if ( ( current_value > 0 ) && ( reduction_rate != 0 ) )
                this.value = ( ( ( current_value - reduction_rate ) > 0 )?( current_value - reduction_rate ):( 0 ) );
        }

    }
}

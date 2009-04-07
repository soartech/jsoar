/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 17, 2008
 */
package org.jsoar.kernel.exploration;

import java.util.EnumMap;
import java.util.Map;



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
        EXPLORATION_REDUCTION_EXPONENTIAL("expontential") {

            /* (non-Javadoc)
             * @see org.jsoar.kernel.exploration.ExplorationParameter.ReductionPolicy#isRateValid(double)
             */
            @Override
            public boolean isRateValid(double rate)
            {
                // exploration.cpp:427:exploration_valid_exponential
                return rate >= 0 && rate <= 1;
            }},
            
        EXPLORATION_REDUCTION_LINEAR("linear") {

            /* (non-Javadoc)
             * @see org.jsoar.kernel.exploration.ExplorationParameter.ReductionPolicy#isRateValid(double)
             */
            @Override
            public boolean isRateValid(double rate)
            {
                // exploration.cpp:435:exploration_valid_linear
                return rate >= 0;
            }};
        
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
        
        /**
         * <p>exploration.cpp:403:exploration_valid_reduction_rate
         * 
         * @param rate
         * @return
         */
        public abstract boolean isRateValid(double rate);
    }
    
    double value;
    String name;
    ReductionPolicy reduction_policy;
    ExplorationValueFunction val_func;
    Map<ReductionPolicy, Double> rates = new EnumMap<ReductionPolicy, Double>(ReductionPolicy.class);
    
    /**
     * exploration.cpp::exploration_update_parameters 
     */
    void update()
    {
        double reduction_rate = rates.get(reduction_policy).doubleValue();            

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
    
    public boolean setReductionRate(ReductionPolicy policy, double rate)
    {
        boolean valid = policy.isRateValid(rate);
        if(valid)
        {
            rates.put(policy, rate);
        }
        return valid;
    }
}

/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 13, 2008
 */
package org.jsoar.kernel;

import java.util.Random;

import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.symbols.Identifier;

/**
 * decision_manipulation.cpp
 * 
 * @author ray
 */
public class DecisionManipulation
{
    private final Decider decider;
    private final Random random;
    
    private boolean select_enabled = false;
    private String select_operator = "";
    private long predict_seed;
    private String prediction;
    
    /**
     * @param decider
     */
    public DecisionManipulation(Decider decider, Random random)
    {
        this.decider = decider;
        this.random = random;
        
        select_init();
        predict_init();
    }

    /**
     * decision_manipulation.cpp:28:select_init
     */
    private void select_init()
    {
        select_enabled = false;
        this.select_operator = "";
    }

    /**
     * decision_manipulation.cpp:37:select_next_operator
     * 
     * @param operator_id
     */
    void select_next_operator( String operator_id )
    {
        select_init( );
        
        this.select_enabled = true;
        this.select_operator = operator_id;
    }

    /**
     * decision_manipulation.cpp:48:select_get_operator
     * 
     * @return
     */
    String select_get_operator(  )
    {
        if ( !this.select_enabled )
            return null;

        return this.select_operator;
    }

    /**
     * decision_manipulation.cpp:59:select_force
     * 
     * @param candidates
     * @param reinit
     * @return
     */
    Preference select_force(Preference candidates, boolean reinit )
    {
        Preference return_val = null;
        Preference cand = candidates;

        if ( this.select_enabled )
        {
            // go through the list till we find a match or done
            while ( cand  != null && return_val == null)
            {
                Identifier valueAsId = cand.value.asIdentifier();
                if ( valueAsId != null )
                {
                    // clear comparison string
                    String temp = Character.toString(valueAsId.name_letter) + valueAsId.name_number;

                    if ( this.select_operator.equals( temp ) )
                        return_val = cand;
                }
                
                cand = cand.next;
            }

            if ( reinit )
                select_init( );
        }

        return return_val;
    }

    /**
     * decision_manipulation.cpp:101:predict_init
     */
    void predict_init( )
    {
        this.predict_seed = 0;
        this.prediction = "";
    }

    /**
     * decision_manipulation.cpp:110:predict_srand_store_snapshot
     */
    void predict_srand_store_snapshot()
    {
        int storage_val = 0;

        // TODO Is the range of nextInt() appropriate??
        while ( storage_val == 0)
            storage_val = random.nextInt(); //SoarRandInt();

        predict_seed = storage_val;
    }

    /**
     * decision_manipulation.cpp:123:predict_srand_restore_snapshot
     * 
     * @param clear_snapshot
     */
    void predict_srand_restore_snapshot( boolean clear_snapshot )
    {
        if ( predict_seed != 0)
            random.setSeed(predict_seed); //  SoarSeedRNG( my_agent->predict_seed );

        if ( clear_snapshot )
            predict_init( );
    }

    /**
     * decision_manipulation.cpp:135:predict_set
     * 
     * @param prediction
     */
    void predict_set( String prediction)
    {
        this.prediction = prediction;
    }

    /**
     * decision_manipulation.cpp:143:predict_get
     * 
     * @return
     */
    String predict_get()
    {
        predict_srand_store_snapshot();
        decider.do_decision_phase( true );

        return prediction;
    }
}

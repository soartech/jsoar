/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 5, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.memory.Instantiation;
import org.jsoar.kernel.memory.WmeImpl;

/**
 * An interface intended to decouple Soar-specific handling of rete matches and unmatches
 * from the general rete algorithm.
 * 
 * @author ray
 */
public interface ReteListener
{
    /**
     * handle initial refraction by adding it to tentative_retractions
     * 
     * <p>Refactored out of add_production_to_rete within "if(refacted_inst != null)"
     * 
     * rete.cpp:3515:add_production_to_rete
     * 
     * @param p
     * @param refracted_inst
     * @param p_node
     */
    void startRefraction(Rete rete, Production p, Instantiation refracted_inst, ReteNode p_node);
    
    /**
     * Called after {@link #startRefraction(Production, Instantiation, ReteNode)} and the initial
     * call to {@link #update_node_with_matches_from_above(ReteNode)} on the p-node.
     * 
     * rete.cpp:3515:add_production_to_rete
     * 
     * @param rete The rete
     * @param p The production being added to the rete
     * @param refracted_inst The non-null refracted instantiation
     * @param p_node The p-node for the production
     * @return true if the refracted instantiation matched, false otherwise
     */
    boolean finishRefraction(Rete rete, Production p, Instantiation refracted_inst, ReteNode p_node);

    /**
     * This method is called when a production matches.
     * 
     * rete.cpp:5481:p_node_left_addition
     * 
     * @param rete The rete
     * @param node
     * @param tok
     * @param w
     */
    void p_node_left_addition(Rete rete, ReteNode node, Token tok, WmeImpl w);
    
    /**
     * This method is called when a production unmatches
     * 
     * BUGBUG shouldn't need to pass in both tok and w -- should have the
     * p-node's token get passed in instead, and have it point to the
     * corresponding instantiation structure.
     * 
     * rete.cpp:5885:p_node_left_removal
     * 
     * @param node
     * @param tok
     * @param w
     */
    void p_node_left_removal(Rete rete, ReteNode node, Token tok, WmeImpl w);

    /**
     * Called when a production is being excised. This is called after the p-node's
     * instantiations have been retracted (p_node_left_removal) but before the
     * node itself is actually removed from the rete.
     * 
     * <p>This method was extracted into this interface to decouple Soar-specific
     * rete stuff from the general rete algorithm. See 
     * 
     * @param rete The rete
     * @param p_node The p-node
     */
    void removingProductionNode(Rete rete, ReteNode p_node);

}

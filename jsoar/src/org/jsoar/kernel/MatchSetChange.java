/*
 * (c) 2008  Dave Ray
 *
 * Created on Aug 15, 2008
 */
package org.jsoar.kernel;

import org.jsoar.kernel.rete.Instantiation;
import org.jsoar.kernel.rete.ReteNode;
import org.jsoar.kernel.rete.Token;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.util.AsListItem;

/**
 * explain.h:81
 * 
 * @author ray
 */
public class MatchSetChange
{
    public final  AsListItem<MatchSetChange> next_prev = new AsListItem<MatchSetChange>(this); // dll for all p nodes
    public final AsListItem<MatchSetChange> of_node = new AsListItem<MatchSetChange>(this); // dll for just this p node
    public ReteNode p_node; // for retractions, this can be null if the p node has been excised
    public Token tok; // for assertions only
    public Wme w; // for assertions only
    
    
    public Instantiation inst;   // for retractions only
  /* REW: begin 08.20.97 */
    public Identifier goal;
    public int level;              // Level of the match of the assertion or retraction
    public final AsListItem<MatchSetChange> in_level = new AsListItem<MatchSetChange>(this); // dll for goal level
  /* REW: end   08.20.97 */

}

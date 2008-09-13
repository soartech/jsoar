/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 12, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.memory.Wme;

/**
 * @author ray
 */
public class SoarReteAssertion
{
    public final Production production;
    public final Token token;
    public final Wme wme;
    
    /**
     * @param production
     * @param token
     * @param wme
     */
    public SoarReteAssertion(Production production, Token token, Wme wme)
    {
        this.production = production;
        this.token = token;
        this.wme = wme;
    }
}

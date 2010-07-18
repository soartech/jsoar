/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 18, 2010
 */
package org.jsoar.kernel.rete;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jsoar.kernel.Production;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.junit.Test;

public class SimpleMatcherTest
{
    @Test
    public void testSimpleMatcherRefProdsByObject() throws Exception
    {
        SimpleMatcher matcher = new SimpleMatcher();
        
        // confirm can add production
        Production p = matcher.addProduction("test (<id> ^foo bar)-->(write matches)");
        assertNotNull(p);
        
        // confirm can add wme and production matches
        SymbolFactoryImpl syms = new SymbolFactoryImpl();
        Wme w = new WmeImpl(syms.createIdentifier('S'), syms.createString("foo"), syms.createString("bar"), true, 0);
        matcher.addWme(w);
        assertTrue(matcher.isMatching(p));
        
        // confirm can remove wme and production unmatches
        matcher.removeWme(w);
        assertFalse(matcher.isMatching(p));
        
        // confirm can re-add wme and production re-matches
        matcher.addWme(w);
        assertTrue(matcher.isMatching(p));
        
        // confirm can remove wme and production unmatches again
        matcher.removeWme(w);
        assertFalse(matcher.isMatching(p));
        
        // confirm production actually removed
        matcher.removeProduction(p);
        boolean exceptionThrown = false;
        try
        {
            matcher.getMatches(p);
        }
        catch(Exception e)
        {
            exceptionThrown = true;
        }
        
        assertTrue(exceptionThrown);
    }
    
    @Test
    public void testSimpleMatcherRefProdsByName() throws Exception
    {
        SimpleMatcher matcher = new SimpleMatcher();
        
        // confirm can add production
        Production p = matcher.addProduction("test (<id> ^foo bar)-->(write matches)");
        assertNotNull(p);
        
        // confirm can add wme and production matches
        SymbolFactoryImpl syms = new SymbolFactoryImpl();
        Wme w = new WmeImpl(syms.createIdentifier('S'), syms.createString("foo"), syms.createString("bar"), true, 0);
        matcher.addWme(w);
        assertTrue(matcher.isMatching("test"));
        
        // confirm can remove wme and production unmatches
        matcher.removeWme(w);
        assertFalse(matcher.isMatching("test"));
        
        // confirm can re-add wme and production re-matches
        matcher.addWme(w);
        assertTrue(matcher.isMatching("test"));
        
        // confirm can remove wme and production unmatches again
        matcher.removeWme(w);
        assertFalse(matcher.isMatching("test"));
        
        // confirm production actually removed
        matcher.removeProduction("test");
        boolean exceptionThrown = false;
        try
        {
            matcher.getMatches("test");
        }
        catch(Exception e)
        {
            exceptionThrown = true;
        }
        
        assertTrue(exceptionThrown);
    }
    
    @Test
    public void testSimpleMatcherRemovalsByAll() throws Exception
    {
        SimpleMatcher matcher = new SimpleMatcher();
        
        // confirm can add production
        Production p = matcher.addProduction("test (<id> ^foo bar)-->(write matches)");
        assertNotNull(p);
        
        // confirm can add wme and production matches
        SymbolFactoryImpl syms = new SymbolFactoryImpl();
        Wme w = new WmeImpl(syms.createIdentifier('S'), syms.createString("foo"), syms.createString("bar"), true, 0);
        matcher.addWme(w);
        assertTrue(matcher.isMatching(p));
        
        // confirm can remove wme and production unmatches
        matcher.removeAllWmes();
        assertFalse(matcher.isMatching(p));
        
        // confirm can re-add wme and production re-matches
        matcher.addWme(w);
        assertTrue(matcher.isMatching(p));
        
        // confirm can remove wme and production unmatches again
        matcher.removeAllWmes();
        assertFalse(matcher.isMatching(p));
        
        // confirm production actually removed
        matcher.removeAllProductions();
        boolean exceptionThrown = false;
        try
        {
            matcher.getMatches(p);
        }
        catch(Exception e)
        {
            exceptionThrown = true;
        }
        
        assertTrue(exceptionThrown);
    }
    
}

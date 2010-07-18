/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 18, 2010
 */
package org.jsoar.kernel.rete;

import static org.junit.Assert.assertEquals;
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
        final SimpleMatcher matcher = new SimpleMatcher();
        
        // confirm can add production
        final Production p = matcher.addProduction("test (<id> ^foo bar)-->(write matches)");
        assertNotNull(p);
        
        // confirm can add wme and production matches
        final SymbolFactoryImpl syms = new SymbolFactoryImpl();
        final Wme w = new WmeImpl(syms.createIdentifier('S'), syms.createString("foo"), syms.createString("bar"), true, 0);
        matcher.addWme(w);
        assertTrue(matcher.isMatching(p));
        
        // confirm there is only 1 match
        PartialMatches pm = matcher.getMatches(p);
        assertEquals(pm.getEntries().size(), 1);
        assertEquals(pm.getEntries().get(0).matches, 1);
        
        // confirm can remove wme and production unmatches
        matcher.removeWme(w);
        assertFalse(matcher.isMatching(p));
        
        // confirm there are 0 matches
        pm = matcher.getMatches(p);
        assertEquals(pm.getEntries().size(), 1);
        assertEquals(pm.getEntries().get(0).matches, 0);
        
        // confirm can re-add wme and production re-matches
        matcher.addWme(w);
        assertTrue(matcher.isMatching(p));

        // confirm there is only 1 match
        pm = matcher.getMatches(p);
        assertEquals(pm.getEntries().size(), 1);
        assertEquals(pm.getEntries().get(0).matches, 1);

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
        final SimpleMatcher matcher = new SimpleMatcher();
        
        // confirm can add production
        // using local scope to make sure nothing else in this test refers to the Production instance directly
        {
            final Production p = matcher.addProduction("test (<id> ^foo bar)-->(write matches)");
            assertNotNull(p);
        }
        
        // confirm can add wme and production matches
        final SymbolFactoryImpl syms = new SymbolFactoryImpl();
        final Wme w = new WmeImpl(syms.createIdentifier('S'), syms.createString("foo"), syms.createString("bar"), true, 0);
        matcher.addWme(w);
        assertTrue(matcher.isMatching("test"));
        
        // confirm there is only 1 match
        PartialMatches pm = matcher.getMatches("test");
        assertEquals(pm.getEntries().size(), 1);
        assertEquals(pm.getEntries().get(0).matches, 1);
        
        // confirm can remove wme and production unmatches
        matcher.removeWme(w);
        assertFalse(matcher.isMatching("test"));
        
        // confirm there are 0 matches
        pm = matcher.getMatches("test");
        assertEquals(pm.getEntries().size(), 1);
        assertEquals(pm.getEntries().get(0).matches, 0);
        
        // confirm can re-add wme and production re-matches
        matcher.addWme(w);
        assertTrue(matcher.isMatching("test"));
        
        // confirm there is only 1 match
        pm = matcher.getMatches("test");
        assertEquals(pm.getEntries().size(), 1);
        assertEquals(pm.getEntries().get(0).matches, 1);
        
        // confirm can remove wme and production unmatches again
        matcher.removeWme(w);
        assertFalse(matcher.isMatching("test"));
        
        // confirm there are 0 matches
        pm = matcher.getMatches("test");
        assertEquals(pm.getEntries().size(), 1);
        assertEquals(pm.getEntries().get(0).matches, 0);
        
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
        final SimpleMatcher matcher = new SimpleMatcher();
        
        // confirm can add production
        final Production p = matcher.addProduction("test (<id> ^foo bar)-->(write matches)");
        assertNotNull(p);
        
        // confirm can add wme and production matches
        final SymbolFactoryImpl syms = new SymbolFactoryImpl();
        final Wme w = new WmeImpl(syms.createIdentifier('S'), syms.createString("foo"), syms.createString("bar"), true, 0);
        matcher.addWme(w);
        assertTrue(matcher.isMatching(p));
        
        // confirm there is only 1 match
        PartialMatches pm = matcher.getMatches(p);
        assertEquals(pm.getEntries().size(), 1);
        assertEquals(pm.getEntries().get(0).matches, 1);
        
        // confirm can remove wme and production unmatches
        matcher.removeAllWmes();
        assertFalse(matcher.isMatching(p));
        
        // confirm there are 0 matches
        pm = matcher.getMatches(p);
        assertEquals(pm.getEntries().size(), 1);
        assertEquals(pm.getEntries().get(0).matches, 0);
        
        // confirm can re-add wme and production re-matches
        matcher.addWme(w);
        assertTrue(matcher.isMatching(p));
        
        // confirm there is only 1 match
        pm = matcher.getMatches(p);
        assertEquals(pm.getEntries().size(), 1);
        assertEquals(pm.getEntries().get(0).matches, 1);
        
        // confirm can remove wme and production unmatches again
        matcher.removeAllWmes();
        assertFalse(matcher.isMatching(p));
        
        // confirm there are 0 matches
        pm = matcher.getMatches(p);
        assertEquals(pm.getEntries().size(), 1);
        assertEquals(pm.getEntries().get(0).matches, 0);
        
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

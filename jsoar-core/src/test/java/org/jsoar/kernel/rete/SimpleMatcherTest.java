/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 18, 2010
 */
package org.jsoar.kernel.rete;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.junit.Test;

public class SimpleMatcherTest {
  @Test
  public void testSimpleMatcherRefProdsByObject() throws Exception {
    final SimpleMatcher matcher = new SimpleMatcher();

    // confirm can add production
    final Production p = matcher.addProduction("test (<id> ^foo bar)-->(write matches)");
    assertNotNull(p);

    // confirm can add wme and production matches once
    final SymbolFactoryImpl syms = new SymbolFactoryImpl();
    final Wme w =
        new WmeImpl(
            syms.createIdentifier('S'),
            syms.createString("foo"),
            syms.createString("bar"),
            true,
            0);
    matcher.addWme(w);
    assertEquals(matcher.getNumberMatches(p), 1);

    // confirm there is only 1 match via partial matches structure
    PartialMatches pm = matcher.getMatches(p);
    assertEquals(pm.getEntries().size(), 1);
    assertEquals(pm.getEntries().get(0).matches, 1);

    // confirm can remove wme and production unmatches
    matcher.removeWme(w);
    assertEquals(matcher.getNumberMatches(p), 0);

    // confirm there are 0 matches via partial matches structure
    pm = matcher.getMatches(p);
    assertEquals(pm.getEntries().size(), 1);
    assertEquals(pm.getEntries().get(0).matches, 0);

    // confirm can re-add wme and production re-matches once
    matcher.addWme(w);
    assertEquals(matcher.getNumberMatches(p), 1);

    // confirm there is only 1 match via partial matches structure
    pm = matcher.getMatches(p);
    assertEquals(pm.getEntries().size(), 1);
    assertEquals(pm.getEntries().get(0).matches, 1);

    // confirm can remove wme and production unmatches again
    matcher.removeWme(w);
    assertEquals(matcher.getNumberMatches(p), 0);

    // confirm production actually removed
    matcher.removeProduction(p);
    boolean exceptionThrown = false;
    try {
      matcher.getMatches(p);
    } catch (Exception e) {
      exceptionThrown = true;
    }

    assertTrue(exceptionThrown);
  }

  @Test
  public void testSimpleMatcherRefProdsByName() throws Exception {
    final SimpleMatcher matcher = new SimpleMatcher();

    // confirm can add production
    // using local scope to make sure nothing else in this test refers to the Production instance
    // directly
    {
      final Production p = matcher.addProduction("test (<id> ^foo bar)-->(write matches)");
      assertNotNull(p);
    }

    // confirm can add wme and production matches once
    final SymbolFactoryImpl syms = new SymbolFactoryImpl();
    final Wme w =
        new WmeImpl(
            syms.createIdentifier('S'),
            syms.createString("foo"),
            syms.createString("bar"),
            true,
            0);
    matcher.addWme(w);
    assertEquals(matcher.getNumberMatches("test"), 1);

    // confirm there is only 1 match via partial matches structure
    PartialMatches pm = matcher.getMatches("test");
    assertEquals(pm.getEntries().size(), 1);
    assertEquals(pm.getEntries().get(0).matches, 1);

    // confirm can remove wme and production unmatches
    matcher.removeWme(w);
    assertEquals(matcher.getNumberMatches("test"), 0);

    // confirm there are 0 matches
    pm = matcher.getMatches("test");
    assertEquals(pm.getEntries().size(), 1);
    assertEquals(pm.getEntries().get(0).matches, 0);

    // confirm can re-add wme and production re-matches once
    matcher.addWme(w);
    assertEquals(matcher.getNumberMatches("test"), 1);

    // confirm there is only 1 match via partial matches structure
    pm = matcher.getMatches("test");
    assertEquals(pm.getEntries().size(), 1);
    assertEquals(pm.getEntries().get(0).matches, 1);

    // confirm can remove wme and production unmatches again
    matcher.removeWme(w);
    assertEquals(matcher.getNumberMatches("test"), 0);

    // confirm there are 0 matches via partial matches structure
    pm = matcher.getMatches("test");
    assertEquals(pm.getEntries().size(), 1);
    assertEquals(pm.getEntries().get(0).matches, 0);

    // confirm production actually removed
    matcher.removeProduction("test");
    boolean exceptionThrown = false;
    try {
      matcher.getMatches("test");
    } catch (Exception e) {
      exceptionThrown = true;
    }

    assertTrue(exceptionThrown);
  }

  @Test
  public void testSimpleMatcherRemovalsByAll() throws Exception {
    final SimpleMatcher matcher = new SimpleMatcher();

    // confirm can add production
    final Production p = matcher.addProduction("test (<id> ^foo bar)-->(write matches)");
    assertNotNull(p);

    // confirm can add wme and production matches once
    final SymbolFactoryImpl syms = new SymbolFactoryImpl();
    final Wme w =
        new WmeImpl(
            syms.createIdentifier('S'),
            syms.createString("foo"),
            syms.createString("bar"),
            true,
            0);
    matcher.addWme(w);
    assertEquals(matcher.getNumberMatches(p), 1);

    // confirm there is only 1 match via partial matches structure
    PartialMatches pm = matcher.getMatches(p);
    assertEquals(pm.getEntries().size(), 1);
    assertEquals(pm.getEntries().get(0).matches, 1);

    // confirm can remove wme and production unmatches
    matcher.removeAllWmes();
    assertEquals(matcher.getNumberMatches(p), 0);

    // confirm there are 0 matches via partial matches structure
    pm = matcher.getMatches(p);
    assertEquals(pm.getEntries().size(), 1);
    assertEquals(pm.getEntries().get(0).matches, 0);

    // confirm can re-add wme and production re-matches once
    matcher.addWme(w);
    assertEquals(matcher.getNumberMatches(p), 1);

    // confirm there is only 1 match via partial matches structure
    pm = matcher.getMatches(p);
    assertEquals(pm.getEntries().size(), 1);
    assertEquals(pm.getEntries().get(0).matches, 1);

    // confirm can remove wme and production unmatches again
    matcher.removeAllWmes();
    assertEquals(matcher.getNumberMatches(p), 0);

    // confirm there are 0 matches via partial matches structure
    pm = matcher.getMatches(p);
    assertEquals(pm.getEntries().size(), 1);
    assertEquals(pm.getEntries().get(0).matches, 0);

    // confirm production actually removed
    matcher.removeAllProductions();
    boolean exceptionThrown = false;
    try {
      matcher.getMatches(p);
    } catch (Exception e) {
      exceptionThrown = true;
    }

    assertTrue(exceptionThrown);
  }

  @Test
  public void testSimpleMatcherMultipleInstances() throws Exception {
    final int NUM_WMES = 10;
    final SimpleMatcher matcher = new SimpleMatcher();

    // this rule will match multiple times
    final Production p = matcher.addProduction("test (<id> ^number <x>)-->(write matches)");
    assertNotNull(p);

    // add a bunch of wmes that will cause multiple matches
    final SymbolFactoryImpl syms = new SymbolFactoryImpl();
    final List<Wme> wmes = new ArrayList<Wme>();
    for (int i = 0; i < NUM_WMES; ++i) {
      final Wme w =
          new WmeImpl(
              syms.createIdentifier('S'),
              syms.createString("number"),
              syms.createInteger(i),
              true,
              0);
      matcher.addWme(w);
      wmes.add(w);
      assertEquals(matcher.getNumberMatches(p), i + 1);
    }

    // confirm there are NUM_WMES matches
    PartialMatches pm = matcher.getMatches(p);
    assertEquals(pm.getEntries().size(), 1);
    assertEquals(pm.getEntries().get(0).matches, NUM_WMES);

    // confirm can remove all but 1 wme and production still matches
    for (int i = 1; i < NUM_WMES; ++i) {
      matcher.removeWme(wmes.get(i));
      assertEquals(matcher.getNumberMatches(p), NUM_WMES - i);
    }

    // confirm production unmatches when remove last wme
    matcher.removeWme(wmes.get(0));
    assertEquals(matcher.getNumberMatches(p), 0);

    // confirm there are 0 matches
    pm = matcher.getMatches(p);
    assertEquals(pm.getEntries().size(), 1);
    assertEquals(pm.getEntries().get(0).matches, 0);
  }
}

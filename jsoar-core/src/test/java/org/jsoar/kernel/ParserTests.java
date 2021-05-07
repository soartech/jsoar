/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 19, 2008
 */
package org.jsoar.kernel;

import static org.junit.Assert.assertTrue;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.rhs.ReordererException;
import org.junit.Test;

/** @author ray */
public class ParserTests extends FunctionalTestHarness {

  @Test
  public void testForBadBug517Fix() throws Exception {
    // Original fix for bug 517 (ca. r299) caused a bug in the following production.
    // This tests against the regression.
    agent.getProductions().loadProduction("test (state <s> ^a <val> -^a {<val> < 1}) -->");
    JSoarTest.verifyProduction(
        agent,
        "test",
        ProductionType.USER,
        "sp {test\n"
            + "    (state <s> ^a <val>)\n"
            + "    (<s> -^a { <val> < 1 })\n"
            + "    -->\n"
            + "    \n"
            + "}\n",
        true);
  }

  @Test
  public void testNegatedConjunctiveTestUnbound() throws Exception {
    boolean success;

    // these should all fail in reorder
    success = false;
    try {
      agent
          .getProductions()
          .loadProduction("test (state <s> ^superstate nil -^foo { <> <bad> }) -->");
    } catch (ReordererException e) {
      // <bad> is unbound referent in value test
      success = true;
    }
    assertTrue(success);

    success = false;
    try {
      agent
          .getProductions()
          .loadProduction("test (state <s> ^superstate nil -^{ <> <bad> } <s>) -->");
    } catch (ReordererException e) {
      // <bad> is unbound referent in attr test
      success = true;
    }
    assertTrue(success);

    success = false;
    try {
      agent
          .getProductions()
          .loadProduction(
              "test (state <s> ^superstate nil -^foo { <> <b> }) -{(<s> ^bar <b>) (<s> -^bar { <> <b>})} -->");
    } catch (ReordererException e) {
      // <b> is unbound referent in test, defined in ncc out of scope
      success = true;
    }
    assertTrue(success);

    success = false;
    try {
      agent
          .getProductions()
          .loadProduction(
              "test  (state <s> ^superstate <d> -^foo { <> <b> }) -{(<s> ^bar <b>) (<s> -^bar { <> <d>})} -->");
    } catch (ReordererException e) {
      // <d> is unbound referent in value test in ncc
      success = true;
    }
    assertTrue(success);

    // these should succeed
    agent
        .getProductions()
        .loadProduction(
            "test (state <s> ^superstate <d>) -{(<s> ^bar <b>) (<s> -^bar { <> <d>})} -->");
    agent
        .getProductions()
        .loadProduction(
            "test (state <s> ^superstate nil) -{(<s> ^bar <d>) (<s> -^bar { <> <d>})} -->");
  }
}

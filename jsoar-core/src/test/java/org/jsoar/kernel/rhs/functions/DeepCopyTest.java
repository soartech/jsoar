/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Dec 15, 2008
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.List;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.ByRef;
import org.junit.Before;
import org.junit.Test;

/** @author ray */
public class DeepCopyTest {

  private Agent agent;

  @Before
  public void setUp() throws Exception {
    this.agent = new Agent();
  }

  @Test
  public void testCreateDeepCopyProduction() {
    // When creating new deep-copy production
    DeepCopy production = new DeepCopy();

    // Then name of production is deep-copy
    assertEquals("deep-copy", production.getName());
    // And production requires 1 mandatory argument
    assertEquals(1, production.getMinArguments());
    assertEquals(1, production.getMaxArguments());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExecuteThrowsExceptionIfArgumentsIsNull() throws RhsFunctionException {
    DeepCopy production = new DeepCopy();
    production.execute(mock(RhsFunctionContext.class), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExecuteThrowsExceptionIfContextIsNull() throws RhsFunctionException {
    DeepCopy production = new DeepCopy();
    production.execute(null, Collections.emptyList());
  }

  @Test(expected = RhsFunctionException.class)
  public void testExecuteThrowsExceptionIfSymbolToCopyIsMissing() throws RhsFunctionException {
    // Given a instance of production deep-copy
    DeepCopy production = new DeepCopy();

    // When executing deep-copy production without specifying symbol to copy
    // Then exception is thrown
    production.execute(mock(RhsFunctionContext.class), Collections.emptyList());
  }

  @Test
  public void testExecute() throws Exception {
    final ByRef<Boolean> matched = ByRef.create(Boolean.FALSE);
    agent
        .getRhsFunctions()
        .registerHandler(
            new StandaloneRhsFunctionHandler("match") {

              @Override
              public Symbol execute(RhsFunctionContext context, List<Symbol> arguments)
                  throws RhsFunctionException {
                matched.value = true;
                return null;
              }
            });
    // First a production to create some structure to copy
    agent
        .getProductions()
        .loadProduction(
            """
                createStructure
                (state <s> ^superstate nil)
                -->
                (<s> ^to-copy <tc>)(<tc> ^value 1 ^location <loc> ^another <a>)(<loc> ^x 123 ^y 3.14 ^name |hello| ^loop <tc> ^sub <sub>)(<a> ^foo bar ^link <sub>)""");

    // Next a production to copy the structure
    agent
        .getProductions()
        .loadProduction(
            """
                copyStructure
                (state <s> ^superstate nil ^to-copy <tc>)
                -->
                (<s> ^copy (deep-copy <tc>))""");

    // Finally a production to validate that the structure is there
    agent
        .getProductions()
        .loadProduction(
            """
                testStructure
                (state <s> ^superstate nil ^to-copy <tc-old> ^copy <c>)
                (<c> ^value 1 ^location <loc> ^another <a>)(<loc> ^x 123 ^y 3.14 ^name |hello| ^loop <tc> ^sub <sub>)(<a> ^foo bar ^link <sub>)
                -->
                (match)""");

    agent.getProperties().set(SoarProperties.WAITSNC, true);
    agent.runFor(2, RunType.DECISIONS);

    assertTrue(matched.value);
  }
}

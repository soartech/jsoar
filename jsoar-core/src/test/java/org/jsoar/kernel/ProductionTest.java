/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 3, 2010
 */
package org.jsoar.kernel;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.util.SourceLocation;
import org.junit.Test;

public class ProductionTest {

  @Test
  public void testSetBreakpointEnabledHasNoAffectWhenInterruptFlagIsSet() {
    final Production p =
        Production.newBuilder()
            .type(ProductionType.USER)
            .name("test")
            .conditions(new PositiveCondition(), new PositiveCondition())
            .actions(new MakeAction())
            .interrupt(true)
            .build();

    assertTrue(p.isBreakpointEnabled());
    p.setBreakpointEnabled(false);
    assertTrue(p.isBreakpointEnabled());
  }

  @Test
  public void testSetBreakpointEnabledHasAffectWhenInterruptFlagIsNotSet() {
    final Production p =
        Production.newBuilder()
            .type(ProductionType.USER)
            .name("test")
            .conditions(new PositiveCondition(), new PositiveCondition())
            .actions(new MakeAction())
            .build();

    assertFalse(p.isBreakpointEnabled());
    p.setBreakpointEnabled(true);
    assertTrue(p.isBreakpointEnabled());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testProductionBuilderThrowsExceptionIfTypeIsNull() {
    Production.newBuilder()
        .type(null)
        .name("test")
        .location(mock(SourceLocation.class))
        .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testProductionBuilderThrowsExceptionIfNameIsNull() {
    Production.newBuilder()
        .type(ProductionType.USER)
        .name(null)
        .location(mock(SourceLocation.class))
        .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testProductionBuilderThrowsExceptionIfLocationIsNull() {
    Production.newBuilder()
        .type(ProductionType.USER)
        .name("test")
        .location(null)
        .build();
  }

}

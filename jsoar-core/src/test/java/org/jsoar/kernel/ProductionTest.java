/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 3, 2010
 */
package org.jsoar.kernel;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import org.jsoar.kernel.Production.Builder;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.rete.ReteNode;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.util.DefaultSourceLocation;
import org.jsoar.util.SourceLocation;
import org.junit.Test;

public class ProductionTest {

  @Test
  public void testSetReteNode() {
    // Given a production
    Production production = Production.newBuilder().type(ProductionType.USER).name("test'").build();
    // And Rete node
    ReteNode reteNode = mock(ReteNode.class);

    // When setting rete node
    production.setReteNode(mock(Rete.class), reteNode);

    // Then rete node of Production matches passed rete node
    assertEquals(reteNode, production.getReteNode());
  }

  @Test(expected = IllegalStateException.class)
  public void testSetReteNodeThrowsExceptionIfReteAlreadySet() {
    // Given a Production
    Production production = Production.newBuilder().type(ProductionType.USER).name("test'").build();
    // And rete set
    production.setReteNode(mock(Rete.class), null);

    // When setting rete node
    // Then IllegalStateException is thrown
    production.setReteNode(mock(Rete.class), null);
  }

  @Test(expected = IllegalStateException.class)
  public void testSetReteNodeThrowsExceptionIfReteNodeAlreadySet() {
    // Given a Production
    Production production = Production.newBuilder().type(ProductionType.USER).name("test'").build();
    // And rete node set
    production.setReteNode(null, mock(ReteNode.class));

    // When setting rete node
    // Then IllegalStateException is thrown
    production.setReteNode(null, mock(ReteNode.class));
  }

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
    Production.newBuilder().type(null).name("test").location(mock(SourceLocation.class)).build();
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
    Production.newBuilder().type(ProductionType.USER).name("test").location(null).build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void testSetDocumentationThrowsExceptionIfValueIsNull() {
    Production production =
        Production.newBuilder()
            .type(ProductionType.USER)
            .name("test")
            .location(mock(SourceLocation.class))
            .build();

    production.setDocumentation(null);
  }

  @Test
  public void testProductionBuilderWithMandatoryProperties() {
    // Given Production builder
    Builder builder = Production.newBuilder();
    // And mandatory properties values
    ProductionType type = ProductionType.TEMPLATE;
    final String name = "TEST-BUILDER";

    // When building production
    Production production = builder.type(type).name(name).build();

    // Then Production instance property values match passed values
    assertEquals(name, production.getName());
    assertEquals(type, production.getType());
    assertEquals(DefaultSourceLocation.UNKNOWN, production.getLocation());
    // And firing count is zero
    assertEquals(0, production.getFiringCount());
  }

  @Test
  public void testProductionBuilderWithDocumentation() {
    final String documentationContent = "TEST DOCUMENTATION";

    Production production =
        Production.newBuilder()
            .documentation(documentationContent)
            .type(ProductionType.USER)
            .name("test")
            .location(mock(SourceLocation.class))
            .build();

    assertEquals(documentationContent, production.getDocumentation());
  }

  @Test
  public void testProductionBuilderWithDocumentationNull() {
    Production production =
        Production.newBuilder()
            .documentation(null)
            .type(ProductionType.USER)
            .name("test")
            .location(mock(SourceLocation.class))
            .build();

    assertEquals("", production.getDocumentation());
  }

  @Test
  public void testIncrementFiringCount() {
    // Given a production
    Production production =
        Production.newBuilder()
            .type(ProductionType.USER)
            .name("test")
            .location(mock(SourceLocation.class))
            .build();
    // And a firing count of zero
    assertEquals(0, production.getFiringCount());

    // When increment firing count
    long firingCount = production.incrementFiringCount();

    // Then returned firing count is one
    assertEquals(1, firingCount);
    // And persisted firing count of production is one
    assertEquals(1, production.getFiringCount());
  }

  @Test
  public void testResetFiringCount() {
    // Given a production
    Production production =
        Production.newBuilder()
            .type(ProductionType.USER)
            .name("test")
            .location(mock(SourceLocation.class))
            .build();
    // And a firing count of one
    production.incrementFiringCount();
    assertEquals(1, production.getFiringCount());

    // When reset firing count
    production.resetFiringCount();

    // Then persisted firing count is zero
    assertEquals(0, production.getFiringCount());
  }
}

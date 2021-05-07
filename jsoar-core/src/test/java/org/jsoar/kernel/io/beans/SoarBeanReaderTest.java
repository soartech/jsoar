/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 19, 2009
 */
package org.jsoar.kernel.io.beans;

import static org.junit.Assert.*;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Phase;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.events.OutputEvent;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.ByRef;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** @author ray */
public class SoarBeanReaderTest {
  private Agent agent;

  @Before
  public void setUp() throws Exception {
    this.agent = new Agent();
    this.agent.getProperties().set(SoarProperties.WAITSNC, true);
  }

  @After
  public void tearDown() throws Exception {
    this.agent = null;
  }

  private <T> T runTest(final Class<T> klass) {
    final SoarBeanReader converter = new SoarBeanReader();

    final ByRef<T> bean = ByRef.create(null);
    agent
        .getEvents()
        .addListener(
            OutputEvent.class,
            new SoarEventListener() {

              @Override
              public void onEvent(SoarEvent event) {
                final Wme testCommand = agent.getInputOutput().getPendingCommands().get(0);
                assertNotNull(testCommand);
                try {
                  bean.value = converter.read(testCommand.getValue().asIdentifier(), klass);
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              }
            });

    agent.runFor(1, RunType.DECISIONS);
    assertNotNull(bean.value);

    return bean.value;
  }

  public static class TestReadBeanWithPublicFields {
    public int integerProp;
    public double doubleProp;
    public String stringProp;
  }

  @Test
  public void testReadBeanWithPublicFields() throws Exception {
    // Since this registers an InputOutput handler it requires that
    // running one decision run until just before the INPUT phase
    // So the handler is called.
    // - ALT
    agent.setStopPhase(Phase.INPUT);
    agent.getProperties().set(SoarProperties.WAITSNC, false);
    agent
        .getProductions()
        .loadProduction(
            ""
                + "testReadBeanWithNoSubObjects\n"
                + "(state <s> ^superstate nil ^io.output-link <ol>)\n"
                + "-->\n"
                + "(<ol> ^test <t>)\n"
                + "(<t> ^integerProp 99 ^doubleProp 3.14 ^stringProp |A String| ^arg |arg0| ^arg |arg1|)\n"
                + "");

    final TestReadBeanWithPublicFields bean = runTest(TestReadBeanWithPublicFields.class);

    assertNotNull(bean);
    assertEquals(99, bean.integerProp);
    assertEquals(3.14, bean.doubleProp, 0.0001);
    assertEquals("A String", bean.stringProp);
  }

  @Test
  public void testConversionFromSoarToJavaStylePropertyNames() throws Exception {
    // Since this registers an InputOutput handler it requires that
    // running one decision run until just before the INPUT phase
    // So the handler is called.
    // - ALT
    agent.setStopPhase(Phase.INPUT);

    agent.getProperties().set(SoarProperties.WAITSNC, false);
    agent
        .getProductions()
        .loadProduction(
            ""
                + "testConversionFromSoarToJavaStylePropertyNames\n"
                + "(state <s> ^superstate nil ^io.output-link <ol>)\n"
                + "-->\n"
                + "(<ol> ^test <t>)\n"
                + "(<t> ^integer-prop 99 ^double*prop 3.14 ^string-prop |A String|)\n"
                + "");

    final TestReadBeanWithPublicFields bean = runTest(TestReadBeanWithPublicFields.class);

    assertNotNull(bean);
    assertEquals(99, bean.integerProp);
    assertEquals(3.14, bean.doubleProp, 0.0001);
    assertEquals("A String", bean.stringProp);
  }

  public static class TestReadBeanWithNoSubObjects {
    private int privateIntegerProp;
    private double doubleProp;
    private String stringProp;

    public int getIntegerProp() {
      return privateIntegerProp;
    }

    public void setIntegerProp(int integerProp) {
      this.privateIntegerProp = integerProp;
    }

    public double getDoubleProp() {
      return doubleProp;
    }

    public void setDoubleProp(double doubleProp) {
      this.doubleProp = doubleProp;
    }

    public String getStringProp() {
      return stringProp;
    }

    public void setStringProp(String stringProp) {
      this.stringProp = stringProp;
    }
  }

  /**
   * Test method for {@link
   * org.jsoar.kernel.io.beans.SoarBeanReader#read(org.jsoar.kernel.symbols.Identifier,
   * java.lang.Class)}.
   */
  @Test
  public void testReadBeanWithNoSubObjects() throws Exception {
    // Since this registers an InputOutput handler it requires that
    // running one decision run until just before the INPUT phase
    // So the handler is called.
    // - ALT
    agent.setStopPhase(Phase.INPUT);

    agent.getProperties().set(SoarProperties.WAITSNC, false);
    agent
        .getProductions()
        .loadProduction(
            ""
                + "testReadBeanWithNoSubObjects\n"
                + "(state <s> ^superstate nil ^io.output-link <ol>)\n"
                + "-->\n"
                + "(<ol> ^test <t>)\n"
                + "(<t> ^integerProp 99 ^doubleProp 3.14 ^stringProp |A String| ^arg |arg0| ^arg |arg1|)\n"
                + "");

    final TestReadBeanWithNoSubObjects bean = runTest(TestReadBeanWithNoSubObjects.class);
    assertNotNull(bean);
    assertEquals(99, bean.privateIntegerProp);
    assertEquals(3.14, bean.doubleProp, 0.0001);
    assertEquals("A String", bean.stringProp);
  }

  public static class TestReadBeanWithSubObjects {
    private TestReadBeanWithNoSubObjects subObject1;
    private TestReadBeanWithNoSubObjects subObject2;

    public TestReadBeanWithNoSubObjects getSubObject1() {
      return subObject1;
    }

    public void setSubObject1(TestReadBeanWithNoSubObjects subObject) {
      this.subObject1 = subObject;
    }

    public TestReadBeanWithNoSubObjects getSubObject2() {
      return subObject2;
    }

    public void setSubObject2(TestReadBeanWithNoSubObjects subObject) {
      this.subObject2 = subObject;
    }
  }

  @Test
  public void testReadBeanWithSubObjects() throws Exception {
    // Since this registers an InputOutput handler it requires that
    // running one decision run until just before the INPUT phase
    // So the handler is called.
    // - ALT
    agent.setStopPhase(Phase.INPUT);

    agent.getProperties().set(SoarProperties.WAITSNC, false);
    agent
        .getProductions()
        .loadProduction(
            ""
                + "testReadBeanWithNoSubObjects\n"
                + "(state <s> ^superstate nil ^io.output-link <ol>)\n"
                + "-->\n"
                + "(<ol> ^test <t>)\n"
                + "(<t> ^subObject1 <sub>)\n"
                + "(<t> ^subObject2 <sub>)\n"
                + "(<sub> ^integerProp 99 ^doubleProp 3.14 ^stringProp |A String| ^arg |arg0| ^arg |arg1|)\n"
                + "");

    final TestReadBeanWithSubObjects bean = runTest(TestReadBeanWithSubObjects.class);
    assertNotNull(bean);
    assertNotNull(bean.subObject1);
    assertNotNull(bean.subObject2);

    final TestReadBeanWithNoSubObjects sub = bean.subObject1;
    assertEquals(99, sub.privateIntegerProp);
    assertEquals(3.14, sub.doubleProp, 0.0001);
    assertEquals("A String", sub.stringProp);

    assertSame(bean.subObject1, bean.subObject2);
  }

  public static class TestReadBeanWithArrayField {
    public int[] integers;
    private TestReadBeanWithNoSubObjects[] objs;

    public TestReadBeanWithNoSubObjects[] getObjs() {
      return objs;
    }

    public void setObjs(TestReadBeanWithNoSubObjects[] objs) {
      this.objs = objs;
    }
  }

  @Test
  public void testReadBeanWithArrayField() throws Exception {
    // Since this registers an InputOutput handler it requires that
    // running one decision run until just before the INPUT phase
    // So the handler is called.
    // - ALT
    agent.setStopPhase(Phase.INPUT);

    agent.getProperties().set(SoarProperties.WAITSNC, false);
    agent
        .getProductions()
        .loadProduction(
            ""
                + "testReadBeanWithArrayField\n"
                + "(state <s> ^superstate nil ^io.output-link <ol>)\n"
                + "-->\n"
                + "(<ol> ^test <t>)\n"
                + "(<t> ^integers <ints> ^objs <objs>)"
                + "(<ints> ^int 1 ^int 2 ^int 3 ^int 4)\n"
                + "(<objs> ^obj <o1> ^obj <o2>)\n"
                + "(<o1> ^integerProp 99 ^doubleProp 3.14 ^stringProp |A String|)\n"
                + "(<o2> ^integerProp 100 ^doubleProp 4.14 ^stringProp |B String|)\n"
                + "");

    final TestReadBeanWithArrayField bean = runTest(TestReadBeanWithArrayField.class);

    assertNotNull(bean);
    assertEquals(4, bean.integers.length);
    assertEquals(1, bean.integers[0]);
    assertEquals(2, bean.integers[1]);
    assertEquals(3, bean.integers[2]);
    assertEquals(4, bean.integers[3]);

    assertEquals(2, bean.objs.length);
    final TestReadBeanWithNoSubObjects o1 = bean.objs[0];
    assertEquals(99, o1.privateIntegerProp);
    assertEquals(3.14, o1.doubleProp, 0.0001);
    assertEquals("A String", o1.stringProp);

    final TestReadBeanWithNoSubObjects o2 = bean.objs[1];
    assertEquals(100, o2.privateIntegerProp);
    assertEquals(4.14, o2.doubleProp, 0.0001);
    assertEquals("B String", o2.stringProp);
  }

  public static class TestReadBeanWithSymbolField {
    public Symbol thisIsASymbol;
  }

  @Test
  public void testReadBeanWithSymbolField() throws Exception {
    // Since this registers an InputOutput handler it requires that
    // running one decision run until just before the INPUT phase
    // So the handler is called.
    // - ALT
    agent.setStopPhase(Phase.INPUT);

    agent.getProperties().set(SoarProperties.WAITSNC, false);
    agent
        .getProductions()
        .loadProduction(
            ""
                + "testReadBeanWithSymbolField\n"
                + "(state <s> ^superstate nil ^io.output-link <ol>)\n"
                + "-->\n"
                + "(<ol> ^test <t>)\n"
                + "(<t> ^this-is-a-symbol 99)"
                + "");

    final TestReadBeanWithSymbolField bean = runTest(TestReadBeanWithSymbolField.class);

    assertNotNull(bean);
    assertNotNull(bean.thisIsASymbol);
    assertEquals(99, bean.thisIsASymbol.asInteger().getValue());
  }
}

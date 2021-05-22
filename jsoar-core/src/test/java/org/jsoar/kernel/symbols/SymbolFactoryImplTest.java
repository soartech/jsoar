/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Aug 30, 2008
 */
package org.jsoar.kernel.symbols;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import org.jsoar.kernel.SoarConstants;
import org.jsoar.util.ByRef;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** @author ray */
public class SymbolFactoryImplTest {

  private SymbolFactoryImpl syms;

  @Before
  public void setUp() throws Exception {
    syms = new SymbolFactoryImpl();
  }

  @After
  public void tearDown() throws Exception {
    syms = null;
  }

  @Test
  public void testCreateIdentifierWithDefaultGoalStackLevel() {
    // When creating identifier with name letter 'A'
    IdentifierImpl identifier = syms.createIdentifier('A');

    // Then created identifier has name letter 'A'
    assertEquals('A', identifier.getNameLetter());
    // And level of identifier is SoarConstants.TOP_GOAL_LEVEL
    assertEquals(SoarConstants.TOP_GOAL_LEVEL, identifier.getLevel());
    // And name number is >= 1
    assertTrue(identifier.getNameNumber() >= 1);
    // And hash is not null
    assertNotEquals(0, identifier.getHash());
  }

  @Test
  public void testCreateIdentifierWithSpecificGoalStackLevel() {
    // When creating identifier with name letter 'C' and level 5
    IdentifierImpl identifier = syms.createIdentifier('C', 5);

    // Then created identifier has name letter 'C'
    assertEquals('C', identifier.getNameLetter());
    // And level of identifier is 5
    assertEquals(5, identifier.getLevel());
    // And name number is >=1
    assertTrue(identifier.getNameNumber() >= 1);
    // And hash is not null
    assertNotEquals(0, identifier.getHash());
  }

  @Test
  public void testCreateIdentifierWithExistingNameNumber() {
    // Given a Identifier with name letter 'A' and number '4'
    IdentifierImpl identifier = syms.createIdentifier('A', 4, 0);
    assertEquals(identifier, syms.findIdentifier('A', 4));

    // When creating new Identifier with same name letter and number
    IdentifierImpl newIdentifier = syms.createIdentifier('A', 4, 0);

    // Then existing identifier is replaced by new identifier
    assertSame(newIdentifier, syms.findIdentifier('A', 4));
  }

  @Test
  public void testCreateIdentifierWithNameLetterIsLowerCase() {
    // When creating a identifier with name letter 'd'
    IdentifierImpl identifier = syms.createIdentifier('d');

    // Then name letter of identifier is 'D'
    assertEquals('D', identifier.getNameLetter());
  }

  @Test
  public void testCreateIdentifierSuccessor() {
    // Given a existing identifier for name letter 'S'
    IdentifierImpl firstIdentifier = syms.createIdentifier('S');

    // When creating next identifier for name letter 'S'
    IdentifierImpl nextIdentifier = syms.createIdentifier('S');

    // Then name number of next identifier is incremented
    assertEquals(firstIdentifier.getNameNumber() + 1, nextIdentifier.getNameNumber());
  }

  @Test
  public void testCreateIdentifierWithNameLetterIsNumber() {
    //  Given a Symbol Factory
    // When creating a identifier and passed name letter is a number
    IdentifierImpl identifier = syms.createIdentifier('1');

    // Then name letter of identifier is 'I'
    assertEquals('I', identifier.getNameLetter());
  }

  @Test
  public void testReset() {
    // Given a symbol factory
    // And a short term identifier
    IdentifierImpl shortTermIdentifier = syms.createIdentifier('A');
    shortTermIdentifier.smem_lti = 0;
    // And a Long Term Identifier within the semantic memory
    IdentifierImpl longTermIdentifier = syms.createIdentifier('B');
    longTermIdentifier.smem_lti = 1;

    // When resetting identifier factory
    syms.reset();

    // Then short term identifier is removed
    assertNull(
        syms.findIdentifier(
            shortTermIdentifier.getNameLetter(), shortTermIdentifier.getNameNumber()));
    // And Long Term Identifier still exist
    assertNotNull(
        syms.findIdentifier(
            longTermIdentifier.getNameLetter(), longTermIdentifier.getNameNumber()));
    // And Id counters for all letters are 1
    char[] alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    for (char letter : alphabet) {
      assertEquals(1, syms.getIdNumber(letter));
    }
  }

  @Test
  public void testMakeFloatConstant() {
    DoubleSymbolImpl s = syms.createDouble(3.14);
    assertNotNull(s);
    assertEquals(3.14, s.getValue(), 0.0001);
    assertNotEquals(0, s.getHash());
    assertSame(s, syms.findDouble(s.getValue()));
    assertSame(s, syms.createDouble(s.getValue()));
  }

  @Test
  public void testCreateIntegerSymbol() {
    // given a integer value
    int value = 99;

    // When creating a integer symbol
    IntegerSymbolImpl symbol = syms.createInteger(value);

    // Then created symbol is not null
    assertNotNull(symbol);
    // And value of symbol matches given integer value
    assertEquals(value, symbol.getValue());
    // And hash of symbol is not 0
    assertNotEquals(0, symbol.getHash());
  }

  @Test
  public void testCreateIntegerSymbolExistingValue() {
    // given a exist integer symbol for integer value
    IntegerSymbolImpl symbol = syms.createInteger(32);

    // When creating integer symbol for same integer value
    // Then existing integer symbol for value is returned
    assertSame(symbol, syms.createInteger(symbol.getValue()));
  }

  @Test
  public void testFindIntegerSymbol() {
    // given a exist integer symbol for integer value
    IntegerSymbolImpl symbol = syms.createInteger(27);

    // When searching for integer symbol with same value
    // Then existing integer symbol for value is returned
    assertSame(symbol, syms.findInteger(symbol.getValue()));
  }

  @Test
  public void testMakeLargeIntConstant() {
    IntegerSymbolImpl s = syms.createInteger(999999999999L);
    assertNotNull(s);
    assertEquals(999999999999L, s.getValue());
    assertNotEquals(0, s.getHash());
    assertSame(s, syms.findInteger(s.getValue()));
    assertSame(s, syms.createInteger(s.getValue()));
  }

  @Test
  public void testCreateStringSymbol() {
    // given a string value
    String value = "A sym constant";

    // When creating a string symbol
    StringSymbolImpl symbol = syms.createString(value);

    // Then created symbol is not null
    assertNotNull(symbol);
    // And value of symbol matches given string value
    assertEquals(value, symbol.getValue());
    // And hash of symbol is not 0
    assertNotEquals(0, symbol.getHash());
  }

  @Test
  public void testCreateStringSymbolExistingValue() {
    // given a existig string symbol
    StringSymbol symbol = syms.createString("Another sym constant");

    // When creating integer symbol for same string value
    // Then existing string symbol for value is returned
    assertSame(symbol, syms.createString(symbol.getValue()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateStringSymbolThrowsExceptionIfValueIsNull() {
    // given a string value null
    String value = null;

    // When creating a string symbol with value null
    // Then IllegalArgumentException is thrown
    StringSymbolImpl symbol = syms.createString(null);
  }

  @Test
  public void testFindStringSymbol() {
    // given a exist string symbol for integer value
    StringSymbol symbol = syms.createString("A third sym constant");

    // When searching for string symbol with same value
    // Then existing string symbol for value is returned
    assertSame(symbol, syms.findString(symbol.getValue()));
  }

  @Test
  public void testRemoveIdentifier() {
    // Given a Symbol Factory
    // And a existing identifier
    IdentifierImpl identifier = syms.createIdentifier('B');

    // When removing the identifier
    syms.removeIdentifier(identifier);

    // Then identifier can't be found anymore
    assertNull(syms.findIdentifier(identifier.getNameLetter(), identifier.getNameNumber()));
  }

  @Test
  public void testRemoveIdentifierNull() {
    // Given a Symbol Factory
    // And a existing identifier
    IdentifierImpl identifier = syms.createIdentifier('C');

    // When removing identifier null
    syms.removeIdentifier(null);

    // Then nothing has changed
    assertNotNull(syms.findIdentifier(identifier.getNameLetter(), identifier.getNameNumber()));
  }

  @Test
  public void testFindNonExistingStringSymbol() {
    // When searching for non existing string symbol
    // Then null is returned
    assertNull(syms.findString("NON-EXISTING"));
  }

  @Test
  public void testGenerateNewSymConstant() {
    StringSymbolImpl a0 = syms.createString("A0");
    StringSymbolImpl a1 = syms.createString("A1");

    ByRef<Integer> number = ByRef.create(0);
    StringSymbol a2 = syms.generateUniqueString("A", number);
    assertNotNull(a2);
    assertNotSame(a0, a2);
    assertNotSame(a1, a2);
    assertEquals("A2", a2.getValue());
    assertEquals(3, number.value.intValue());
  }

  @Test
  public void testCreateJavaSymbol() {
    File f = new File(System.getProperty("user.dir"));
    JavaSymbol js = syms.findJavaSymbol(f);
    assertNull(js);
    js = syms.createJavaSymbol(f);
    assertNotNull(js);
    assertEquals(f, js.getValue());
  }

  @Test
  public void testNullJavaSymbol() {
    JavaSymbol js = syms.findJavaSymbol(null);
    assertNotNull(js);
    assertNull(js.getValue());
  }

  @Test
  public void testGarbageCollectedSymbolsAreRemovedFromCache() {
    for (int i = 0; i < 1000; ++i) {
      assertNotNull(syms.createInteger(i));
      assertNotNull(syms.createString(Integer.toString(i)));
    }
    // Why do I believe this test works? Because it fails if I remove the
    // call to the garbage collector here :)
    System.gc();
    for (int i = 0; i < 1000; ++i) {
      assertNull(syms.findInteger(i));
      assertNull(syms.findString(Integer.toString(i)));
    }
  }

  @Test
  public void testGetStringSymbols() {
    final StringSymbolImpl a = syms.createString("a");
    final StringSymbolImpl b = syms.createString("b");

    final List<StringSymbol> strings = syms.getSymbols(StringSymbol.class);
    assertNotNull(strings);
    assertEquals(2, strings.size());
    assertTrue(strings.contains(a));
    assertTrue(strings.contains(b));
  }

  @Test
  public void testGetIntegerSymbols() {
    final IntegerSymbolImpl a = syms.createInteger(2);
    final IntegerSymbolImpl b = syms.createInteger(3);

    final List<IntegerSymbol> values = syms.getSymbols(IntegerSymbol.class);
    assertNotNull(values);
    assertEquals(2, values.size());
    assertTrue(values.contains(a));
    assertTrue(values.contains(b));
  }

  @Test
  public void testGetDoubleSymbols() {
    final DoubleSymbolImpl a = syms.createDouble(2.2);
    final DoubleSymbolImpl b = syms.createDouble(3.3);

    final List<DoubleSymbol> values = syms.getSymbols(DoubleSymbol.class);
    assertNotNull(values);
    assertEquals(2, values.size());
    assertTrue(values.contains(a));
    assertTrue(values.contains(b));
  }

  @Test
  public void testGetVariableSymbols() {
    final Variable a = syms.make_variable("a");
    final Variable b = syms.make_variable("b");

    final List<Variable> values = syms.getSymbols(Variable.class);
    assertNotNull(values);
    assertEquals(2, values.size());
    assertTrue(values.contains(a));
    assertTrue(values.contains(b));
  }

  @Test
  public void testGetJavaSymbols() {
    final JavaSymbolImpl a = syms.createJavaSymbol(new File("hi"));
    final JavaSymbolImpl b = syms.createJavaSymbol(new File("bye"));
    final JavaSymbolImpl n = syms.createJavaSymbol(null);

    final List<JavaSymbol> values = syms.getSymbols(JavaSymbol.class);
    assertNotNull(values);
    assertEquals(3, values.size());
    assertTrue(values.contains(a));
    assertTrue(values.contains(b));
    assertTrue(values.contains(n));
  }

  @Test
  public void testGetIdentifierSymbols() {
    final IdentifierImpl a = syms.createIdentifier('s');
    final IdentifierImpl b = syms.createIdentifier('i');

    final List<Identifier> values = syms.getSymbols(Identifier.class);
    assertNotNull(values);
    assertEquals(2, values.size());
    assertTrue(values.contains(a));
    assertTrue(values.contains(b));
  }

  @Test
  public void testImportReturnsInputUnchangedIfItsAlreadyOwnedByFactory() {
    final IntegerSymbol s = syms.createInteger(99);
    assertSame(s, syms.importSymbol(s));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testImportThrowsAnExceptionForIdentifiers() {
    final Identifier id = syms.createIdentifier('T');
    syms.importSymbol(id);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testImportThrowsAnExceptionForVariables() {
    final Variable id = syms.make_variable("foo");
    syms.importSymbol(id);
  }

  @Test
  public void testCanImportStringsAcrossFactories() {
    final SymbolFactory other = new SymbolFactoryImpl();
    final StringSymbol i = syms.createString("test");
    final Symbol s = other.importSymbol(i);
    assertNotSame(i, s);
    assertEquals(i.getValue(), s.asString().getValue());
  }

  @Test
  public void testCanImportIntegersAcrossFactories() {
    final SymbolFactory other = new SymbolFactoryImpl();
    final IntegerSymbol i = syms.createInteger(12345);
    final Symbol s = other.importSymbol(i);
    assertNotSame(i, s);
    assertEquals(i.getValue(), s.asInteger().getValue());
  }

  @Test
  public void testCanImportDoublesAcrossFactories() {
    final SymbolFactory other = new SymbolFactoryImpl();
    final DoubleSymbol i = syms.createDouble(12345.9);
    final Symbol s = other.importSymbol(i);
    assertNotSame(i, s);
    assertEquals(i.getValue(), s.asDouble().getValue(), 0.000001);
  }

  @Test
  public void testCanImportJavaSymbolsAcrossFactories() {
    final SymbolFactory other = new SymbolFactoryImpl();
    final JavaSymbol i = syms.createJavaSymbol(new File("."));
    final Symbol s = other.importSymbol(i);
    assertNotSame(i, s);
    assertSame(i.getValue(), s.asJava().getValue());
  }
}

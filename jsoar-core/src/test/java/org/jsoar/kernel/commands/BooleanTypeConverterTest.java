package org.jsoar.kernel.commands;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BooleanTypeConverterTest {

  @Test
  public void testConvertValueEnabled() {
    convertValue("enable", true);
    convertValue("ENABLE", true);
  }

  @Test
  public void testConvertValueDisable() {
    convertValue("disable", false);
    convertValue("DISABLE", false);
  }

  @Test
  public void testConvertValueOn() {
    convertValue("on", true);
    convertValue("ON", true);
  }

  @Test
  public void testConvertValueOff() {
    convertValue("off", false);
    convertValue("OFF", false);
  }

  @Test
  public void testConvertValueYes() {
    convertValue("yes", true);
    convertValue("YES", true);
  }

  @Test
  public void testConvertValueNo() {
    convertValue("no", false);
    convertValue("NO", false);
  }

  @Test
  public void testConvertValueTrue() {
    convertValue("true", true);
    convertValue("TRUE", true);
  }

  @Test
  public void testConvertValueFalse() {
    convertValue("false", false);
    convertValue("FALSE", false);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConvertThrowsExceptionIfValueIsNull() {
    BooleanTypeConverter converter = new BooleanTypeConverter();
    converter.convert(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConvertThrowsExceptionIfValueInvalid() {
    BooleanTypeConverter converter = new BooleanTypeConverter();
    converter.convert("invalid");
  }

  private void convertValue(String value, boolean expectedResult) {
    BooleanTypeConverter converter = new BooleanTypeConverter();
    boolean result = converter.convert(value);
    assertEquals(expectedResult, result);
  }
}

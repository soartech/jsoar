package org.jsoar.kernel.parser.original;

import static org.mockito.Mockito.mock;

import org.jsoar.kernel.symbols.VariableGenerator;
import org.junit.Test;

public class OriginalParserImplTest {

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorThrowsExceptionIfVariableGeneratorIsNull() {
    new OriginalParserImpl(null,mock(Lexer.class));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorThrowsExceptionIfLexerIsNull() {
    new OriginalParserImpl(mock(VariableGenerator.class), null);
  }

}

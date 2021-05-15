/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 10, 2010
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.Test;

public class GetUrlTest extends JSoarTest {

  @Test
  public void testCanReadTheContentsOfAUrl() throws Exception {
    // Given a external resource
    // TODO: Create temp file with content
    // And a URL pointing to the external resource
    final URL urlToGet =
        GetUrlTest.class.getResource("GetUrlTest_testCanReadTheContentsOfAUrl.txt");
    assertNotNull(urlToGet);
    // And a get-url production
    final GetUrl get = new GetUrl();

    // When executing url production
    final Symbol result =
        get.execute(rhsFuncContext, Symbols.asList(syms, urlToGet.toExternalForm()));

    // Then returned String symbol matches content of external resource
    assertNotNull(result);
    final StringSymbol resultAsString = result.asString();
    assertNotNull(resultAsString);

    final StringWriter expected = new StringWriter();
    Resources.asCharSource(urlToGet, Charsets.UTF_8).copyTo(expected);
    assertEquals(expected.toString(), resultAsString.toString());
  }

  @Test(expected = RhsFunctionException.class)
  public void testExecuteThrowsExceptionIfUrlIsInvalid() throws RhsFunctionException {
    executeGetUrl(mock(RhsFunctionContext.class), "INVALID URL");
  }

  @Test(expected = RhsFunctionException.class)
  public void testExecuteThrowsExceptionIfUrlIsMissing() throws RhsFunctionException {
    // Given a instance of production get-url
    GetUrl production = new GetUrl();

    // When executing get-url production without passing url
    // Then exception is thrown
    production.execute(mock(RhsFunctionContext.class), Collections.emptyList());
  }

  @Test(expected = RhsFunctionException.class)
  public void testExecuteThrowsExceptionIfUrlIsNull() throws RhsFunctionException {
    executeGetUrl(mock(RhsFunctionContext.class), null);
  }

  @Test(expected = RhsFunctionException.class)
  public void testExecuteThrowsExceptionIfUrlPointsToNonExistingResource()
      throws RhsFunctionException {
    executeGetUrl(mock(RhsFunctionContext.class), "http://non-existing.org/resource");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExecuteThrowsExceptionIfContextIsNull() throws RhsFunctionException {
    GetUrl production = new GetUrl();
    production.execute(null, List.of(mock(Symbol.class)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testExecuteThrowsExceptionIfArgumentsIsNull() throws RhsFunctionException {
    GetUrl production = new GetUrl();
    production.execute(mock(RhsFunctionContext.class), null);
  }

  private void executeGetUrl(final RhsFunctionContext context, final String urlValue)
      throws RhsFunctionException {
    // Given a URL
    Symbol url = mock(Symbol.class);
    when(url.toString()).thenReturn(urlValue);
    // And a instance of production get-url
    GetUrl production = new GetUrl();

    // When executing get-url production
    // Then exception is thrown
    production.execute(mock(RhsFunctionContext.class), List.of(url));
  }
}

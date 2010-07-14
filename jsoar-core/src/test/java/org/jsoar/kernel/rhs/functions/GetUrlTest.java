/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 10, 2010
 */
package org.jsoar.kernel.rhs.functions;


import static org.junit.Assert.*;

import java.io.StringWriter;
import java.net.URL;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;

public class GetUrlTest extends JSoarTest
{

    @Test
    public void testCanReadTheContentsOfAUrl() throws Exception
    {
        final GetUrl get = new GetUrl();
        final URL urlToGet = GetUrlTest.class.getResource("GetUrlTest_testCanReadTheContentsOfAUrl.txt");
        assertNotNull(urlToGet);
        
        final Symbol result = get.execute(rhsFuncContext, Symbols.asList(syms, urlToGet.toExternalForm()));
        assertNotNull(result);
        final StringSymbol resultAsString = result.asString();
        assertNotNull(resultAsString);
        
        final StringWriter expected = new StringWriter();
        CharStreams.copy(Resources.newReaderSupplier(urlToGet, Charsets.UTF_8), expected);
        assertEquals(expected.toString(), resultAsString.toString());
    }
}

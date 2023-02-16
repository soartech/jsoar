/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 10, 2010
 */
package org.jsoar.kernel.rhs.functions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.StringWriter;
import java.net.URL;

import org.jsoar.JSoarTest;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.junit.jupiter.api.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;

class GetUrlTest extends JSoarTest
{
    
    @Test
    void testCanReadTheContentsOfAUrl() throws Exception
    {
        final GetUrl get = new GetUrl();
        final URL urlToGet = GetUrlTest.class.getResource("GetUrlTest_testCanReadTheContentsOfAUrl.txt");
        assertNotNull(urlToGet);
        
        final Symbol result = get.execute(rhsFuncContext, Symbols.asList(syms, urlToGet.toExternalForm()));
        assertNotNull(result);
        final StringSymbol resultAsString = result.asString();
        assertNotNull(resultAsString);
        
        final StringWriter expected = new StringWriter();
        Resources.asCharSource(urlToGet, Charsets.UTF_8).copyTo(expected);
        assertEquals(expected.toString(), resultAsString.toString());
    }
}

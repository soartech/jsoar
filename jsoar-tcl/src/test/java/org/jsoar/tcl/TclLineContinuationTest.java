package org.jsoar.tcl;

import org.junit.Test;

/**
 * Test for jtcl / jacl bug where the TCL line continuation operator doesn't work with
 * files that use Windows-style (CRLF) line breaks.
 * 
 * @author charles.newton
 */
public class TclLineContinuationTest extends TclTest
{
    @Test
    public void testSource() throws Exception
    {
        sourceTestFile(getClass(), "textExecute.soar");
    }
}

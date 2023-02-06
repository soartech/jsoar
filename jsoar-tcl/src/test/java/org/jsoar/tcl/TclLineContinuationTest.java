package org.jsoar.tcl;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import org.junit.jupiter.api.Test;

import com.google.common.io.ByteStreams;

/**
 * Test for jtcl / jacl bug where the TCL line continuation operator doesn't work with
 * files that use Windows-style (CRLF) line breaks.
 * 
 * @author charles.newton
 */
public class TclLineContinuationTest extends TclTestBase
{
    
    /**
     * Test sourcing via a URL.
     */
    @Test
    public void testSourceURL() throws Exception
    {
        sourceTestFile(getClass(), "textExecute.soar");
    }
   
    /**
     * Test sourcing via a file.
     */
    @Test
    public void testSourceFile() throws Exception
    {
        URL url = getSourceTestFile(getClass(), "textExecute.soar");
        final InputStream in = new BufferedInputStream(url.openStream());
        File tmp = File.createTempFile("TclLineContinuationTest", "testSource");
        tmp.deleteOnExit();
        final FileOutputStream fos = new FileOutputStream(tmp);
        ByteStreams.copy(in, fos);
        fos.close();
        ifc.source(tmp);
    }
    
    /**
     * Test sourcing via a call to eval.
     */
    @Test
    public void testSourceEval() throws Exception
    {
        URL url = getSourceTestFile(getClass(), "textExecute.soar");
        final InputStream in = new BufferedInputStream(url.openStream());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteStreams.copy(in, out);
        ifc.eval(out.toString());
    }
}

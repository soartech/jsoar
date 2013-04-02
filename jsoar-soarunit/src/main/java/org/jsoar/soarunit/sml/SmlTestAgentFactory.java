/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 28, 2010
 */
package org.jsoar.soarunit.sml;

import org.jsoar.kernel.SoarException;
import org.jsoar.soarunit.Test;
import org.jsoar.soarunit.TestAgent;
import org.jsoar.soarunit.TestAgentFactory;

/**
 * @author ray
 */
public class SmlTestAgentFactory implements TestAgentFactory
{
//    static
//    {
//        // as of 9.3.2, we no longer need to preload the libraries
//        //preloadSmlLibraries();
//    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgentFactory#createTestAgent()
     */
    @Override
    public TestAgent createTestAgent()
    {
        return new SmlTestAgent();
    }

    /* (non-Javadoc)
     * @see org.jsoar.soarunit.TestAgentFactory#debugTest(org.jsoar.soarunit.Test, boolean)
     */
    @Override
    public void debugTest(Test test, boolean exitOnClose) throws SoarException,
            InterruptedException
    {
        new SmlTestAgent().debug(test, exitOnClose);
    }

//    private static void preloadSmlLibraries()
//    {
        /*
        TODO SoarUnit SML Someday we may need this code. For now, Soar has to be on the system path for the debugger to work anyway.
        final String soarHome = System.getProperty("soar.home", null);
        if(soarHome != null)
        {
            final String soarBinPath = new File(soarHome, "bin").getAbsolutePath();
            final String currentLibPath = System.getProperty("java.library.path", "");
            if(currentLibPath.length() == 0)
            {
                System.setProperty("java.library.path", soarBinPath);
            }
            else
            {
                System.setProperty("java.library.path",
                        soarBinPath + System.getProperty("path.separator") + currentLibPath);
            }
        }
        */

        // these are the libraries as of soar 9.3.1. In 9.3.2, the libraries changed and no longer need to be preloaded.
//        System.loadLibrary("ElementXML");
//        System.loadLibrary("SoarKernelSML");
//        System.loadLibrary("Java_sml_ClientInterface");
//    }
}

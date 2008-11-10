/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 9, 2008
 */
package sml.connection;

import sml.ElementXML;
import sml.kernel.KernelSML;

/**
 * @author ray
 */
public class ReceivedCall implements IncomingCallback
{

    /* (non-Javadoc)
     * @see sml.connection.IncomingCallback#execute(sml.connection.Connection, sml.ElementXML, java.lang.Object)
     */
    @Override
    public ElementXML execute(Connection pConnection, ElementXML pIncoming, Object pUserData)
    {
        //unused(pUserData) ;

        // This must be initialized when the connection was created.
        KernelSML pKernel = (KernelSML)pConnection.GetUserData() ;

        return pKernel.ProcessIncomingSML(pConnection, pIncoming) ;
    }

}

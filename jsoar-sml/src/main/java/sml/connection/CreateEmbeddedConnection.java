/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 7, 2008
 */
package sml.connection;

import sml.sml_Names;
import sml.kernel.KernelSML;

/**
 * @author ray
 */
public class CreateEmbeddedConnection implements CreateEmbeddedConnectionFunction
{

    /* (non-Javadoc)
     * @see sml.connection.CreateEmbeddedConnectionFunction#execute(sml.connection.Connection_Sender_Handle, sml.connection.ProcessMessageFunction, int, int)
     */
    @Override
    public Connection_Receiver_Handle execute(Connection_Sender_Handle hSenderConnection,
            ProcessMessageFunction pProcessMessage, int connectionType, int portToListenOn)
    {
        boolean synch = (connectionType == EmbeddedConnection.SML_SYNCH_CONNECTION) ;

        // Create a connection object which we'll use to talk back to this sender
        EmbeddedConnection pConnection = synch ?
                            EmbeddedConnectionSynch.CreateEmbeddedConnectionSynch() :
                            EmbeddedConnectionAsynch.CreateEmbeddedConnectionAsynch() ;

        // For debugging, record that this connection object is from kernel to client.
        // The client will also have a Connection object which will not have this flag set.
        pConnection.SetIsKernelSide(true) ;

        // Record our kernel object with this connection.  I think we only want one kernel
        // object even if there are many connections (because there's only one kernel) so for now
        // that's how things are set up.
        KernelSML pKernelSML = KernelSML.CreateKernelSML(portToListenOn) ;
        pConnection.SetUserData(pKernelSML) ;

        // If this is a synchronous connection then commands will execute on the embedded client's thread
        // and we don't use the receiver thread.  (Why not?  If we allowed it to run then we'd have to (a)
        // sychronize execution between the two threads and (b) sometimes Soar would be running in the client's thread and
        // sometimes in the receiver's thread (depending on where "run" came from) and that could easily introduce a lot of
        // complicated bugs or where performance would be different depending on whether you pressed "run" in the environment or "run" in a
        // remote debugger).
        if (!pConnection.IsAsynchronous())
            pKernelSML.StopReceiverThread() ;

        // Register for "calls" from the client.
        pConnection.RegisterCallback(new ReceivedCall(), null, sml_Names.getKDocType_Call(), true) ;

        // The original sender is a receiver to us so we need to reverse the type.
        pConnection.AttachConnectionInternal((Connection_Receiver_Handle)hSenderConnection, pProcessMessage) ;

        // Record this as one of the active connections
        // Must only do this after the pConnection object has been fully initialized
        // as the receiver thread may access it once it has been added to this list.
        pKernelSML.AddConnection(pConnection) ;

        return (Connection_Receiver_Handle)pConnection ;
    }

}

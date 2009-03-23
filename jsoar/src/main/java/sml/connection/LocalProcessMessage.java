/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 7, 2008
 */
package sml.connection;

import sml.ElementXML;

/**
 * @author ray
 */
public class LocalProcessMessage implements ProcessMessageFunction
{

    /* (non-Javadoc)
     * @see sml.connection.ProcessMessageFunction#execute(sml.connection.Connection_Receiver_Handle, sml.ElementXML, int)
     */
    @Override
    public ElementXML execute(Connection_Receiver_Handle hReceiverConnection, ElementXML hIncomingMsg, int action)
    {
        // This is the connection object we created in this class, passed to the kernel and have
        // now received back.
        EmbeddedConnection pConnection = (EmbeddedConnection)hReceiverConnection ;

        // Make sure we have been passed a valid connection object.
        if (pConnection == null)
            return null ;

        if (action == SML_MESSAGE_ACTION_CLOSE)
        {
            // Close our connection to the remote process
            pConnection.ClearConnectionHandle() ;

            return null ;
        }

        // Synch connections are all happening within a single thread
        if (action == SML_MESSAGE_ACTION_SYNCH)
        {
            // Create an object to wrap this message.
            ElementXML incomingMsg = hIncomingMsg;

            // For a synchronous connection, immediately execute the incoming message, generating a response
            // which is immediately passed back to the caller.
            ElementXML pResponse = pConnection.InvokeCallbacks(incomingMsg) ;

            if (pResponse == null)
                return null;

            //ElementXML_Handle hResponse = pResponse->Detach() ;
            //delete pResponse ;
            return pResponse ;
        }

        // Asynch connections involve a thread switch.  The message comes in on
        // one thread, is dropped in a message queue and picked up by a second thread.
        if (action == SML_MESSAGE_ACTION_ASYNCH)
        {
            // Store the incoming message on a queue and execute it on the receiver's thread (our thread) at a later point.
            ((EmbeddedConnectionAsynch)pConnection).AddToIncomingMessageQueue(hIncomingMsg) ;

            // There is no immediate response to an asynch message.
            // The response will be sent back to the caller as another asynch message later, once the command has been executed.
            return null ;
        }

        // Not an action we understand, so just ignore it.
        // This allows future versions to use other actions if they wish and
        // we'll remain somewhat compatible.
        return null ;
    }

}

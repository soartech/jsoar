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
public interface ProcessMessageFunction
{
    static final int SML_MESSAGE_ACTION_SYNCH = 1;       // Respond to the message immediately (on the caller's thread)
    static final int SML_MESSAGE_ACTION_CLOSE =   2;       // Close down the connection
    static final int SML_MESSAGE_ACTION_ASYNCH =  3;       // Messages are executed on the receiver's thread, not on the senders, so there is no immediate response.
    static final int SML_MESSAGE_ACTION_TRACE_ON = 4;       // Turn on full tracing of messages (making this a special message means we can do this as a runtime choice)
    static final int SML_MESSAGE_ACTION_TRACE_OFF = 5;      // Turn off full tracing of messages (making this a special message means we can do this as a runtime choice)
    ElementXML execute(Connection_Receiver_Handle receiver, ElementXML pXml, int action) ;
}

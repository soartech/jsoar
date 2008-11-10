/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 7, 2008
 */
package sml.connection;

/**
 * @author ray
 */
public interface CreateEmbeddedConnectionFunction
{
    Connection_Receiver_Handle execute(Connection_Sender_Handle hSenderConnection, ProcessMessageFunction pSenderProcessMessage, int connectionType, int portToListenOn) ;
}

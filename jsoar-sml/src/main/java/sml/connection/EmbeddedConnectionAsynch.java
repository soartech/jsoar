/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 7, 2008
 */
package sml.connection;

import java.util.Iterator;
import java.util.LinkedList;

import sml.ElementXML;
import sml.sml_Names;

/**
 * @author ray
 */
public class EmbeddedConnectionAsynch extends EmbeddedConnection
{
    static EmbeddedConnection CreateEmbeddedConnectionAsynch() { return new EmbeddedConnectionAsynch() ; }

    // Clients should not use this.  Use Connection::CreateEmbeddedConnection instead.
    // Making it protected so you can't accidentally create one like this.
    EmbeddedConnectionAsynch() { } 

    /** A list of messages we've received that have "ack" fields but have yet to match up to the commands which triggered them */
    LinkedList<ElementXML>  m_ReceivedMessageList = new LinkedList<ElementXML>();

    //enum            { kMaxListSize = 10 } ;

    /** Ensures only one thread accesses the response list at a time **/
    //soar_thread::Mutex  m_ListMutex ;
    private String m_ListMutex = new String("EmbeddedConnectionAsynch.m_ListMutex"); 

    /** An event object which we use to have one thread sleep while waiting for another thread to drop off a response to a message */
    //soar_thread::Event  m_WaitEvent ;

    /** Adds the message to the queue, taking ownership of it at the same time */
    void AddResponseToList(ElementXML pResponse)
    {
        if (pResponse == null)
            return ;

        // If this message isn't a response to a command we don't need to keep it
        // because we will never need to retrieve it.
        String pAckID = pResponse.GetAttribute(sml_Names.getKAck()) ;

        if (pAckID == null)
        {
            //delete pResponse ;
            return ;
        }

        //soar_thread::Lock lock(&m_ListMutex) ;
        synchronized(m_ListMutex)
        {
        m_ReceivedMessageList.push(pResponse) ;

        if (m_bTraceCommunications)
            System.out.printf("!! Adding ack for id %s to the pending message list\n", pAckID) ;

        // We keep the received message list from growing indefinitely.  This is because
        // a client may send a command and choose not to listen for the response.
        // (I don't believe this happens today, but it is allowed by the API).
        // In that case the message would remain on this list forever and if we allowed it
        // to grow over time we could be searching an ever increasingly large list of dead messages
        // that will never be retrieved.  I believe (but haven't conclusively proved to my satisfaction yet)
        // that we will never have more messages pending here, for which the client is interested in the
        // response, than there are threads sending commands, so a small max list size should be fine.
        /*
        while (m_ReceivedMessageList.size() > kMaxListSize)
        {
            if (m_bTraceCommunications)
                sml::PrintDebugFormat("Had to clean a message from the pending message list") ;

            ElementXML* pLast = m_ReceivedMessageList.back() ;
            delete pLast ;
            m_ReceivedMessageList.pop_back() ;
        }
        */
        }
        
    }
    
    ElementXML IsResponseInList(String pID)
    {
        //soar_thread::Lock lock(&m_ListMutex) ;
        synchronized(m_ListMutex)
        {
            Iterator<ElementXML> it = m_ReceivedMessageList.iterator();
            while(it.hasNext())
            {
                ElementXML pXML = it.next();
                if(DoesResponseMatch(pXML, pID))
                {
                    if (m_bTraceCommunications)
                        System.out.printf("Found match for %s in pending message list\n", pID) ;
                    
                    it.remove();
                    return pXML;
                }
            }

            return null ;
        }
        
    }

    boolean DoesResponseMatch(ElementXML pResponse, String pID)
    {
        if (pResponse == null || pID == null)
            return false ;

        String pMsgID = pResponse.GetAttribute(sml_Names.getKAck()) ;
        
        if (pMsgID == null)
            return false ;

        if (pMsgID.equals(pID))
            return true ;
        
        if (m_bTraceCommunications)
            System.out.printf("Received ack for message %s while looking for %s\n", pMsgID, pID) ;

        return false ;
    }

    public void delete()
    {
        super.delete();
    }

    // Commands are added to this queue that this connection will
    // process in the future.  E.g. A client would use this call to add
    // a command to the queue that the kernel would then execute.
    void AddToIncomingMessageQueue(ElementXML hMsg)
    {
        // Make sure only one thread modifies the message queue at a time.
        //soar_thread::Lock lock(&m_IncomingMutex) ;
        m_IncomingMessageQueue.push(hMsg) ;

        // Wake up anybody who's waiting for a response
        //m_WaitEvent.TriggerEvent() ;
    }

    public boolean IsAsynchronous() { return true ; }
    public void SendMsg(ElementXML pMsg)
    {
        ClearError() ;

        // Check that we have somebody to send this message to.
        if (m_hConnection == null)
        {
            SetError(ErrorCode.kNoEmbeddedLink) ;
            return ;
        }

        // Add a reference to this object, which will then be released by the receiver of this message when
        // they are done with it.
        // pMsg->AddRefOnHandle() ;
        ElementXML hSendMsg = pMsg; //->GetXMLHandle() ;
/*
    #ifdef _DEBUG
        if (IsTracingCommunications())
        {
            char* pStr = pMsg->GenerateXMLString(true) ;
            sml::PrintDebugFormat("%s Sending %s\n", IsKernelSide() ? "Kernel" : "Client", pStr) ;
            pMsg->DeleteString(pStr) ;
        }
    #endif
*/
        // Make the call to the kernel, passing the message over with the ASYNCH flag, which means there
        // will be no immediate response.
        ElementXML hResponse = m_pProcessMessageFunction.execute(m_hConnection, hSendMsg, ProcessMessageFunction.SML_MESSAGE_ACTION_ASYNCH) ;

        if (hResponse != null)
        {
            SetError(ErrorCode.kInvalidResponse) ;
        }
        
    }
    public ElementXML GetResponseForID(String pID, boolean wait)
    {
        ElementXML pResponse = null;

        // Check if we already have this response cached
        if (DoesResponseMatch(m_pLastResponse, pID))
        {
            pResponse = m_pLastResponse ;
            m_pLastResponse = null ;
            return pResponse ;
        }

        // Also check the list of responses we've stored
        // (This list will always be empty if we're only executing commands
        //  on one thread, but if we are using multiple threads it can come into play).
        pResponse = IsResponseInList(pID) ;
        if (pResponse != null)
        {
            return pResponse ;
        }

        // How long we sleep in seconds+milliseconds each pass through
        // (0 means we only sleep if another thread is scheduled to run --
        //  it ensures maximum performance otherwise).
        long sleepTimeSecs = 0 ;
        long sleepTimeMillisecs = 0 ;

        // How long we will wait before checking for a message (in msecs)
        // (If one comes in it'll wake us up from this immediately, but having
        //  a timeout ensures we don't get stuck forever somehow).
        long maximumWaitTimeSeconds = 1 ;
        long maximumWaitTimeMilliseconds = 0 ;

        // If we don't already have this response cached,
        // then read any pending messages.
        do
        {
            // Loop until there are no more messages waiting for us
            while (ReceiveMessages(false))
            {
                // Check each message to see if it's a match
                if (DoesResponseMatch(m_pLastResponse, pID))
                {
                    pResponse = m_pLastResponse ;
                    m_pLastResponse = null ;
                    return pResponse ;
                } else {
                    AddResponseToList(m_pLastResponse) ;
                    m_pLastResponse = null ;
                }
            }

            // Check to see if the message has been added to the list of
            // waiting messages.  This could have happened on a different
            // thread while we were in here waiting.
            /*ElementXML*/ pResponse = IsResponseInList(pID) ;
            if (pResponse != null)
            {
                return pResponse ;
            }
/*
    #ifdef PROFILE_CONNECTIONS
            m_pTimer->Start() ;
    #endif
*/
            try
            {
                // Wait for a response for up to a second
                // If one comes in it will trigger this event to wake us up immediately.
                synchronized(this)
                {
                    this.wait(1000 * maximumWaitTimeSeconds + maximumWaitTimeMilliseconds);
                    // m_WaitEvent.WaitForEvent(maximumWaitTimeSeconds, maximumWaitTimeMilliseconds) ;
                }

                // Allow other threads the chance to update
                // (by calling with 0 for sleep time we don't give away cycles if
                //  no other thread is waiting to execute).
                Thread.sleep(1000 * sleepTimeSecs + sleepTimeMillisecs);
                //sml::Sleep(sleepTimeSecs, sleepTimeMillisecs) ;
            }
            catch (InterruptedException e)
            {
                // TODO?
                Thread.currentThread().interrupt();
            }
/*
    #ifdef PROFILE_CONNECTIONS
            m_IncomingTime += m_pTimer->Elapsed() ;
    #endif
*/
            // Check if the connection has been closed
            if (IsClosed())
                return null ;

        } while (wait) ;

        // If we get here we didn't find the response.
        // (If we're waiting we'll wait forever, so we'll only get here if
        //  we chose not to wait).
        return null ;
        
    }
    public boolean ReceiveMessages(boolean allMessages)
    {
        // Make sure only one thread is sending messages at a time
        // (This allows us to run a separate thread in clients polling for events even
        //  when the client is sleeping, but we don't want them both to be sending/receiving at the same time).
        //soar_thread::Lock lock(&m_ClientMutex) ;
        synchronized (m_ClientMutex)
        {
        boolean receivedMessage = false ;

        ElementXML pIncomingMsg = PopIncomingMessageQueue() ;

        // While we have messages waiting to come in keep reading them
        while (pIncomingMsg != null)
        {
            // Record that we got at least one message
            receivedMessage = true ;

            // Pass this message back to the client and possibly get their response
            ElementXML pResponse = this.InvokeCallbacks(pIncomingMsg) ;

            // If we got a response to the incoming message, send that response back.
            if (pResponse != null)
            {
                SendMsg(pResponse) ;        
            }

            // We're done with the response
            //delete pResponse ;

            // Record the last incoming message
            //delete m_pLastResponse ;
            m_pLastResponse = pIncomingMsg ;

            // If we're only asked to read one message, we're done.
            if (!allMessages)
                break ;

            // Get the next message from the queue
            pIncomingMsg = PopIncomingMessageQueue() ;
        }

        return receivedMessage ;
        }
    }

}

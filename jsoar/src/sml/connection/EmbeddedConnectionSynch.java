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
public class EmbeddedConnectionSynch extends EmbeddedConnection
{
    // Clients should not use this.  Use Connection::CreateEmbeddedConnection instead which creates
    // a two-way connection.  This just creates a one-way object.
    static EmbeddedConnection CreateEmbeddedConnectionSynch() { return new EmbeddedConnectionSynch() ; }

    // Clients should not use this.  Use Connection::CreateEmbeddedConnection instead.
    // Making it protected so you can't accidentally create one like this.
    EmbeddedConnectionSynch() { } 

    public void delete() { super.delete(); } 

    public boolean IsAsynchronous() { return false ; }
    
    public void SendMsg(ElementXML pMsg)
    {
        ClearError() ;

        // Check that we have somebody to send this message to.
        assert(m_hConnection != null);
        if (m_hConnection == null)
        {
            SetError(ErrorCode.kNoEmbeddedLink) ;
            return ;
        }
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
        ElementXML hResponse = null;

        // Add a reference to this object, which will then be released by the receiver of this message when
        // they are done with it.
        //pMsg->AddRefOnHandle() ;
        ElementXML hSendMsg = pMsg; //->GetXMLHandle() ;

        // Make the call to the kernel, passing the message over and getting an immediate response since this is
        // an embedded synchronous (in thread) call.
        hResponse = m_pProcessMessageFunction.execute(m_hConnection, hSendMsg, ProcessMessageFunction.SML_MESSAGE_ACTION_SYNCH) ;

        // We cache the response
        m_pLastResponse = hResponse; //->Attach(hResponse) ;
    }
    
    public ElementXML GetResponseForID(String pID, boolean wait)
    {
        // For the embedded connection there's no ambiguity over what was the "last" call.
        //unused(pID) ;

        // There's also no need to wait, we always have the result on hand.
        //unused(wait) ;
        
        ClearError() ;

        ElementXML hResponse = m_pLastResponse; //->Detach() ;

        if (hResponse == null)
            return null ;

        // We create a new wrapper object and return that.
        // (If we returned a pointer to m_LastResponse it could change when new messages come in).
        ElementXML pResult = new ElementXML(hResponse) ;
/*
    #ifdef _DEBUG
        if (IsTracingCommunications())
        {
            char* pStr = pResult->GenerateXMLString(true) ;
            sml::PrintDebugFormat("%s Received %s\n", IsKernelSide() ? "Kernel" : "Client", pStr) ;
            pResult->DeleteString(pStr) ;
        }
    #endif
*/
        return pResult ;
        
    }
    
    public boolean ReceiveMessages(boolean allMessages)      
    { 
        ClearError() ; 
        return false ; 
    } 

}

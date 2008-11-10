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
public abstract class EmbeddedConnection extends Connection
{
    static final int SML_SYNCH_CONNECTION = 1;   // Incoming messages are executed immediately on the caller's thread
    static final int SML_ASYNCH_CONNECTION = 2;   // Incoming messages are queued and executed later on the receiver's thread
    
    /*************************************************************
    * @brief Simple function that does the casting from the handle
    *        (which we passed to the other side of the connection)
    *        back to its original object.
    *************************************************************/
    private static EmbeddedConnection GetConnectionFromHandle(Connection_Receiver_Handle hConnection)
    {
        return (EmbeddedConnection)hConnection ;
    }    
    
    /** To "send" a message we call to the process message function for this receiver. **/
    Connection_Receiver_Handle m_hConnection ;

    /** These are the two functions a DLL exports to support an embedded connection interface */
    ProcessMessageFunction              m_pProcessMessageFunction ;
    CreateEmbeddedConnectionFunction    m_pCreateEmbeddedFunction ;
/*
   TODO  #ifdef KERNEL_SML_DIRECT
        // These are shortcut methods we can use if this is an embedded connection
        // to optimize I/O performance.
        DirectAddWMEStringFunction          m_pDirectAddWMEStringFunction ;
        DirectAddWMEIntFunction             m_pDirectAddWMEIntFunction ;
        DirectAddWMEDoubleFunction          m_pDirectAddWMEDoubleFunction ;
        DirectRemoveWMEFunction             m_pDirectRemoveWMEFunction ;

        DirectAddIDFunction                 m_pDirectAddIDFunction ;

        DirectGetAgentSMLHandleFunction     m_pDirectGetAgentSMLHandleFunction;

        DirectRunFunction                   m_pDirectRunFunction ;
    #endif
*/
    /** We need to cache the responses to calls **/
    ElementXML m_pLastResponse ;

    // Clients should not use this.  Use Connection::CreateEmbeddedConnection instead.
    // Making it protected so you can't accidentally create one like this.
    protected EmbeddedConnection()
    {
        m_pLastResponse = new ElementXML() ;
        m_hConnection   = null ;
        m_pProcessMessageFunction = null;
        m_pCreateEmbeddedFunction = null;
    }
    
    public void delete()
    {
        m_pLastResponse = null;
    }

    // Link two embedded connections together
    void AttachConnectionInternal(Connection_Receiver_Handle hConnection, ProcessMessageFunction pProcessMessage)
    {
        ClearError() ;
        m_hConnection = hConnection ;
        m_pProcessMessageFunction = pProcessMessage ;
    }
    
    boolean AttachConnection(String pLibraryName, boolean optimized, int portToListenOn)
    {
        // Get the functions that a DLL must export to support an embedded connection.
        m_pProcessMessageFunction = new LocalProcessMessage(); // (ProcessMessageFunction)GetProcAddress(hLibrary, "sml_ProcessMessage") ;
        m_pCreateEmbeddedFunction = new CreateEmbeddedConnection(); // (CreateEmbeddedConnectionFunction)GetProcAddress(hLibrary, "sml_CreateEmbeddedConnection") ;
/*
TODO    #ifdef KERNEL_SML_DIRECT
        m_pDirectAddWMEStringFunction =     (DirectAddWMEStringFunction)GetProcAddress(hLibrary, "sml_DirectAddWME_String") ;
        m_pDirectAddWMEIntFunction =        (DirectAddWMEIntFunction)GetProcAddress(hLibrary, "sml_DirectAddWME_Int") ;
        m_pDirectAddWMEDoubleFunction =     (DirectAddWMEDoubleFunction)GetProcAddress(hLibrary, "sml_DirectAddWME_Double") ;
        m_pDirectRemoveWMEFunction =        (DirectRemoveWMEFunction)GetProcAddress(hLibrary, "sml_DirectRemoveWME") ;

        m_pDirectAddIDFunction =            (DirectAddIDFunction)GetProcAddress(hLibrary, "sml_DirectAddID") ;

        m_pDirectGetAgentSMLHandleFunction = (DirectGetAgentSMLHandleFunction)GetProcAddress(hLibrary, "sml_DirectGetAgentSMLHandle") ;

        m_pDirectRunFunction =              (DirectRunFunction)GetProcAddress(hLibrary, "sml_DirectRun") ;
        
        // Check that we got the list of functions and if so enable the direct connection
        if (m_pDirectAddWMEStringFunction && m_pDirectAddWMEIntFunction && m_pDirectAddWMEDoubleFunction &&
            m_pDirectRemoveWMEFunction    && m_pDirectAddIDFunction     && m_pDirectRunFunction)
        {
            // We only enable direct connections if we found all of the methods, this is a synchronous connection (i.e. we execute
            // on the client's thread) and the client says it's ok to use these optimizations.
            if (optimized && !IsAsynchronous())
                m_bIsDirectConnection = true ;
        }
    #endif
*/
        // See if we got the functions
        if (m_pProcessMessageFunction == null || m_pCreateEmbeddedFunction == null)
        {
            SetError(ErrorCode.kFunctionsNotFound) ;
            return false ;
        }
/*
    #else // defined(LINUX_SHARED) || defined(WINDOWS_SHARED)
        // If we're not in Windows and we can't dynamically load methods we'll just get
        // by with the two we really need.  This just means we can't get maximum optimization on
        // this particular platform.
        m_pProcessMessageFunction = &sml_ProcessMessage;
        m_pCreateEmbeddedFunction = &sml_CreateEmbeddedConnection;

    #endif // not (defined(LINUX_SHARED) || defined(WINDOWS_SHARED))
*/
        // We only use the creation function once to create a connection object (which we'll pass back
        // with each call).
        int connectionType = this.IsAsynchronous() ? SML_ASYNCH_CONNECTION : SML_SYNCH_CONNECTION ;
        m_hConnection = m_pCreateEmbeddedFunction.execute( (Connection_Sender_Handle)this, new LocalProcessMessage(), connectionType, portToListenOn) ;

        if (m_hConnection == null)
        {
            SetError(ErrorCode.kCreationFailed) ;
            return false ;
        }

        // When we reach here we have a connection object (m_hConnection) back from KernelSML and
        // we have the function (m_pProcessMessageFunction) that we'll use to communicate with that library.
        return true ;
        
    }
    void ClearConnectionHandle() { m_hConnection = null ; }

    public void CloseConnection()
    {
        ClearError() ;

        if (m_hConnection != null)
        {
            // Make the call to the kernel to close this connection
            ElementXML hResponse = m_pProcessMessageFunction.execute(m_hConnection, (ElementXML)null, ProcessMessageFunction.SML_MESSAGE_ACTION_CLOSE) ;
            //unused(hResponse) ;
        }
        
        m_hConnection = null ;
        
    }
    public boolean IsClosed()
    {
        return (m_hConnection == null) ;        
    }
    public boolean IsRemoteConnection() { return false ; }

    public void SetTraceCommunications(boolean state)
    {
        ClearError() ;

        m_bTraceCommunications = state ;

        if (m_hConnection != null)
        {
            // Tell the kernel to turn tracing on or off
            ElementXML hResponse = m_pProcessMessageFunction.execute(m_hConnection, (ElementXML)null, 
                        state ? ProcessMessageFunction.SML_MESSAGE_ACTION_TRACE_ON : ProcessMessageFunction.SML_MESSAGE_ACTION_TRACE_OFF) ;
            //unused(hResponse) ;
        }
        
    }

    // Overridden in concrete subclasses
    public abstract boolean IsAsynchronous();     // Returns true if messages are queued and executed on receiver's thread
    public abstract void SendMsg(ElementXML pMsg);
    public abstract ElementXML GetResponseForID(String pID, boolean wait);
    public abstract boolean ReceiveMessages(boolean allMessages);
/*
    TODO #ifdef KERNEL_SML_DIRECT
        // Direct methods, only supported for embedded connections and only used to optimize
        // the speed when doing I/O over an embedded connection (where speed is most critical)
        void DirectAddWME_String(Direct_AgentSML_Handle pAgentSML, char const* pId, char const* pAttribute, char const* pValue, long clientTimetag)
        {
            m_pDirectAddWMEStringFunction(pAgentSML, pId, pAttribute, pValue, clientTimetag) ;
        }
        void DirectAddWME_Int(Direct_AgentSML_Handle pAgentSML, char const* pId, char const* pAttribute, int value, long clientTimetag)
        {
            m_pDirectAddWMEIntFunction(pAgentSML, pId, pAttribute, value, clientTimetag) ;
        }
        void DirectAddWME_Double(Direct_AgentSML_Handle pAgentSML, char const* pId, char const* pAttribute, double value, long clientTimetag)
        {
            m_pDirectAddWMEDoubleFunction(pAgentSML, pId, pAttribute, value, clientTimetag) ;
        }
        void DirectRemoveWME(Direct_AgentSML_Handle pAgentSML, long clientTimetag)
        {
            m_pDirectRemoveWMEFunction(pAgentSML, clientTimetag) ;
        }

        void DirectAddID(Direct_AgentSML_Handle pAgentSML, char const* pId, char const* pAttribute, char const* pValueId, long clientTimetag)
        {
            m_pDirectAddIDFunction(pAgentSML, pId, pAttribute, pValueId, clientTimetag) ;
        }

        Direct_AgentSML_Handle DirectGetAgentSMLHandle(char const* pAgentName)
        {
            return m_pDirectGetAgentSMLHandleFunction(pAgentName) ;
        }

        void DirectRun(char const* pAgentName, boolean forever, int stepSize, int interleaveSize, int count)
        {
            m_pDirectRunFunction(pAgentName, forever, stepSize, interleaveSize, count) ;
        }

    #endif
*/
}

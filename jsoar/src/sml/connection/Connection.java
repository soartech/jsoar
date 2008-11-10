/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 6, 2008
 */
package sml.connection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.jsoar.util.ByRef;

import sml.AnalyzeXML;
import sml.ElementXML;
import sml.sml_Names;

/**
 * @author ray
 */
public abstract class Connection
{
    public static final int kDefaultSMLPort = 12121;

    // Maps from SML document types (e.g. "call") to a list of functions to call when that type of message is received.
    protected final Map<String, LinkedList<Callback>>     m_CallbackMap = new HashMap<String, LinkedList<Callback>>();

    // The client or kernel may wish to keep state information (e.g. a pointer into gSKI for the kernel) with the
    // connection.  If so, it can store it here.  The caller retains ownership of this object, so it won't be
    // deleted when the connection is deleted.
    protected Object           m_pUserData ;

    // The ID to use for the next message we send
    protected int             m_MessageID ;

    // The error status of the last function called.
    protected ErrorCode       m_ErrorCode ;

    // A list of messages that have been received on this connection and are waiting to be executed.
    // This queue may not be in use for a given type of connection
    protected final LinkedList<ElementXML>    m_IncomingMessageQueue = new LinkedList<ElementXML>();

    // We use this mutex to serialize acccess to the incoming message queue (for certain types of connections)
    protected String  m_IncomingMutex = new String("m_IncomingMutex");

    // True if we can make direct calls to gSKI to optimize I/O
    protected boolean m_bIsDirectConnection ;

    // True if we want to dump debug info about messages sent and received.
    protected boolean m_bTraceCommunications ;

    // True if this connection object is on the kernel side of the conversation
    // This has no effect on the logic, but can be helpful for debugging
    protected boolean m_bIsKernelSide ;

    // A client needs to get this mutex before sending and receiving messages.
    // This allows us to use a separate thread in the client to keep connections
    // alive even when the client itself goes to sleep.
    protected String m_ClientMutex = new String("m_ClientMutex");

    // This information can be requested and set by clients, so one client can
    // find out who else is connected.
    protected String m_ID ;          // Unique ID, machine generated (by kernel)
    protected String m_Name ;        // Name, optionally set by client (e.g. debugger)
    protected String m_Status ;      // Status, optionally set by client from fixed set of values
    protected String m_AgentStatus ; // Agent status, referring to last created agent.  Similar to connection status above.

    // The value to use for this connection's client side time tags (so each connection can have its own part of the id space)
    protected long        m_InitialTimeTagCounter ;

    // High resolution timer -- useful for profiling
    // TODO soar_thread::OSSpecificTimer*   m_pTimer ;
    protected double                          m_IncomingTime ;
    protected double                          m_OutgoingTime ;

    public Connection()
    {
        m_MessageID = 0 ;
/*
        #ifdef _DEBUG
            // It's useful to start somewhere other than 0 in debug, especially when dealing with
            // remote connections, so it's clear which id's come from which client.
            int rand = (int)(clock() % 10) ;
            m_MessageID = 100 * rand ;
        #endif
*/
        m_InitialTimeTagCounter = 0 ;
        m_pUserData = null ;
        m_bIsDirectConnection = false ;
        m_bTraceCommunications = false ;
        m_bIsKernelSide = false ;

        // TODO m_pTimer = soar_thread::MakeTimer() ;
        m_IncomingTime = 0.0 ;
        m_OutgoingTime = 0.0 ;
    }
    
    public void delete()
    {
        // Delete all callback objects
        m_CallbackMap.clear();

        // Clear out any messages sitting on the queues
        m_IncomingMessageQueue.clear();

        // TODO delete m_pTimer ;
        
    }

    /*************************************************************
    * @brief Creates a connection to a receiver that is embedded
    *        within the same process.
    *
    * @param pLibraryName   The name of the library to load, without an extension (e.g. "SoarKernelSML").  Case-sensitive (to support Linux).
    *                       This library will be dynamically loaded and connected to.
    * @param ClientThread   If true, Soar will run in the client's thread and the client must periodically call over to the
    *                       kernel to check for incoming messages on remote sockets.
    *                       If false, Soar will run in a thread within the kernel and that thread will check the incoming sockets itself.
    *                       However, this kernel thread model requires a context switch whenever commands are sent to/from the kernel.
    * @param Optimized      If this is a client thread connection, we can short-circuit parts of the messaging system for sending input and
    *                       running Soar.  If this flag is true we use those short cuts.  If you're trying to debug the SML libraries
    *                       you may wish to disable this option (so everything goes through the standard paths).  Has no affect if not running on client thread.
    * @param port           The port number the server should use to receive remote connections.  The default port for SML is 12121 (picked at random).
    *
    * @param pError         Pass in a pointer to an int and receive back an error code if there is a problem.
    * @returns An EmbeddedConnection instance.
    *************************************************************/
    public static Connection CreateEmbeddedConnection(String pLibraryName, boolean clientThread, boolean optimized, 
            int portToListenOn /*= kDefaultSMLPort*/, ByRef<ErrorCode> pError /*= NULL*/)
    {
        // Set an initial error code and then replace it if something goes wrong.
        if (pError != null) pError.value = ErrorCode.kNoError ;

        // We also use the terms "synchronous" and "asynchronous" for client thread or kernel thread.
        EmbeddedConnection pConnection = clientThread ?
                                        EmbeddedConnectionSynch.CreateEmbeddedConnectionSynch() :
                                        EmbeddedConnectionAsynch.CreateEmbeddedConnectionAsynch() ;

        pConnection.AttachConnection(pLibraryName, optimized, portToListenOn) ;

        // Report any errors
        if (pError != null) pError.value = pConnection.GetLastError() ;

        return pConnection ;
    }

    /*************************************************************
    * @brief Creates a connection to a receiver that is in a different
    *        process.  The process can be on the same machine or a different machine.
    *
    * @param sharedFileSystem   If true the local and remote machines can access the same set of files.
    *                   For example, this means when loading a file of productions, sending the filename is
    *                   sufficient, without actually sending the contents of the file.
    *                   (NOTE: It may be a while before we really support passing in 'false' here)
    * @param pIPaddress The IP address of the remote machine (e.g. "202.55.12.54").
    *                   Pass "127.0.0.1" to create a connection between two processes on the same machine.
    * @param port       The port number to connect to.  The default port for SML is 12121 (picked at random).
    * @param pError     Pass in a pointer to an int and receive back an error code if there is a problem.  (Can pass NULL).
    *
    * @returns A RemoteConnection instance.
    *************************************************************/
    public static Connection CreateRemoteConnection(boolean sharedFileSystem, String pIPaddress, 
            int port /*= kDefaultSMLPort*/, ByRef<ErrorCode> pError /*= NULL*/)
    {
        return null;
        // TODO RemoteConnection pConnection;
/*
        #ifdef ENABLE_NAMED_PIPES
            if(pIPaddress == 0) {

                sock::ClientNamedPipe* pNamedPipe = new sock::ClientNamedPipe() ;

                std::stringstream name;
                name << port;
                
                bool ok = pNamedPipe->ConnectToServer(name.str().c_str()) ;

                if(!ok) {
                    if(pError) *pError = Error::kConnectionFailed ;
                    return NULL;
                }

                // Wrap the pipe inside a remote connection object
                pConnection = new RemoteConnection(sharedFileSystem, pNamedPipe) ;

            } else
        #endif
*/
        /* TODO ConnectToServer
            {
                sock::ClientSocket* pSocket = new sock::ClientSocket() ;

                boolean ok = pSocket->ConnectToServer(pIPaddress, port) ;

                if (!ok)
                {
                    // BADBAD: Can we get a more detailed error from pSocket?
                    if (pError) *pError = Error::kConnectionFailed ;
                    delete pSocket ;
                    return NULL ;
                }
                
                // Wrap the socket inside a remote connection object
                pConnection = new RemoteConnection(sharedFileSystem, pSocket) ;
            }
*/
           // TODO return pConnection ;
        
    }

    /*************************************************************
    * @brief Create a new connection object wrapping a socket.
    *        The socket is generally obtained from a ListenerSocket.
    *        (Clients don't generally use this method--use the one above instead)
    *************************************************************/
// TODO    public static Connection CreateRemoteConnection(sock::DataSender* pDataSender)
//    {
//        // This is a server side connection, so it doesn't have any way to know
//        // if the client and it share the same file system, so just set it to true by default.
//        return new RemoteConnection(true, pDataSender) ;        
//    }

    /*************************************************************
    * @brief Shuts down this connection.
    *************************************************************/
    public abstract void CloseConnection();

    /*************************************************************
    * @brief Returns true if this connection has been closed or
    *        is otherwise not usable.
    *************************************************************/
    public abstract boolean IsClosed();

    /*************************************************************
    * @brief Returns true if this is a remote connection (i.e. over a socket,
    *        may in fact be on the same machine).
    *************************************************************/
    public abstract boolean IsRemoteConnection();

    /*************************************************************
    * @brief Returns true if messages are queued and executed on receiver's thread.
    *        (Always true for a remote connection.  May be true or false for
    *        an embedded connection, depending on how it was created).
    *************************************************************/
    public abstract boolean IsAsynchronous();

    /*************************************************************
    * @brief Returns true if direct access to gSKI is available.
    *        This allows us to optimize I/O calls by calling directly
    *        to gSKI (and hence the kernel) without using the messaging system at all.
    *        The direct connection is only true if this is a synchronous embedded connection.
    *************************************************************/
    public boolean IsDirectConnection() { return m_bIsDirectConnection ; }

    /*************************************************************
    * @brief Print out debug information about the messages we are sending and receiving.
    *        Currently only affects remote connections, but we may extend things.
    *************************************************************/
    public void        SetTraceCommunications(boolean state)  { m_bTraceCommunications = state ; }
    public boolean        IsTracingCommunications()           { return m_bTraceCommunications ; }

    /*************************************************************
    * @brief True if this connection is from the kernel to the client (false if other way, from client to kernel).
    *        This has no impact on the logic but can help with debugging.
    *************************************************************/
    public void        SetIsKernelSide(boolean state) { m_bIsKernelSide = state ; }
    public boolean        IsKernelSide()              { return m_bIsKernelSide ; }

    /*************************************************************
    * @brief Send a message to the SML receiver (e.g. from the environment to the Soar kernel).
    *        The error code that is returned indicates whether the command was successfully sent,
    *        not whether the command was interpreted successfully by Soar.
    *
    * @param pMsg   The message (as an object representing XML) that is to be sent.
    *               The caller should release this message object after making the send call
    *               once it if finished using it.
    *************************************************************/
    public abstract void SendMsg(ElementXML pMsg);

    /*************************************************************
    * @brief Retrieve any commands, notifications, responses etc. that are waiting.
    *        Messages that are received are routed to callback functions in the client for processing.
    *
    *        This call never blocks.  In an embedded situation, this does nothing as incoming messages are
    *        sent directly to the callback functions.
    *        In a remote situation, the client must call this function periodically, to read incoming messages
    *        on the socket.
    *
    *        We use a callback model (rather than retrieving each message in turn here) so that the embedded model and
    *        the remote model are closer to each other.
    *
    * @param allMessages    If false, only retrieves at most one message before returning, otherwise gets all messages.
    * @return   True if read at least one message.
    *************************************************************/
    public abstract boolean ReceiveMessages(boolean allMessages);

    /*************************************************************
    * @brief Retrieve the response to the last call message sent.
    *
    *        In an embedded situation, this result is always immediately available and the "wait" parameter is ignored.
    *        In a remote situation, if wait is false and the result is not immediately available this call returns false.
    *
    *        The ID is only required when the client is remote (because then there might be many responses waiting on the socket).
    *        A message can only be retrieved once, so a second call with the same ID will return NULL.
    *        Only the response to the last call message can be retrieved.
    *
    *        The client is not required to call to get the result of a command it has sent.
    *
    *        The implementation of this function will call ReceiveMessages() to get messages one at a time and process them.  Thus callbacks may be
    *        invoked while the client is blocked waiting for the particular response they requested.
    *
    *        A response that is returned to the client through GetResultOfMessage() will not be passed to a callback
    *        function registered for response messages.  This allows a client to register a general function to check for
    *        any error messages and yet retrieve specific responses to calls that it is particularly interested in.
    *
    * @param pID    The id of the original SML message (the id is a attribute in the top level [sml] tag)
    * @param wait   If true wait until the result is received (or we time out and report an error).
    *
    * @returns The message that is a response to pID or NULL if none is found.
    *************************************************************/
    public abstract ElementXML GetResponseForID(String pID, boolean wait);

    /*************************************************************
    * @brief Retrieve the response to the last call message sent.
    *
    *        In an embedded situation, this result is always immediately available and the "wait" parameter is ignored.
    *        In a remote situation, if wait is false and the result is not immediately available this call returns false.
    *
    *        The message is only required when the client is remote (because then there might be many responses waiting on the socket).
    *        A message can only be retrieved once, so a second call with the same ID will return NULL.
    *        Only the response to the last call message can be retrieved.
    *
    *        The client is not required to call to get the result of a command it has sent.
    *
    *        The implementation of this function will call ReceiveMessages() to get messages one at a time and process them.  Thus callbacks may be
    *        invoked while the client is blocked waiting for the particular response they requested.
    *
    *        A response that is returned to the client through GetResultOfMessage() will not be passed to a callback
    *        function registered for response messages.  This allows a client to register a general function to check for
    *        any error messages and yet retrieve specific responses to calls that it is particularly interested in.
    *
    * @param pMsg   The original SML message that we wish to get a response from.
    * @param wait   If true wait until the result is received (or we time out and report an error).
    *
    * @returns The message that is a response to pMsg or NULL if none is found.
    *************************************************************/
    public ElementXML GetResponse(ElementXML pXML)
    {
        return GetResponse(pXML, true);
    }
    public ElementXML GetResponse(ElementXML pXML, boolean wait /*= true*/)
    {
        if (pXML == null)
        {
            SetError(ErrorCode.kInvalidArgument) ;
            return null ;
        }

        String pID = pXML.GetAttribute(sml_Names.getKID()) ;

        if (pID == null)
        {
            SetError(ErrorCode.kArgumentIsNotSML) ;
            return null ;
        }

        return GetResponseForID(pID, wait) ;
        
    }

    /*************************************************************
    * @brief Register a callback for a particular type of incoming message.
    *
    *        Messages are currently one of:
    *        "call", "response" or "notify"
    *        A call is always paired to a response (think of this as a remote function call that returns a value)
    *        while a notify does not receive a response (think of this as a remote function call that does not return a value).
    *        This type is stored in the "doctype" attribute of the top level SML node in the message.
    *        NOTE: doctype's are case sensitive.
    *
    *        You MUST register a callback for the "call" type of message.  This callback must return a "response" message which is then
    *        sent back over the connection.  Other callbacks should not return a message.
    *        Once the returned message has been sent it will be deleted.
    *
    *        We will maintain a list of callbacks for a given type of SML document and call each in turn.
    *        Each callback on the list will be called in turn until one returns a non-NULL response.  No further callbacks
    *        will be called for that message.  This ensures that only one response is sent to a message.
    *
    * @param callback   The function to call when an incoming message is received (of the right type)
    * @param pUserData  This data is passed to the callback.  It allows the callback to have some context to work in.  Can be NULL.
    * @param pType      The type of message to register for (currently one of "call", "response" or "notify").
    * @param addToEnd   If true add the callback to the end of the list (called last).  If false, add to front where it will be called first.
    *************************************************************/
    public void RegisterCallback(IncomingCallback callback, Object pUserData, String pType, boolean addToEnd)
    {
        ClearError() ;

        if (callback == null || pType == null)
        {
            SetError(ErrorCode.kInvalidArgument) ;
            return ;
        }

        // Create the callback object to be stored in the map
        Callback pCallback = new Callback(this, callback, pUserData) ;

        LinkedList<Callback> pList = m_CallbackMap.get(pType);

        if(pList == null)
        {
            // Need to create the list
            pList = new LinkedList<Callback>() ;
            m_CallbackMap.put(pType, pList);
        }
        
        // Add the callback to the list
        if (addToEnd)
            pList.add(pCallback) ;
        else
            pList.push(pCallback) ;
        
    }

    /*************************************************************
    * @brief Removes a callback from the list of callbacks for a particular type of incoming message.
    *
    * @param callback   The function that was previously registered.  If NULL removes all callbacks for this type of message.
    * @param pType      The type of message to unregister from (currently one of "call", "response" or "notify").
    *************************************************************/
    public void UnregisterCallback(IncomingCallback callback, String pType)
    {
        ClearError() ;

        if (pType == null)
        {
            SetError(ErrorCode.kInvalidArgument) ;
            return ;
        }

        // See if we have a list of callbacks for this type
        LinkedList<Callback> pList = GetCallbackList(pType) ;

        if (pList == null)
        {
            SetError(ErrorCode.kCallbackNotFound) ;
            return ;
        }

        if (callback == null)
        {
            // Caller asked to delete all callbacks for this type
            m_CallbackMap.remove(pType);
            return ;
        }

        boolean found = false;
        Iterator<Callback> it = pList.iterator();
        while(it.hasNext())
        {
            Callback pCallback = it.next();
            if(pCallback.getFunction() == callback)
            {
                it.remove();
                found = true;
            }
        }

        if (!found)
            SetError(ErrorCode.kCallbackNotFound) ;
        
    }

    /*************************************************************
    * @brief Invoke the list of callbacks matching the doctype of the incoming message.
    *
    * @param pIncomingMsg   The SML message that should be passed to the callbacks.
    *
    * @returns The response message (or NULL if there is no response from any calback).
    *************************************************************/
    public ElementXML InvokeCallbacks(ElementXML pIncomingMsg)
    {
        ClearError() ;

        MessageSML pIncomingSML = (MessageSML)pIncomingMsg ;

        // Check that we were passed a valid message.
        if (pIncomingMsg == null)
        {
            SetError(ErrorCode.kInvalidArgument) ;
            return null ;
        }

        // Retrieve the type of this message
        String pType = pIncomingSML.GetDocType() ;

        // Check that this message has a valid doc type (all valid SML do)
        if (pType == null)
        {
            SetError(ErrorCode.kNoDocType) ;
            return null ;
        }

        // Decide if this message is a "call" which requires a "response"
        boolean isIncomingCall = pIncomingSML.IsCall() ;

        // See if we have a list of callbacks for this type
        LinkedList<Callback> pList = GetCallbackList(pType) ;

        // Nobody was interested in this type of message, so we're done.
        if (pList == null)
        {
            return null ;
        }

        // Walk the list of callbacks in turn until we reach
        // the end or one returns a message.
        for(Callback pCallback : new ArrayList<Callback>(pList))
        {
            ElementXML pResponse = pCallback.Invoke(pIncomingMsg) ;

            if (pResponse != null)
            {
                if (isIncomingCall)
                    return pResponse ;

                // This callback was not for a call and should not return a result.
                // Delete the result and ignore it.
//                pResponse.ReleaseRefOnHandle() ;
//                pResponse = NULL ;
            }
        }

        // If this is a call, we must respond
        if (isIncomingCall)
            SetError(ErrorCode.kNoResponseToCall) ;

        // Nobody returned a response
        return null ;
        
    }

    /*************************************************************
    * @brief Get and set the user data.
    *
    * The client or kernel may wish to keep state information (e.g. a pointer into gSKI for the kernel) with the
    * connection.  If so, it can store it here.  The caller retains ownership of this object, so it won't be
    * deleted when the connection is deleted.
    *************************************************************/
    public void    SetUserData(Object pUserData)    { m_pUserData = pUserData ; }
    public Object   GetUserData()                   { return m_pUserData ; }

    /*************************************************************
    * @brief Get and set id, name and status
    * ID - unique machine generated id (created by kernel)
    * Name   - optional, set by client.  Should always be the same for a given client (e.g. debugger/java-toh etc.)
    * Status - optional, set by client from fixed list of values
    * Agent status - optional, set by client and refers to last created agent (set to "created" initially by kernel).
    *************************************************************/
    public String GetID()                 { return m_ID ; }
    public void SetID(String pID)         { m_ID = pID ; }
    public String GetName()               { return m_Name ; }
    public void SetName(String pName)     { m_Name = pName ; }
    public String GetStatus()             { return m_Status ; }
    public void SetStatus(String pStatus) { m_Status = pStatus ; }
    public String GetAgentStatus()        { return m_AgentStatus ; }
    public void SetAgentStatus(String pStatus) { m_AgentStatus = pStatus ; }

    /*************************************************************
    * @brief Send a message and get the response.
    *
    * @param pAnalysis  This will be filled in with the analyzed response
    * @param pMsg       The message to send
    * @returns          True if got a reply
    *************************************************************/
    public boolean SendMessageGetResponse(AnalyzeXML pAnalysis, ElementXML pMsg)
    {
        // If the connection is already closed, don't do anything
        if (IsClosed())
            return false ;

        // Make sure only one thread is sending messages at a time
        // (This allows us to run a separate thread in clients polling for events even
        //  when the client is sleeping, but we don't want them both to be sending/receiving at the same time).
        synchronized(m_ClientMutex)
        {

        // Send the command over.
        SendMsg(pMsg);

        // There was an error in the send, so we're done.
        if (HadError())
        {
            return false ;
        }

        // Get the response
        ElementXML pResponse = GetResponse(pMsg) ;

        // These was an error in getting the response
        if (HadError())
            return false ;

        if (pResponse == null)
        {
            // We failed to get a reply when one was expected
            SetError(ErrorCode.kFailedToGetResponse) ;
            return false ;
        }

        // Analyze the response and return the analysis
        pAnalysis.Analyze(pResponse) ;
/*
    #ifdef _DEBUG
        char* pMsgText = pResponse->GenerateXMLString(true) ;
        pResponse->DeleteString(pMsgText) ;
    #endif
*/
        pResponse.delete();

        // If the response is not SML, return false
        if (!pAnalysis.IsSML())
        {
            SetError(ErrorCode.kResponseIsNotSML) ;
            return false ;
        }

        // If we got an error, return false.
        if (pAnalysis.GetErrorTag() != null)
        {
            SetError(ErrorCode.kSMLErrorMessage) ;
            return false ;
        }

        return true ;    
        }
    }

    /*************************************************************
    * @brief Build an SML message and send it over the connection
    *        returning the analyzed version of the response.
    *
    * This family of commands are designed for access based on
    * a named agent.  This agent's name is passed as the first
    * parameter and then the other parameters define the details
    * of which method to call for the agent.
    * 
    * Passing NULL for the agent name is valid and indicates
    * that the command is not agent specific (e.g. "shutdown-kernel"
    * would pass NULL).
    *
    * Uses SendMessageGetResponse() to do its work.
    *
    * @param pResponse      The response from the kernel to this command.
    * @param pCommandName   The command to execute
    * @param pAgentName     The name of the agent this command is going to (can be NULL -> implies going to top level of kernel)
    * @param pParamName1    The name of the first argument for this command
    * @param pParamVal1     The value of the first argument for this command
    * @param rawOuput       If true, sends back a simple string form for the result which the caller will probably just print.
    *                       If false, sendds back a structured XML object that the caller can analyze and do more with.
    * @returns  True if command was sent and received back without any errors (either in sending or in executing the command).
    *************************************************************/
    public boolean SendAgentCommand(AnalyzeXML pResponse, String pCommandName, boolean rawOutput /*= false*/)
    {
        ElementXML pMsg = CreateSMLCommand(pCommandName, rawOutput) ;

        boolean result = SendMessageGetResponse(pResponse, pMsg) ;

        pMsg.delete() ;
        
        return result ;
        
    }

    public boolean SendAgentCommand(AnalyzeXML pResponse, String pCommandName, String pAgentName)
    {
        return SendAgentCommand(pResponse, pCommandName, pAgentName, false);
    }
    public boolean SendAgentCommand(AnalyzeXML pResponse, String pCommandName, String pAgentName, boolean rawOutput /*= false*/)
    {
        ElementXML pMsg = CreateSMLCommand(pCommandName, rawOutput) ;

        //add the agent name parameter
        if (pAgentName != null)
            AddParameterToSMLCommand(pMsg, sml_Names.getKParamAgent(), pAgentName);

        boolean result = SendMessageGetResponse(pResponse, pMsg) ;

        pMsg.delete();
        
        return result ;        
    }

    public boolean SendAgentCommand(AnalyzeXML pResponse, String pCommandName, String pAgentName,
                     String pParamName1, String pParamVal1, boolean rawOutput /*= false*/)
    {
        ElementXML pMsg = CreateSMLCommand(pCommandName, rawOutput) ;

        //add the agent name parameter
        if (pAgentName != null)
            AddParameterToSMLCommand(pMsg, sml_Names.getKParamAgent(), pAgentName);

        // add the other parameters
        AddParameterToSMLCommand(pMsg, pParamName1, pParamVal1);

        boolean result = SendMessageGetResponse(pResponse, pMsg) ;

        pMsg.delete();
        
        return result ;
        
    }

    public boolean SendAgentCommand(AnalyzeXML pResponse, String pCommandName, String pAgentName,
                     String pParamName1, String pParamVal1,
                     String pParamName2, String pParamVal2, boolean rawOutput /*= false*/)
    {
        ElementXML pMsg = CreateSMLCommand(pCommandName, rawOutput) ;

        //add the agent name parameter
        if (pAgentName != null)
            AddParameterToSMLCommand(pMsg, sml_Names.getKParamAgent(), pAgentName);

        // add the other parameters
        AddParameterToSMLCommand(pMsg, pParamName1, pParamVal1);
        AddParameterToSMLCommand(pMsg, pParamName2, pParamVal2);

        boolean result = SendMessageGetResponse(pResponse, pMsg) ;

        pMsg.delete();
        
        return result ;
        
    }

    public boolean SendAgentCommand(AnalyzeXML pResponse, String pCommandName, String pAgentName,
                     String pParamName1, String pParamVal1,
                     String pParamName2, String pParamVal2,
                     String pParamName3, String pParamVal3, boolean rawOutput /*= false*/)
    {
        ElementXML pMsg = CreateSMLCommand(pCommandName, rawOutput) ;

        //add the agent name parameter
        if (pAgentName != null)
            AddParameterToSMLCommand(pMsg, sml_Names.getKParamAgent(), pAgentName);

        // add the other parameters
        AddParameterToSMLCommand(pMsg, pParamName1, pParamVal1);
        AddParameterToSMLCommand(pMsg, pParamName2, pParamVal2);
        AddParameterToSMLCommand(pMsg, pParamName3, pParamVal3);

        boolean result = SendMessageGetResponse(pResponse, pMsg) ;

        pMsg.delete();
        
        return result ;
        
    }

    /*************************************************************
    * @brief Build an SML message and send it over the connection
    *        returning the analyzed version of the response.
    *
    * This family of commands are designed for an object model access
    * to the kernel (e.g. using the gSKI interfaces).
    * In this model, the first parameter is always an indentifier
    * representing the "this" pointer.  The name of the command gives
    * the method name (in some manner) and the other parameters
    * define the arguments to the method.
    *
    * As of this writing, we are largely moving away from this model,
    * but the code is still here in case it has value in the future.
    *
    * Uses SendMessageGetResponse() to do its work.
    *************************************************************/
    public boolean SendClassCommand(AnalyzeXML pResponse, String pCommandName)
    {
        ElementXML pMsg = CreateSMLCommand(pCommandName) ;

        boolean result = SendMessageGetResponse(pResponse, pMsg) ;

        pMsg.delete();
        
        return result ;
        
    }

    public boolean SendClassCommand(AnalyzeXML pResponse, String pCommandName, String pThisID)
    {
        ElementXML pMsg = CreateSMLCommand(pCommandName) ;

        //add the 'this' pointer parameter
        AddParameterToSMLCommand(pMsg, sml_Names.getKParamThis(), pThisID);

        boolean result = SendMessageGetResponse(pResponse, pMsg) ;

        pMsg.delete();
        
        return result ;
        
    }

    public boolean SendClassCommand(AnalyzeXML pResponse, String pCommandName, String pThisID,
                     String pParamName1, String pParamVal1)
    {
        ElementXML pMsg = CreateSMLCommand(pCommandName) ;

        //add the 'this' pointer parameter
        AddParameterToSMLCommand(pMsg, sml_Names.getKParamThis(), pThisID);
        if (pParamVal1 != null) AddParameterToSMLCommand(pMsg, pParamName1, pParamVal1);

        boolean result = SendMessageGetResponse(pResponse, pMsg) ;

        pMsg.delete();
        
        return result ;
        
    }

    public boolean SendClassCommand(AnalyzeXML pResponse, String pCommandName, String pThisID,
                     String pParamName1, String pParamVal1,
                     String pParamName2, String pParamVal2)
    {
        ElementXML pMsg = CreateSMLCommand(pCommandName) ;

        //add the 'this' pointer parameter
        AddParameterToSMLCommand(pMsg, sml_Names.getKParamThis(), pThisID);

        // Note: If first param is missing, second must be ommitted too (normal optional param syntax)
        if (pParamVal1 != null)               AddParameterToSMLCommand(pMsg, pParamName1, pParamVal1);
        if (pParamVal1 != null && pParamVal2 != null) AddParameterToSMLCommand(pMsg, pParamName2, pParamVal2);

        boolean result = SendMessageGetResponse(pResponse, pMsg) ;

        pMsg.delete();
        
        return result ;
        
    }

    public boolean SendClassCommand(AnalyzeXML pResponse, String pCommandName, String pThisID,
                     String pParamName1, String pParamVal1,
                     String pParamName2, String pParamVal2,
                     String pParamName3, String pParamVal3)
    {
        ElementXML pMsg = CreateSMLCommand(pCommandName) ;

        //add the 'this' pointer parameter
        AddParameterToSMLCommand(pMsg, sml_Names.getKParamThis(), pThisID);
        
        if (pParamVal1 != null)                             AddParameterToSMLCommand(pMsg, pParamName1, pParamVal1);
        if (pParamVal1 != null && pParamVal2 != null)               AddParameterToSMLCommand(pMsg, pParamName2, pParamVal2);
        if (pParamVal1 != null && pParamVal2 != null && pParamVal3 != null) AddParameterToSMLCommand(pMsg, pParamName3, pParamVal3);

        boolean result = SendMessageGetResponse(pResponse, pMsg) ;

        pMsg.delete();
        
        return result ;
        
    }

    /*************************************************************
    * @brief Returns the error status from the last function called.
    *        0 if successful, otherwise an error code to indicate what went wrong.
    *************************************************************/
    ErrorCode GetLastError() { return m_ErrorCode ; }

    public boolean      HadError()     { return m_ErrorCode != ErrorCode.kNoError ; } 

    /*************************************************************
    * @brief Creates a new ID that's unique for this generator.
    *
    * @returns The new ID.
    *************************************************************/
    public int GenerateID() { return m_MessageID++ ; }

    /*************************************************************
    * @brief Create a basic SML message, with the top level <sml> tag defined
    *        together with the version, doctype, soarVersion and id filled in.
    *
    * Use this call if you plan on building up a message manually and would like
    * a little help getting started.
    *
    * @param pType  The type of message (currently one of "call", "response" or "notify").
    *               Think of a call as a remote function call that returns a value (the response).
    *               Think of a notify as a remote function call that does not return a value.
    *
    * @returns The new SML message
    *************************************************************/
    public ElementXML CreateSMLMessage(String pType)
    {
        MessageSML pMsg = new MessageSML() ;
        pMsg.SetID(GenerateID()) ;
        pMsg.SetDocType(pType) ;

        return pMsg ;
    }

    /*************************************************************
    * @brief Create a basic SML command message.  You should then add parameters to this command.
    *        This function calls CreateSMLMessage() and then adds a <command> tag to it.
    *        E.g. the command might be "excise -production" so the name of the command is "excise".
    *        Then add parameters to this.
    *
    * @param pName  The name of the command (the meaning depends on whoever receives this command).
    * @param rawOutput  If true, results from command will be a string wrapped in a <raw> tag, rather than full structured XML. (Defaults to false).
    * 
    * @returns The new SML command message.
    *************************************************************/
    public ElementXML CreateSMLCommand(String pCommandName) { return CreateSMLCommand(pCommandName, false); }
    public ElementXML CreateSMLCommand(String pName, boolean rawOutput /*= false*/)
    {
        // Create a new call message
        MessageSML pMsg = new MessageSML(MessageSML.DocType.kCall, GenerateID()) ;

        // Create the command tag
        TagCommand pCommand = new TagCommand() ;
        pCommand.SetName(pName) ;

        if (rawOutput)
            pCommand.AddAttribute(sml_Names.getKCommandOutput(), sml_Names.getKRawOutput()) ;

        pMsg.AddChild(pCommand) ;

        return pMsg ;
        
    }

    /*************************************************************
    * @brief Add a parameter to an SML command message.
    *
    * The type of the value is optional as presumably the recipient knows how to parse it.
    *
    * @param pCommand   An existing SML command message.
    * @param pName      The name of this parameter (can't be NULL).
    * @param pValue     The value of this parameter (represented as a string).  Can be empty, can't be NULL.
    * @param pValueType The type of the value (e.g. "int" or "string".  Anything can go here as long as the recipient understands it) (usually will be NULL).
    * 
    * @returns Pointer to the ElementXML_Handle for the <command> tag (not the full message, just the <command> part)
    *          This is rarely needed, but could be used to optimize the code.  DO NOT release this handle.
    *************************************************************/
    public ElementXML /*_Handle*/ AddParameterToSMLCommand(ElementXML pCommand, String pName, String pValue)
    {
        return AddParameterToSMLCommand(pCommand, pName, pValue, null);
    }
    public ElementXML /*_Handle*/ AddParameterToSMLCommand(ElementXML pMsg, String pName, String pValue, String pValueType /*= NULL*/)
    {
        ClearError() ;
/*
        #ifdef DEBUG
            if (!pName || !pValue)
            {
                SetError(Error::kNullArgument) ;
                return NULL ;
            }

            if (!pMsg->IsTag(sml_Names::kTagSML))
            {
                SetError(Error::kArgumentIsNotSML) ;
                return NULL ;
            }
        #endif
*/
            // Get the command object
            ElementXML command = new ElementXML(null) ;
            ElementXML pCommand = command ;
            boolean found = pMsg.GetChild(pCommand, 0) ;
/*
        #ifdef DEBUG
            if (!found || !pCommand->IsTag(sml_Names::kTagCommand))
            {
                SetError(Error::kSMLHasNoCommand) ;
                return NULL ;
            }
        #else
            unused(found) ;
        #endif
*/
            // Create the arg tag
            TagArg pArg = new TagArg() ;

            pArg.SetParam(pName) ;
            pArg.SetValue(pValue) ;
            
            if (pValueType != null)
                pArg.SetType(pValueType) ;

            pCommand.AddChild(pArg) ;

            return pCommand;
        
    }

    /*************************************************************
    * @brief Create a basic SML response message.  You should then add content to this response.
    *        This function calls CreateSMLMessage() and fills in the appropriate "ack" attribute
    *        to respond to the incoming message.
    *
    * @param pIncomingMsg   The original message that we are responding to.
    * 
    * @returns The new SML response message.
    *************************************************************/
    public ElementXML CreateSMLResponse(ElementXML pIncomingMsg)
    {
        ClearError() ;
/*
        #ifdef DEBUG
            if (!pIncomingMsg)
            {
                SetError(Error::kNullArgument) ;
                return NULL ;
            }

            if (!pIncomingMsg->IsTag(sml_Names::kTagSML))
            {
                SetError(Error::kArgumentIsNotSML) ;
                return NULL ;
            }

            MessageSML* pIncomingSML = (MessageSML*)pIncomingMsg ;

            if (!pIncomingSML->GetID())
            {
                SetError(Error::kArgumentIsNotSML) ;
                return NULL ;
            }
        #endif
*/
            // Create a new response message
            MessageSML pMsg = new MessageSML(MessageSML.DocType.kResponse, GenerateID()) ;

            // Messages must have an ID and we use that as the response
            String pAck = ((MessageSML)pIncomingMsg).GetID() ;

            // Add an "ack=<id>" field to the response so the caller knows which
            // message we are responding to.
            pMsg.AddAttribute(sml_Names.getKAck(), pAck) ;

            return pMsg ;        
    }

    /*************************************************************
    * @brief Adds an <error> tag and an error message to a response message.
    *
    * @param pResponse  The response message we are adding an error to.
    * @param pErrorMsg  A description of the error in a form presentable to the user
    * @param errorCode  An optional numeric code for the error (to support programmatic responses to the error)
    *************************************************************/
    public  void AddErrorToSMLResponse(ElementXML pResponse, String pErrorMsg, int errorCode /*= -1*/)
    {
        ClearError() ;
/*
        #ifdef DEBUG
            if (!pResponse || !pErrorMsg)
            {
                SetError(Error::kNullArgument) ;
                return ;
            }

            if (!pResponse->IsTag(sml_Names::kTagSML))
            {
                SetError(Error::kArgumentIsNotSML) ;
                return ;
            }
        #endif
*/
            // Create a result tag so if you only
            // check for results you still get an error message.
            // Also add an error tag, so we can distinguish between
            // errors and success based on message structure.
            TagResult pTag = new TagResult() ;

            pTag.SetCharacterData(pErrorMsg) ;
            pTag.AddAttribute(sml_Names.getKCommandOutput(), sml_Names.getKRawOutput()) ;

            pResponse.AddChild(pTag) ;

            // Create the error tag
            TagError pError = new TagError() ;

            pError.SetDescription(pErrorMsg) ;

            if (errorCode != -1)
                pError.SetErrorCode(errorCode) ;

            pResponse.AddChild(pError) ;        
    }

    /*************************************************************
    * @brief Adds a <result> tag and fills in character data for that result.
    *
    * @param pResponse  The response message we are adding an error to.
    * @param pResult    The result (as a text string)
    *************************************************************/
    public void AddSimpleResultToSMLResponse(ElementXML pResponse, String pResult)
    {
        ClearError() ;
/*
        #ifdef DEBUG
            if (!pResponse || !pResult)
            {
                SetError(Error::kNullArgument) ;
                return ;
            }

            if (!pResponse->IsTag(sml_Names::kTagSML))
            {
                SetError(Error::kArgumentIsNotSML) ;
                return ;
            }
        #endif
*/
            // Create the result tag
            TagResult pTag = new TagResult() ;

            pTag.SetCharacterData(pResult) ;
            pTag.AddAttribute(sml_Names.getKCommandOutput(), sml_Names.getKRawOutput()) ;

            pResponse.AddChild(pTag) ;        
    }

    public void SetInitialTimeTagCounter(int value)   { m_InitialTimeTagCounter = value ; }
    public long GetInitialTimeTagCounter()             { return m_InitialTimeTagCounter ; } 

    public double GetIncomingTime() { return m_IncomingTime ; }
    public double GetOutgoingTime() { return m_OutgoingTime ; }

    /*************************************************************
    * @brief Resets the last error value to 0.
    *************************************************************/
    protected void ClearError()   { m_ErrorCode = ErrorCode.kNoError ; }

    /*************************************************************
    * @brief Set the error code
    *************************************************************/
    protected void SetError(ErrorCode error)  { m_ErrorCode = error ; }

    /*************************************************************
    * @brief Gets the list of callbacks associated with a given doctype (e.g. "call")
    **************************************************************/
    protected LinkedList<Callback> GetCallbackList(String pType)
    {
        return m_CallbackMap.get(pType);
    }

    /*************************************************************
    * @brief Removes the top message from the incoming message queue
    *        in a thread safe way.
    *        Returns NULL if there is no waiting message.
    *************************************************************/
    protected ElementXML PopIncomingMessageQueue()
    {
        // Ensure only one thread is changing the message queue at a time
        // This lock is released when we exit this function.
        synchronized(m_IncomingMutex)
        {

        if (m_IncomingMessageQueue.size() == 0)
            return null ;

        //  Read the first message that's waiting
        ElementXML pIncomingMsg = m_IncomingMessageQueue.pop() ;

       // The guy receiving this expects to delete this object anyway when they're done
        return pIncomingMsg ;
        }
    }


}

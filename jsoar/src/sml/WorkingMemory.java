/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 5, 2008
 */
package sml;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import sml.connection.Connection;

/**
 * @author ray
 */
public class WorkingMemory
{
    Agent      m_Agent ;
    Identifier m_InputLink ;
    Identifier m_OutputLink ; // this is initialized the first time an agent generates output; until then it is null

    // List of changes that are pending to be sent to the kernel
    final DeltaList   m_DeltaList  = new DeltaList();

    // List of changes to output-link since last time client checked
    final OutputDeltaList m_OutputDeltaList = new OutputDeltaList();

    // A temporary list of wme's with no parent identifier
    // Should always be empty at the end of an output call from the kernel.
    List<WMElement>     m_OutputOrphans ;

    void RecordAddition(WMElement pWME)
    {
        // For additions, the delta list does not take ownership of the wme.
        m_OutputDeltaList.AddWME(pWME) ;

        // Mark this wme as having just been added (in case the client would prefer
        // to walk the tree).
        pWME.SetJustAdded(true) ;

        // record timetag . WME mapping for deletion lookup
        m_TimeTagWMEMap.put(pWME.GetTimeTag(), pWME);
    }
    void RecordDeletion(WMElement pWME)
    {
        // This list takes ownership of the deleted wme.
        // When the item is removed from the delta list it will be deleted.
        m_OutputDeltaList.RemoveWME(pWME) ;

        // remove timetag . WME mapping
        m_TimeTagWMEMap.remove( pWME.GetTimeTag() );
    }

    WMElement SearchWmeListForID(List<WMElement> pWmeList, String pID, boolean deleteFromList)
    {
        Iterator<WMElement> it = pWmeList.iterator();
        while(it.hasNext())
        {
            final WMElement pWME = it.next();
            if(pID.equals(pWME.GetIdentifierName()))
            {
                if(deleteFromList)
                {
                    it.remove();
                }
                return pWME;
            }
        }
        return null;
    }

    Map<String, IdentifierSymbol>     m_IdSymbolMap = new TreeMap<String, IdentifierSymbol>();
    Map<Integer, WMElement>       m_TimeTagWMEMap = new TreeMap<Integer, WMElement>();

    // Searches for an identifier object that matches this id.
    IdentifierSymbol   FindIdentifierSymbol(String pID)
    {
        return m_IdSymbolMap.get(pID);
    }
    void                RecordSymbolInMap( IdentifierSymbol pSymbol )
    {
        m_IdSymbolMap.put(pSymbol.GetIdentifierSymbol(), pSymbol);
    }
    void                RemoveSymbolFromMap( IdentifierSymbol pSymbol )
    {
        if(m_Deleting)
        {
            return;
        }
        m_IdSymbolMap.remove(pSymbol.GetIdentifierSymbol());
    }
    boolean                m_Deleting = false; // used when we're being deleted and the maps shouldn't be updated

    // Create a new WME of the appropriate type based on this information.
    WMElement          CreateWME(IdentifierSymbol pParentSymbol, String pID, String pAttribute, String pValue, String pType, int timeTag)
    {
        // Value is an identifier
        if (pType.equals(sml_Names.getKTypeID()))
        {
            IdentifierSymbol pSharedIdentifierSymbol = this.FindIdentifierSymbol( pValue );
            Identifier pNewIdentifier = null;
            if ( pSharedIdentifierSymbol != null )
            {
                pNewIdentifier = new Identifier(GetAgent(), pParentSymbol, pID, pAttribute, pSharedIdentifierSymbol, timeTag);
            }
            else
            {
                pNewIdentifier = new Identifier(GetAgent(), pParentSymbol, pID, pAttribute, pValue, timeTag) ;
            }
            return pNewIdentifier;
        }

        // Value is a string
        if (pType.equals(sml_Names.getKTypeString()))
            return new StringElement(GetAgent(), pParentSymbol, pID, pAttribute, pValue, timeTag) ;

        // Value is an int
        if (pType.equals(sml_Names.getKTypeInt()))
        {
            int value = Integer.valueOf(pValue);
            return new IntElement(GetAgent(), pParentSymbol, pID, pAttribute, value, timeTag) ;
        }

        // Value is a float
        if (pType.equals(sml_Names.getKTypeDouble()))
        {
            double value = Double.valueOf(pValue) ;
            return new FloatElement(GetAgent(), pParentSymbol, pID, pAttribute, value, timeTag) ;
        }

        return null ;
        
    }

// public ...
    WorkingMemory()
    {
        m_InputLink = null;
        m_OutputLink = null;
        m_Agent = null;
        m_Deleting = false;
    }

    public void delete()
    {
        m_Deleting = true;
        m_OutputLink.delete();
        m_InputLink.delete();
    }

    void            SetAgent(Agent pAgent)
    {
        m_Agent = pAgent;
    }
    Agent          GetAgent()        { return m_Agent ; }
    String     GetAgentName()
    {
        return GetAgent().GetAgentName();
    }
    Connection     GetConnection()
    {
        return GetAgent().GetConnection();
    }

    void            ClearOutputLinkChanges()
    {
        // Clear the list, deleting any WMEs that it owns
        m_OutputDeltaList.Clear(true, true, true) ;

        //// We only maintain this information for values on the output link
        //// as the client knows what's happening on the input link (presumably)
        //// as it is controlling the creation of those objects.
        //if (m_OutputLink)
        //{
        //  // Reset the information about how the output link just changed.
        //  // This is definitely being maintained.
        //  m_OutputLink.ClearJustAdded() ;
        //  m_OutputLink.ClearChildrenModified() ;
        //}
        
    }

    OutputDeltaList GetOutputLinkChanges() { return m_OutputDeltaList ; }

    DeltaList      GetInputDeltaList()     { return m_DeltaList ; }

    // These functions are documented in the agent and handled here.
    Identifier     GetInputLink()
    {
        if (m_InputLink == null)
        {
            AnalyzeXML response = new AnalyzeXML();

            if (GetConnection().SendAgentCommand(response, sml_Names.getKCommand_GetInputLink(), GetAgentName()))
            {
                m_InputLink = new Identifier(GetAgent(), response.GetResultString(), GenerateTimeTag()) ;
            }
        }

        return m_InputLink ;
        
    }
    Identifier     GetOutputLink()
    {
        return m_OutputLink;
    }
    StringElement  CreateStringWME(Identifier parent, String pAttribute, String pValue)
    {
        assert(m_Agent == parent.GetAgent()) ;

        StringElement pWME = new StringElement(GetAgent(), parent, parent.GetValueAsString(), pAttribute, pValue, GenerateTimeTag()) ;

        // Record that the identifer owns this new WME
        parent.AddChild(pWME) ;

        /*
    #ifdef SML_DIRECT
        if ( GetConnection().IsDirectConnection() )
        {
            EmbeddedConnection* pConnection = static_cast<EmbeddedConnection*>( GetConnection() );
            pConnection.DirectAddWME_String( m_AgentSMLHandle, parent.GetValueAsString(), pAttribute, pValue, pWME.GetTimeTag());

            // Return immediately, without adding it to the commit list.
            return pWME ;
        }
    #endif
    */

        // Add it to our list of changes that need to be sent to Soar.
        m_DeltaList.AddWME(pWME) ;

        // Commit immediately if we're configured that way (makes life simpler for the client)
        if (IsAutoCommitEnabled())
            Commit() ;

        return pWME ;
    }
    
    IntElement     CreateIntWME(Identifier parent, String pAttribute, int value)
    {
        assert(m_Agent == parent.GetAgent()) ;

        IntElement pWME = new IntElement(GetAgent(), parent, parent.GetValueAsString(), pAttribute, value, GenerateTimeTag()) ;

        // Record that the identifer owns this new WME
        parent.AddChild(pWME) ;
/*
    #ifdef SML_DIRECT
        if ( GetConnection().IsDirectConnection() )
        {
            EmbeddedConnection* pConnection = static_cast<EmbeddedConnection*>( GetConnection() );
            pConnection.DirectAddWME_Int( m_AgentSMLHandle, parent.GetValueAsString(), pAttribute, value, pWME.GetTimeTag());

            // Return immediately, without adding it to the commit list.
            return pWME ;
        }
    #endif
*/
        // Add it to our list of changes that need to be sent to Soar.
        m_DeltaList.AddWME(pWME) ;

        // Commit immediately if we're configured that way (makes life simpler for the client)
        if (IsAutoCommitEnabled())
            Commit() ;

        return pWME ;

    }
    FloatElement   CreateFloatWME(Identifier parent, String pAttribute, double value)
    {
        assert(m_Agent == parent.GetAgent()) ;

        FloatElement pWME = new FloatElement(GetAgent(), parent, parent.GetValueAsString(), pAttribute, value, GenerateTimeTag()) ;

        // Record that the identifer owns this new WME
        parent.AddChild(pWME) ;
/*
    #ifdef SML_DIRECT
        if ( GetConnection().IsDirectConnection() )
        {
            EmbeddedConnection* pConnection = static_cast<EmbeddedConnection*>( GetConnection() );
            pConnection.DirectAddWME_Double( m_AgentSMLHandle, parent.GetValueAsString(), pAttribute, value, pWME.GetTimeTag());

            // Return immediately, without adding it to the commit list.
            return pWME ;
        }
    #endif
*/
        // Add it to our list of changes that need to be sent to Soar.
        m_DeltaList.AddWME(pWME) ;

        // Commit immediately if we're configured that way (makes life simpler for the client)
        if (IsAutoCommitEnabled())
            Commit() ;

        return pWME ;
        
    }

    Identifier     CreateIdWME(Identifier parent, String pAttribute)
    {
        assert(m_Agent == parent.GetAgent()) ;

        // Create a new, unique id (e.g. "i3").  This id will be mapped to a different id
        // in the kernel.
        final String id = GenerateNewID(pAttribute) ;

        Identifier pWME = new Identifier(GetAgent(), parent, parent.GetValueAsString(), pAttribute, id, GenerateTimeTag()) ;

        // Record that the identifer owns this new WME
        parent.AddChild(pWME) ;
/*
    #ifdef SML_DIRECT
        if (GetConnection().IsDirectConnection())
        {
            EmbeddedConnection* pConnection = static_cast<EmbeddedConnection*>( GetConnection() );
            pConnection.DirectAddID( m_AgentSMLHandle, parent.GetValueAsString(), pAttribute, id.c_str(), pWME.GetTimeTag());

            // Return immediately, without adding it to the commit list.
            return pWME ;
        }
    #endif
*/
        // Add it to our list of changes that need to be sent to Soar.
        m_DeltaList.AddWME(pWME) ;

        // Commit immediately if we're configured that way (makes life simpler for the client)
        if (IsAutoCommitEnabled())
            Commit() ;

        return pWME ;        
    }
    Identifier     CreateSharedIdWME(Identifier parent, String pAttribute, Identifier pSharedValue)
    {
        assert(m_Agent == parent.GetAgent()) ;
        assert(m_Agent == pSharedValue.GetAgent()) ;

        // Look up the id from the existing identifier
        @SuppressWarnings("unused")
        String id = pSharedValue.GetValueAsString() ;

        // Create the new WME with the same value
        Identifier pWME = new Identifier(GetAgent(), parent, parent.GetValueAsString(), pAttribute, pSharedValue, GenerateTimeTag()) ;

        // Record that the identifer owns this new WME
        parent.AddChild(pWME) ;
/*
    #ifdef SML_DIRECT
        if (GetConnection().IsDirectConnection())
        {
            EmbeddedConnection* pConnection = static_cast<EmbeddedConnection*>( GetConnection() );
            pConnection.DirectAddID( m_AgentSMLHandle, parent.GetValueAsString(), pAttribute, id.c_str(), pWME.GetTimeTag());

            // Return immediately, without adding it to the commit list.
            return pWME ;
        }
    #endif
*/
        // Add it to our list of changes that need to be sent to Soar.
        m_DeltaList.AddWME(pWME) ;

        // Commit immediately if we're configured that way (makes life simpler for the client)
        if (IsAutoCommitEnabled())
            Commit() ;

        return pWME ;
        
    }

    void            UpdateString(StringElement pWME, String pValue)
    {
        if (pWME == null || pValue == null)
            return ;

        assert(m_Agent == pWME.GetAgent()) ;

        // If the value hasn't changed and we're set to not blink the wme (remove/add it again)
        // then there's no work to do.
        if (!m_Agent.IsBlinkIfNoChange())
        {
            if (pWME.GetValue().equals(pValue))
                return ;
        }

        // Changing the value logically is a remove and then an add

        // Get the tag of the value to remove
        int removeTimeTag = pWME.GetTimeTag() ;

        // Change the value and the time tag (this is equivalent to us deleting the old object
        // and then creating a new one).
        pWME.SetValue(pValue) ;
        pWME.GenerateNewTimeTag() ;
/*
    #ifdef SML_DIRECT
        if ( GetConnection().IsDirectConnection() )
        {
            EmbeddedConnection* pConnection = static_cast<EmbeddedConnection*>( GetConnection() );
            pConnection.DirectRemoveWME( m_AgentSMLHandle, removeTimeTag );

            pConnection.DirectAddWME_String( m_AgentSMLHandle, pWME.GetIdentifierName(), pWME.GetAttribute(), pValue, pWME.GetTimeTag());

            // Return immediately, without adding it to the commit list.
            return ;
        }
    #endif
*/
        // Add it to the list of changes that need to be sent to Soar.
        m_DeltaList.UpdateWME(removeTimeTag, pWME) ;

        // Commit immediately if we're configured that way (makes life simpler for the client)
        if (IsAutoCommitEnabled())
            Commit() ;

    }
    void            UpdateInt(IntElement pWME, int value)
    {
        if (pWME == null)
            return ;

        assert(m_Agent == pWME.GetAgent()) ;

        // If the value hasn't changed and we're set to not blink the wme (remove/add it again)
        // then there's no work to do.
        if (!m_Agent.IsBlinkIfNoChange())
        {
            if (pWME.GetValue() == value)
                return ;
        }

        // Changing the value logically is a remove and then an add

        // Get the tag of the value to remove
        int removeTimeTag = pWME.GetTimeTag() ;

        // Change the value and the time tag (this is equivalent to us deleting the old object
        // and then creating a new one).
        pWME.SetValue(value) ;
        pWME.GenerateNewTimeTag() ;

        /*
    #ifdef SML_DIRECT
        if ( GetConnection().IsDirectConnection() )
        {
            EmbeddedConnection* pConnection = static_cast<EmbeddedConnection*>( GetConnection() );
            pConnection.DirectRemoveWME( m_AgentSMLHandle, removeTimeTag );

            pConnection.DirectAddWME_Int( m_AgentSMLHandle, pWME.GetIdentifierName(), pWME.GetAttribute(), value, pWME.GetTimeTag());

            // Return immediately, without adding it to the commit list.
            return ;
        }
    #endif
    */

        // Add it to the list of changes that need to be sent to Soar.
        m_DeltaList.UpdateWME(removeTimeTag, pWME) ;

        // Commit immediately if we're configured that way (makes life simpler for the client)
        if (IsAutoCommitEnabled())
            Commit() ;

    }
    
    void            UpdateFloat(FloatElement pWME, double value)
    {
        if (pWME == null)
            return ;

        assert(m_Agent == pWME.GetAgent()) ;

        // If the value hasn't changed and we're set to not blink the wme (remove/add it again)
        // then there's no work to do.
        if (!m_Agent.IsBlinkIfNoChange())
        {
            // Note: There's no error margin allowed on this, so the value must match exactly or
            // the wme will blink.
            if (pWME.GetValue() == value)
                return ;
        }

        // Changing the value logically is a remove and then an add

        // Get the tag of the value to remove
        int removeTimeTag = pWME.GetTimeTag() ;

        // Change the value and the time tag (this is equivalent to us deleting the old object
        // and then creating a new one).
        pWME.SetValue(value) ;
        pWME.GenerateNewTimeTag() ;

/*
    #ifdef SML_DIRECT
        if ( GetConnection().IsDirectConnection() )
        {
            EmbeddedConnection* pConnection = static_cast<EmbeddedConnection*>( GetConnection() );
            pConnection.DirectRemoveWME( m_AgentSMLHandle, removeTimeTag );

            pConnection.DirectAddWME_Double( m_AgentSMLHandle, pWME.GetIdentifierName(), pWME.GetAttribute(), value, pWME.GetTimeTag());

            // Return immediately, without adding it to the commit list.
            return ;
        }
    #endif
*/
        // Add it to the list of changes that need to be sent to Soar.
        m_DeltaList.UpdateWME(removeTimeTag, pWME) ;

        // Commit immediately if we're configured that way (makes life simpler for the client)
        if (IsAutoCommitEnabled())
            Commit() ;
        
    }

    boolean            DestroyWME(WMElement pWME)
    {
        assert(m_Agent == pWME.GetAgent()) ;

        IdentifierSymbol parent = pWME.GetIdentifier() ;

        // We can't delete top level WMEs (e.g. the WME that represents the input-link's ID)
        // Those are architecturally created.
        if (parent == null)
            return false ;

        // The parent identifier no longer owns this WME
        parent.RemoveChild(pWME) ;
/*
    #ifdef SML_DIRECT
        if ( GetConnection().IsDirectConnection() )
        {
            EmbeddedConnection* pConnection = static_cast<EmbeddedConnection*>( GetConnection() );
            pConnection.DirectRemoveWME( m_AgentSMLHandle, pWME.GetTimeTag() );

            // Return immediately, without adding it to the commit list
            delete pWME ;

            return true ;
        }
    #endif
*/
        // Add it to the list of changes to send to Soar.
        m_DeltaList.RemoveWME(pWME.GetTimeTag()) ;

        // Now we can delete it
        pWME.delete();

        // Commit immediately if we're configured that way (makes life simpler for the client)
        if (IsAutoCommitEnabled())
            Commit() ;

        return true ;        
    }

    boolean            TryToAttachOrphanedChildren(Identifier pPossibleParent)
    {
        if (m_OutputOrphans.isEmpty())
            return false ;

        boolean deleteFromList = true ;
        WMElement pWme = SearchWmeListForID(m_OutputOrphans, pPossibleParent.GetValueAsString(), deleteFromList) ;

        while (pWme != null)
        {
            pWme.SetParent(pPossibleParent) ;
            pPossibleParent.AddChild(pWme) ;

            if (this.GetAgent().GetKernel().IsTracingCommunications())
                System.out.printf("Adding orphaned child to this ID: %s ^%s %s (time tag %d)\n", 
                        pWme.GetIdentifierName(), pWme.GetAttribute(), pWme.GetValueAsString(), pWme.GetTimeTag()) ;

            // If the wme being attached is itself an identifier, we have to check in turn to see if it has any orphaned children
            if (pWme.IsIdentifier())
                TryToAttachOrphanedChildren(pWme.ConvertToIdentifier()) ;

            // Make a record that this wme was added so we can alert the client to this change.
            RecordAddition(pWme) ;

            pWme = SearchWmeListForID(m_OutputOrphans, pPossibleParent.GetValueAsString(), deleteFromList) ;
        }

        return true ;
        
    }
    boolean            ReceivedOutputRemoval(ElementXML pWmeXML, boolean tracing)
    {
        // We're removing structure from the output link
        String pTimeTag = pWmeXML.GetAttribute(sml_Names.getKWME_TimeTag()) ; // These will usually be kernel side time tags (e.g. +5 not -7)

        int timeTag = Integer.valueOf(pTimeTag) ;

        // If we have no output link we can't delete things from it.
        if (m_OutputLink == null)
            return false ;

        // Find the WME which matches this tag.
        // This may fail as we may have removed the parent of this WME already in the series of remove commands.
        //WMElement* pWME = m_OutputLink.FindFromTimeTag(timeTag) ;
        WMElement pWMEIter = m_TimeTagWMEMap.get( timeTag );

        // Delete the WME
        if (pWMEIter != null && pWMEIter.GetParent() != null)
        {
            if (tracing)
                System.out.printf("Removing output wme: time tag %s\n", pTimeTag) ;

            pWMEIter.GetParent().RemoveChild(pWMEIter) ;

            // Make a record that this wme was removed, so we can tell the client about it.
            // This recording will also involve deleting the wme.
            RecordDeletion(pWMEIter) ;
        }
        else
        {
            if (tracing)
                System.out.printf("Remove output wme request (seems to already be gone): time tag %s\n", pTimeTag) ;
            return false ;
        }

        return true ;
        
    }
    boolean            ReceivedOutputAddition(ElementXML pWmeXML, boolean tracing)
    {
        // We're adding structure to the output link
        String pID         = pWmeXML.GetAttribute(sml_Names.getKWME_Id()) ;   // These IDs will be kernel side ids (e.g. "I3" not "i3")
        String pAttribute  = pWmeXML.GetAttribute(sml_Names.getKWME_Attribute()) ;
        String pValue      = pWmeXML.GetAttribute(sml_Names.getKWME_Value()) ;
        String pType       = pWmeXML.GetAttribute(sml_Names.getKWME_ValueType()) ;    // Can be NULL (=> string)
        String pTimeTag    = pWmeXML.GetAttribute(sml_Names.getKWME_TimeTag()) ;  // These will be kernel side time tags (e.g. +5 not -7)

        // Set the default value
        if (pType == null)
            pType = sml_Names.getKTypeString ();

        // Check we got everything we need
        if (pID == null || pAttribute == null || pValue == null || pTimeTag == null)
            return false ;

        if (tracing)
        {
            System.out.printf("Received output wme: %s ^%s %s (time tag %s)\n", pID, pAttribute, pValue, pTimeTag) ;
        }

        int timeTag = Integer.valueOf(pTimeTag) ;

        // Find the parent wme that we're adding this new wme to
        // (Actually, there can be multiple WMEs that have this identifier
        //  as its value, but any one will do because the true parent is the
        //  identifier symbol which is the same for any identifiers).
        IdentifierSymbol pParentSymbol = FindIdentifierSymbol(pID) ;
        WMElement pAddWme = null;

        if (pParentSymbol != null)
        {
            // Create a client side wme object to match the output wme and add it to
            // our tree of objects.
            pAddWme = CreateWME(pParentSymbol, pID, pAttribute, pValue, pType, timeTag) ;
            if (pAddWme != null)
            {
                pParentSymbol.AddChild(pAddWme) ;

                // Make a record that this wme was added so we can alert the client to this change.
                RecordAddition(pAddWme) ;
            }
            else
            {
                System.out.println("Unable to create an output wme -- type was not recognized") ;
                // TODO
                //GetAgent().SetDetailedError(Error::kOutputError, "Unable to create an output wme -- type was not recognized") ;
            }
        }
        else
        {
            // See if this is the output-link itself (we want to keep a handle to that specially)
            if (m_OutputLink == null && sml_Names.getKOutputLinkName().equalsIgnoreCase(pAttribute))
            {
                m_OutputLink = new Identifier(GetAgent(), pValue, timeTag) ;

            } else if (m_OutputLink != null && 
                    (m_OutputLink.GetValueAsString().equals(pValue) && 
                            sml_Names.getKOutputLinkName().equalsIgnoreCase(pAttribute)))
            {
                // Adding output link again but we already have it so ignored
            } else
            {
                // If we reach here we've received output which is out of order (e.g. (Y ^att value) before (X ^att Y))
                // so there's no parent to connect it to.  We'll create the wme, keep it on a special list of orphans
                // and try to reconnect it later.
                pAddWme = CreateWME(null, pID, pAttribute, pValue, pType, timeTag) ;

                if (tracing)
                    System.out.printf("Received output wme (orphaned): %s ^%s %s (time tag %s)\n", pID, pAttribute, pValue, pTimeTag) ;

                if (pAddWme != null)
                    m_OutputOrphans.add(pAddWme) ;
            }
        }

        // If we have an output wme still waiting to be connected to its parent
        // and we get in a new wme that is creating an identifier see if they match up.
        if (pAddWme != null && pAddWme.IsIdentifier() && !m_OutputOrphans.isEmpty())
        {
            TryToAttachOrphanedChildren(pAddWme.ConvertToIdentifier()) ;
        }

        return true ;
        
    }
    boolean            ReceivedOutput(AnalyzeXML pIncoming, ElementXML pResponse)
    {

//        #ifdef _DEBUG
//            char * pMsgText = pIncoming.GetCommandTag().GenerateXMLString(true, true) ;
//        #endif

            // Get the command tag which contains the list of wmes
            final ElementXML pCommand = pIncoming.GetCommandTag() ;

            int nChildren = pCommand.GetNumberChildren() ;

            ElementXML wmeXML = new ElementXML() ;
            ElementXML pWmeXML = wmeXML ;

            boolean ok = true ;

            // Make sure the output orphans list is empty
            // We'll use this to store output wmes that have no parent identifier yet
            // (this is rare but can happen if the kernel generates wmes in an unusual order)
            m_OutputOrphans.clear() ;

            boolean tracing = this.GetAgent().GetKernel().IsTracingCommunications() ;

            for (int i = 0 ; i < nChildren ; i++)
            {
                pCommand.GetChild(wmeXML, i) ;

                // Ignore tags that aren't wmes.
                if (!pWmeXML.IsTag(sml_Names.getKTagWME()))
                    continue ;

                // Find out if this is an add or a remove
                String pAction = pWmeXML.GetAttribute(sml_Names.getKWME_Action()) ;

                if (pAction == null)
                    continue ;

                boolean add = sml_Names.getKValueAdd().equals(pAction) ;
                boolean remove = sml_Names.getKValueRemove().equals(pAction) ;

                if (add)
                {
                    ok = ReceivedOutputAddition(pWmeXML, tracing) && ok ;
                }
                else if (remove)
                {
                    ok = ReceivedOutputRemoval(pWmeXML, tracing) && ok ;
                }
            }

            // Check that we managed to reconnect all of the orphaned wmes
            if (!m_OutputOrphans.isEmpty())
            {
                ok = false ;

                if (tracing)
                    System.out.println("Some output WMEs have no matching parent IDs -- they are ophans.  This is bad.") ;

                // TODO  GetAgent().SetDetailedError(Error::kOutputError, "Some output WMEs have no matching parent IDs -- they are ophans.  This is bad.") ;
                m_OutputOrphans.clear() ;   // Have to discard them.
            }

            // Let anyone listening for the output notification know that output was just received.
            GetAgent().FireOutputNotification() ;

            // Call any handlers registered to listen for output
            // (This is one way to retrieve output).
            // We do this at the end of the output handler so that all of the children of the wme
            // have been received and recorded before we call the handler.
            if (GetAgent().IsRegisteredForOutputEvent() && m_OutputLink != null)
            {
                int nWmes = m_OutputDeltaList.GetSize() ;
                for (int i = 0 ; i < nWmes ; i++)
                {
                    WMElement pWme = m_OutputDeltaList.GetDeltaWME(i).getWME() ;

                    if (pWme.GetParent() == null || pWme.GetParent().GetIdentifierSymbol() == null)
                        continue ;

                    // We're only looking for top-level wmes on the output link so check if our identifier's symbol
                    // matches the output link's value
                    if (pWme.GetParent().GetIdentifierSymbol().equals(m_OutputLink.GetValueAsString()))
                    {
                        // Notify anyone who's listening to this event
                        GetAgent().ReceivedOutputEvent(pWme) ;
                    }
                }

                // This is potentially wrong, but I think we should now clear the list of changes.
                // If someone is working with the callback handler model it would be very hard for them to call this
                // themselves and without this call somewhere we'll get mutiple calls to the same handlers.
                ClearOutputLinkChanges() ;
            }

//        #ifdef _DEBUG
//                ElementXML::DeleteString(pMsgText) ;
//        #endif

            // Returns false if any of the adds/removes fails
            return ok ;
    }

    boolean            SynchronizeInputLink()
    {
        AnalyzeXML response = new AnalyzeXML();

        // Call to the kernel to get the current state of the input link
        boolean ok = GetConnection().SendAgentCommand(response, sml_Names.getKCommand_GetAllInput(), GetAgentName()) ;
        
        if (!ok)
            return false ;

//    #ifdef _DEBUG
//        char* pStr = response.GenerateXMLString(true) ;
//    #endif

        // Erase the existing input link and create a new representation from scratch
        m_InputLink.delete() ;
        m_InputLink = null ;

        GetInputLink() ;

        // Get the result tag which contains the list of wmes
        ElementXML pMain = response.GetResultTag() ;

        int nChildren = pMain.GetNumberChildren() ;

        ElementXML wmeXML = new ElementXML(null) ;
        ElementXML pWmeXML = wmeXML ;

        boolean tracing = this.GetAgent().GetKernel().IsTracingCommunications() ;

        for (int i = 0 ; i < nChildren ; i++)
        {
            pMain.GetChild(wmeXML, i) ;

            // Ignore tags that aren't wmes.
            if (!pWmeXML.IsTag(sml_Names.getKTagWME()))
                continue ;

            // Get the wme information
            String pID         = pWmeXML.GetAttribute(sml_Names.getKWME_Id()) ;   // These IDs will be kernel side ids (e.g. "I3" not "i3")
            String pAttribute  = pWmeXML.GetAttribute(sml_Names.getKWME_Attribute()) ;
            String pValue      = pWmeXML.GetAttribute(sml_Names.getKWME_Value()) ;
            String pType       = pWmeXML.GetAttribute(sml_Names.getKWME_ValueType()) ;    // Can be NULL (=> string)
            String pTimeTag    = pWmeXML.GetAttribute(sml_Names.getKWME_TimeTag()) ;  // These will be kernel side time tags (e.g. +5 not -7)

            // Set the default value
            if (pType == null)
                pType = sml_Names.getKTypeString();

            // Check we got everything we need
            if (pID == null || pAttribute == null || pValue == null || pTimeTag == null)
                continue ;

            if (tracing)
            {
                System.out.printf("Received input wme: %s ^%s %s (time tag %s)\n", pID, pAttribute, pValue, pTimeTag) ;
            }

            int timeTag = Integer.valueOf(pTimeTag) ;

            // Find the parent wme that we're adding this new wme to
            // (Actually, there can be multiple WMEs that have this identifier
            //  as its value, but any one will do because the true parent is the
            //  identifier symbol which is the same for any identifiers).
            IdentifierSymbol pParentSymbol = FindIdentifierSymbol(pID) ;
            WMElement pAddWme = null;

            if (pParentSymbol != null)
            {
                // Create a client side wme object to match the input wme and add it to
                // our tree of objects.
                pAddWme = CreateWME(pParentSymbol, pID, pAttribute, pValue, pType, timeTag) ;
                if (pAddWme != null)
                {
                    pParentSymbol.AddChild(pAddWme) ;
                }
                else
                {
                    System.out.println("Unable to create an input wme -- type was not recognized") ;
                    // TODO GetAgent().SetDetailedError(Error::kOutputError, "Unable to create an input wme -- type was not recognized") ;
                }
            }
            else
            {
                if (tracing)
                    System.out.printf("Received input wme (orphaned): %s ^%s %s (time tag %s)\n", pID, pAttribute, pValue, pTimeTag) ;
            }
        }

//    #ifdef _DEBUG
//        response.DeleteString(pStr) ;
//    #endif

        // Returns false if had any errors
        return ok ;
    }
    boolean            SynchronizeOutputLink()
    {
        // Not supported for direct connections
        //if (GetConnection().IsDirectConnection())
        //  return false ;

        AnalyzeXML incoming = new AnalyzeXML();
        ElementXML response = new ElementXML();

        // Call to the kernel to get the current state of the output link
        boolean ok = GetConnection().SendAgentCommand(incoming, sml_Names.getKCommand_GetAllOutput(), GetAgentName()) ;
        
        if (!ok)
            return false ;

//    #ifdef _DEBUG
//        char* pStr = incoming.GenerateXMLString(true) ;
//    #endif

        // Erase the existing output link and create a new representation from scratch
        if (m_OutputLink != null)
        {
            //int children = m_OutputLink.GetNumberChildren() ;
            m_OutputLink.delete();
            m_OutputLink = null ;
        }

        // Process the new list of output -- as if it had just occurred in the agent (when in fact we're just synching with it)
        ok = ReceivedOutput(incoming, response) ;

//    #ifdef _DEBUG
//        incoming.DeleteString(pStr) ;
//    #endif

        // Returns false if had any errors
        return ok ;
    }

    int            GenerateTimeTag()
    {
        // We use negative tags on the client, so we don't mistake them
        // for ones from the real kernel.
        int tag = GetAgent().GetKernel().GenerateNextTimeTag() ;

        return tag ;
        
    }
    String            GenerateNewID(String pAttribute)
    {
        int id = GetAgent().GetKernel().GenerateNextID() ;

        // we'll start our ids with lower case so we can distinguish them
        // from soar id's.  We'll take the first letter of the attribute,
        // much as soar does, but always add a unique number to the back,
        // so the choice of initial letter really isn't important.
        char letter = pAttribute.charAt(0) ;

        // Convert to lowercase
        if (letter >= 'A' || letter <= 'Z')
            letter = (char)(letter - 'A' + 'a');

        // Make sure we got a letter here (just in case)
        if (letter < 'a' || letter > 'z')
            letter = 'a' ;

        return letter + Integer.toString(id);
    }

    void            Refresh()
    {
        if (m_InputLink != null)
        {
            AnalyzeXML response = new AnalyzeXML();

            // Start by getting the input link identifier
            if (GetConnection().SendAgentCommand(response, sml_Names.getKCommand_GetInputLink(), GetAgentName()))
            {
                // Technically we should reset the value of the input link identifier itself, but it should never
                // change value (since it's architecturally created) and also adding a method to set the identifier value
                // is a bad idea if we only need it here.
                @SuppressWarnings("unused")
                String result = response.GetResultString() ;
            }

            m_InputLink.Refresh() ;

            // Send the new input link over to the kernel
            Commit() ;
        }

        // At one time we deleted the output link at the end of an init-soar but the current
        // implementation of init-soar recreates the output link (in the kernel) at the end of reinitializing the agent
        // so the output link should not be deleted but it's children need to be.
        if (m_OutputLink != null)
        {
            //int outputs = m_OutputLink.GetNumberChildren() ;

            // The children should all have been deleted during the init-soar cleanup
            // If not, we may be looking at a memory leak.
            //assert(outputs == 0) ;
            m_OutputLink.GetSymbol().DeleteAllChildren() ;

        // clean up the IdSymbolMap table. See Bug #1094
        IdentifierSymbol out_sym = m_IdSymbolMap.get(m_OutputLink.GetValueAsString());
        if (out_sym != null) {
          m_IdSymbolMap.clear();
          m_IdSymbolMap.put(m_OutputLink.GetValueAsString(), out_sym);
        }
        else {
          m_IdSymbolMap.clear();
        }
        }        
    }

    boolean            IsCommitRequired() { return (m_DeltaList.GetSize() != 0) ; }
    
    boolean            Commit()
    {
        int deltas = m_DeltaList.GetSize() ;

        // If nothing has changed, we have no work to do.
        // This allows us to call Commit() multiple times without causing problems
        // as later calls will be ignored if the current set of changes has been sent already.
        if (deltas == 0)
            return true ;

        // Build the SML message we're doing to send.
        ElementXML pMsg = GetConnection().CreateSMLCommand(sml_Names.getKCommand_Input()) ;

        // Add the agent parameter and as a side-effect, get a pointer to the <command> tag.  This is an optimization.
        ElementXML command = GetConnection().AddParameterToSMLCommand(pMsg, sml_Names.getKParamAgent(), GetAgentName()) ;
        //ElementXML command(hCommand) ;

        // Build the list of WME changes
        for (int i = 0 ; i < deltas ; i++)
        {
            // Get the next change
            TagWme pDelta = m_DeltaList.GetDelta(i) ;

            // Add it as a child of the command tag
            // (the command takes ownership of the delta)
            command.AddChild(pDelta) ;
        }

        // This is important.  We are working with a subpart of pMsg.
        // If we retain ownership of the handle and delete the object
        // it will release the handle...deleting part of our message.
        //command.Detach() ;

        // We have transfered the list of deltas over to pMsg
        // so we need to clear the list, but not delete the tags that it contains.
        m_DeltaList.Clear(false) ;
/*
    #ifdef _DEBUG
        // Generate a text form of the XML so we can look at it in the debugger.
        char* pStr = pMsg.GenerateXMLString(true) ;
        pMsg.DeleteString(pStr) ;
    #endif
*/
        // Send the message
        AnalyzeXML response = new AnalyzeXML();
        boolean ok = GetConnection().SendMessageGetResponse(response, pMsg) ;

        // Clean up
        // delete pMsg ;

        return ok ;        
    }
    boolean            IsAutoCommitEnabled(){ return m_Agent.IsAutoCommitEnabled() ; }

}

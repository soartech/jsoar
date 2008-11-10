/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 5, 2008
 */
package sml;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ray
 */
class DeltaList
{
    final List<TagWme> m_DeltaList = new ArrayList<TagWme>();
    
    void delete()
    {
        Clear(true) ;
    }

    // We make deleting the contents optional as
    // we may just be moving the tags out of here
    // and into a message for sending, in which case
    // we won't delete them.
    void Clear(boolean deleteContents)
    {
        if (deleteContents)
        {
            for (TagWme pDelta : m_DeltaList)
            {
                pDelta.delete();
            }
        }

        m_DeltaList.clear() ;
    }

    void RemoveWME(int timeTag)
    {
        // Create the wme tag
        TagWme pTag = new TagWme() ;

        // For removes, we just use the time tag
        pTag.SetTimeTag(timeTag) ;
        pTag.SetActionRemove() ;

        m_DeltaList.add(pTag) ;   
        
    }

    void AddWME(WMElement pWME)
    {
        // Create the wme tag
        TagWme pTag = new TagWme() ;

        // For adds we send everything
        pTag.SetIdentifier(pWME.GetIdentifier().GetIdentifierSymbol()) ;
        pTag.SetAttribute(pWME.GetAttribute()) ;
        pTag.SetValue(pWME.GetValueAsString(), pWME.GetValueType()) ;
        pTag.SetTimeTag(pWME.GetTimeTag()) ;
        pTag.SetActionAdd() ;

        m_DeltaList.add(pTag) ;   
        
    }

    void UpdateWME(int timeTagToRemove, WMElement pWME)
    {
        // This is equivalent to a remove of the old value followed by an add of the new
        // We could choose to use a single tag for this later on.
        RemoveWME(timeTagToRemove) ;
        AddWME(pWME) ;
    }

    int GetSize()           { return m_DeltaList.size() ; }
    TagWme GetDelta(int i) { return m_DeltaList.get(i) ; }

}

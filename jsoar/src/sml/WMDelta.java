/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 5, 2008
 */
package sml;

/**
 * @author ray
 */
class WMDelta
{
    public static enum ChangeType { kAdded, kRemoved } ;

    final ChangeType  m_ChangeType ;

    // If this is an element that has been removed then
    // we actually own this pointer.  Otherwise we don't.  Be careful.
    final WMElement  m_pWME ;

    public WMDelta(ChangeType change, WMElement pWME)
    {
        m_ChangeType = change ;
        m_pWME       = pWME ;
    }

    void delete()
    {
        if(m_ChangeType == ChangeType.kRemoved)
        {
            m_pWME.delete();
        }
    }

    ChangeType getChangeType() { return m_ChangeType ; }
    WMElement getWME()        { return m_pWME ; }
}

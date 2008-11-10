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
class OutputDeltaList
{
    final List<WMDelta>       m_DeltaList = new ArrayList<WMDelta>();

    public OutputDeltaList() { }

    public void delete()
    {
        Clear(true, false, false) ;
    }

    public void Clear(boolean deleteContents, boolean clearJustAdded /*= false*/, boolean clearChildrenModified /*= false*/)
        {
            if ( clearJustAdded || clearChildrenModified )
            {
                for (WMDelta iter : m_DeltaList)
                {
                    WMElement pWME = iter.getWME();

                    if ( clearJustAdded )
                    {
                        pWME.SetJustAdded( false );
                    }
                    if ( clearChildrenModified )
                    {
                        if ( pWME.IsIdentifier() )
                        {
                            Identifier pID = (Identifier) pWME;
                            pID.m_pSymbol.SetAreChildrenModified( false );
                        }
                    }
                }
            }

            if (deleteContents)
            {
                for(WMDelta delta : m_DeltaList)
                {
                    delta.delete();
                }
            }

            m_DeltaList.clear() ;
        }

        void RemoveWME(WMElement pWME)
        {
            WMDelta pDelta = new WMDelta(WMDelta.ChangeType.kRemoved, pWME) ;
            m_DeltaList.add(pDelta) ;
        }

        void AddWME(WMElement pWME)
        {
            WMDelta pDelta = new WMDelta(WMDelta.ChangeType.kAdded, pWME) ;
            m_DeltaList.add(pDelta) ;
        }

        int GetSize()               { return m_DeltaList.size() ; }
        WMDelta GetDeltaWME(int i) { return m_DeltaList.get(i) ; }

}

/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 1, 2008
 */
package sml;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author ray
 */
class ArgMap
{
    private Map<String, ElementXML> argMap = new TreeMap<String, ElementXML>();
    private List<ElementXML> argList = new ArrayList<ElementXML>();
    
    // NOTE: Must record the arguments in the same order they exist in the original document
    // so we can look them up by position.  This map takes ownership of the pArg object.
    public void RecordArg(ElementXML pArg)
    {
        if (pArg == null)
            return ;

        argList.add(pArg) ;

        // Look up the argument's name (if it has one)
        String pName = pArg.GetAttribute(sml_Names.getKArgParam()) ;

        if (pName != null)
        {
            argMap.put(pName, pArg);
        }
    }

    // Returns the value of the named argument.  If that name is not found, returns the value for the
    // given position.  If you are not interested in looking up by position pass -1 for position
    // (in which case you'll get back NULL if the name'd arg doesn't exist).
    public String GetArgValue(String pName, int position)
    {
        ElementXML hArg = GetArgHandle(pName, position) ;

        if (hArg == null)
            return null;

        return hArg.GetCharacterData();
    }

    // Returns a handle to the <arg> tag.  We return a handle rather than an ElementXML* object
    // so we don't create ElementXML objects we don't need.  (It's just for efficiency).
    public ElementXML GetArgHandle(String pName, int position)
    {
        // First try to look up the argument by name.
        ElementXML hArg = argMap.get(pName);
        if(hArg != null)
        {
            return hArg;
        }

        // Check if we're asking for a position that doesn't exist
        // in this argument list.  (This isn't necessarily an error
        // if we have optional arguments).
        if (position < 0 || position >= argList.size())
        {
            return null ;
        }

        return argList.get(position);
    }
    
}

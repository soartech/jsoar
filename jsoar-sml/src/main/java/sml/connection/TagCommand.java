/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 7, 2008
 */
package sml.connection;

import sml.ElementXML;
import sml.sml_Names;

/**
 * @author ray
 */
public class TagCommand extends ElementXML
{
    public TagCommand()
    {
        SetTagName(sml_Names.getKTagCommand()) ;
    }

    String GetName()
    {
        String pID = this.GetAttribute(sml_Names.getKCommandName()) ;
        return pID ;
    }

    void SetName(String pName)
    {
        this.AddAttribute(sml_Names.getKCommandName(), CopyString(pName), false) ;
    }

    // NOTE: Be careful with this one.  If you call this, you must keep pName in scope
    // for the life of this object, which generally means it needs to be a static constant.
    void SetNameFast(String pName)
    {
        this.AddAttribute(sml_Names.getKCommandName(), pName) ;
    }

}

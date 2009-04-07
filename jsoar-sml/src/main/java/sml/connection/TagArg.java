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
public class TagArg extends ElementXML
{
    TagArg()
    {
        this.SetTagName(sml_Names.getKTagArg()) ;
    }

    void SetParam(String pName)
    {
        this.AddAttribute(sml_Names.getKArgParam(), CopyString(pName), false) ;
    }

    // NOTE: Be careful with this one.  If you call this, you must keep pName in scope
    // for the life of this object, which generally means it needs to be a static constant.
    void SetParamFast(String pName)
    {
        this.AddAttribute(sml_Names.getKArgParam(), pName) ;
    }

    void SetType(String pType)
    {
        this.AddAttribute(sml_Names.getKArgType(), CopyString(pType), false) ;
    }

    // NOTE: Be careful with this one.  If you call this, you must keep pType in scope
    // for the life of this object, which generally means it needs to be a static constant.
    void SetTypeFast(String pType)
    {
        this.AddAttribute(sml_Names.getKArgType(), pType) ;
    }

    void SetValue(String pValue)
    {
        this.SetCharacterData(pValue) ;
    }
}

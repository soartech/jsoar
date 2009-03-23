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
public class TagError extends ElementXML
{
    TagError()
    {
        SetTagName(sml_Names.getKTagError()) ;
    }
    public void delete()
    {
        super.delete();
    }

    void SetDescription(String pErrorMsg)
    {
        this.SetCharacterData(pErrorMsg) ;
    }

    void SetErrorCode(int error)
    {
        this.AddAttribute(sml_Names.getKErrorCode(), Integer.toString(error)) ;
    }

}

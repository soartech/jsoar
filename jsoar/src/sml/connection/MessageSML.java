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
public class MessageSML extends ElementXML
{
    public static enum DocType
    {
        kCall, kResponse, kNotify
    };

    public MessageSML()
    {
        SetTagName(sml_Names.getKTagSML());
        AddAttribute(sml_Names.getKSMLVersion(), sml_Names.getKSMLVersionValue());
    }

    public MessageSML(DocType type, int id)
    {
        SetTagName(sml_Names.getKTagSML());
        AddAttribute(sml_Names.getKSMLVersion(), sml_Names.getKSMLVersionValue());

        String pDocType = sml_Names.getKDocType_Call();
        if (type == DocType.kResponse)
            pDocType = sml_Names.getKDocType_Response();
        else if (type == DocType.kNotify)
            pDocType = sml_Names.getKDocType_Notify();

        // Note: This version requires that pDocType never go out of scope,
        // which is fine
        // as long as we set it to a static constant as we've done here. Do not
        // change this to
        // accept a string from the user without changing the call to not be to
        // "FastFast" method.
        AddAttribute(sml_Names.getKDocType(), pDocType);

        // This is the only place where we need to allocate a string. Up to
        // here, the rest is
        // setting pointers to constants, so very fast.
        SetID(id);
    }

    public void delete()
    {
        super.delete();
    }

    public String GetID()
    {
        String pID = this.GetAttribute(sml_Names.getKID());
        return pID;
    }

    void SetID(int id)
    {
        this.AddAttribute(sml_Names.getKID(), Integer.toString(id));
    }

    void SetDocType(String pType)
    {
        this.AddAttribute(sml_Names.getKDocType(), CopyString(pType), false);
    }

    String GetDocType()
    {
        String pDocType = this.GetAttribute(sml_Names.getKDocType());
        return pDocType;
    }

    boolean IsCall()
    {
        String pDocType = GetDocType();
        return pDocType.equals(sml_Names.getKDocType_Call());
    }

    boolean IsResponse()
    {
        String pDocType = GetDocType();
        return pDocType.equals(sml_Names.getKDocType_Response());
    }

    boolean IsNotify()
    {
        String pDocType = GetDocType();
        return pDocType.equals(sml_Names.getKDocType_Notify());
    }

}

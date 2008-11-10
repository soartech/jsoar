/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 5, 2008
 */
package sml;

/**
 * @author ray
 */
class TagWme extends ElementXML
{

    /**
     * 
     */
    public TagWme()
    {
        // TODO Auto-generated constructor stub
    }
    public void SetIdentifier(String pIdentifier)
    {
        this.AddAttribute(sml_Names.getKWME_Id(), CopyString(pIdentifier), false) ;
    }

    public void SetAttribute(String pAttribute)
    {
        this.AddAttribute(sml_Names.getKWME_Attribute(), CopyString(pAttribute), false) ;
    }

    public void SetValue(String pValue, String pType)
    {
        this.AddAttribute(sml_Names.getKWME_Value(), CopyString(pValue), false) ;

        // The string type is the default, so we don't need to add it to the object
        // We do a direct pointer comparison here for speed, so if the user passes in "string" without using
        // sml_Names, we'll add it to the list of attributes (which does no harm).  This all just saves a little time.
        if (pType != null && !pType.equals(sml_Names.getKTypeString()))
            this.AddAttribute(sml_Names.getKWME_ValueType(), CopyString(pType), false) ;
    }

    public void SetTimeTag(int timeTag)
    {
        this.AddAttribute(sml_Names.getKWME_TimeTag(), Integer.toString(timeTag), false) ;
    }

    public void SetActionAdd()
    {
        this.AddAttribute(sml_Names.getKWME_Action(), sml_Names.getKValueAdd()) ;
    }

    public void SetActionRemove()
    {
        this.AddAttribute(sml_Names.getKWME_Action(), sml_Names.getKValueRemove()) ;
    }

}

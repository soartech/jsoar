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
public class TagResult extends ElementXML
{
    TagResult()
    {
        SetTagName(sml_Names.getKTagResult()) ;
    }
    public void delete()
    {
        super.delete();
    }

}

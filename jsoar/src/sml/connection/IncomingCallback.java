/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 6, 2008
 */
package sml.connection;

import sml.ElementXML;

/**
 * @author ray
 */
public interface IncomingCallback
{
    ElementXML execute(Connection connection, ElementXML xml, Object user) ;
}

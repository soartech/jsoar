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
public class Callback
{
    Connection         m_pConnection ;
    IncomingCallback    m_pCallback ;
    Object               m_pUserData ;

    /*************************************************************
    * @brief Accessors
    *************************************************************/
    IncomingCallback getFunction() { return m_pCallback ; }

    /*************************************************************
    * @brief Constructor
    *************************************************************/
    Callback(Connection pConnection, IncomingCallback pFunc, Object pUserData)
    {
        m_pConnection   = pConnection ;
        m_pCallback     = pFunc ;
        m_pUserData     = pUserData ; 
    }
    
    /*************************************************************
    * @brief Invoke this callback, passing the message,
    *        the connection and the user's data (which can be anything).
    * 
    * @returns NULL or a response to this message.
    *************************************************************/
    ElementXML Invoke(ElementXML pIncomingMessage)
    {
        ElementXML pResult = m_pCallback.execute(m_pConnection, pIncomingMessage, m_pUserData) ;
        return pResult ;
    }
}

/*
 * Copyright (c) 2012 Soar Technology Inc.
 *
 * Created on July 10, 2012
 */
package org.jsoar.kernel.io.xml;

import java.io.File;

import org.jsoar.kernel.io.InputOutput;

/**
 * An XML parser which adds XML graphs to the JSoar input link.
 * 
 * @author chris.kawatsu
 * 
 */
public interface XmlFileToWme
{
    /**
     * Add the contents of an XML file to the JSoar input link. The WMEs will be
     * added under the input link with the same name as the root XML node.
     * 
     * @param file
     *            - the XML file
     * @param io
     *            - agent's I/O object
     */
    public void xmlToWme(File file, InputOutput io);
}

/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on July 10, 2012
 */
package org.jsoar.kernel.io.xml;

import java.io.File;

/**
 * An XML parser which adds XML graphs to the JSoar input link.
 * 
 * @author chris.kawatsu
 *
 */
public interface XmlFileToWme {
	/**
	 * Add the contents of an XML file to the JSoar input link.
	 * 
	 * @param f - the XML file
	 * @param b - the JSoar builder
	 */
	public void xmlToWme(File file);
}

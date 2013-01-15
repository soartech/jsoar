/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on July 10, 2012
 */
package org.jsoar.kernel.io.xml;

@SuppressWarnings("serial")
public class TagAlreadyAddedException extends Exception {

	public TagAlreadyAddedException(String string) {
		super(string);
	}

}

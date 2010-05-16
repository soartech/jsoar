/*
 * (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on May 15, 2010
 */
package org.jsoar.kernel.commands;

import java.io.File;
import java.net.URL;

import org.jsoar.kernel.SoarException;

/**
 * @author ray
 */
public interface SourceCommandAdapter
{
    void eval(File file) throws SoarException;
    void eval(URL url) throws SoarException;
    String eval(String code) throws SoarException;
}

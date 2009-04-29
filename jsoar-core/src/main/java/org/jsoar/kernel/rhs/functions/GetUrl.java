/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on April 28, 2009
 */
package org.jsoar.kernel.rhs.functions;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.FileTools;

/**
 * A RHS function that opens a connection to a URL, reads it and returns the
 * contents as a string.
 * 
 * @author ray
 */
public class GetUrl extends AbstractRhsFunctionHandler
{
    /**
     */
    public GetUrl()
    {
        super("get-url", 1, 1);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
     */
    @Override
    public Symbol execute(final RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        
        // Parse the URL
        final String urlString = arguments.get(0).toString();
        final URL url;
        try
        {
            url = new URL(urlString);
        }
        catch (MalformedURLException e)
        {
            throw new RhsFunctionException("In '" + getName() + "' RHS function: " + e.getMessage());
        }
        
        // Open an input stream
        final InputStream is;
        try
        {
            is = new BufferedInputStream(url.openStream());
        }
        catch (IOException e)
        {
            throw new RhsFunctionException("In '" + getName() + "' RHS function: " + e.getMessage());
        }
        
        try
        {
            // Read the input stream into a buffer
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            FileTools.copy(is, out);
            return context.getSymbols().createString(out.toString());
        }
        catch (IOException e)
        {
            throw new RhsFunctionException("In '" + getName() + "' RHS function: " + e.getMessage());
        }
        finally
        {
            try
            {
                is.close();
            }
            catch (IOException e)
            {
                throw new RhsFunctionException("In '" + getName() + "' RHS function: " + e.getMessage());
            }
        }
    }
}

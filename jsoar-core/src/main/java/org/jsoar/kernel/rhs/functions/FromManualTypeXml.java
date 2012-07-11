/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on July 10, 2012
 */
package org.jsoar.kernel.rhs.functions;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.jsoar.kernel.io.xml.AutoTypeXmlToWme;
import org.jsoar.kernel.io.xml.ManualTypeXmlToWme;
import org.jsoar.kernel.io.xml.TagAlreadyAddedException;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.XmlTools;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * A RHS function that parses an XML string using {@link AutoTypeXmlToWme} and
 * returns a working memory representation of the XML.
 * 
 * @author chris.kawatsu
 */
public class FromManualTypeXml extends AbstractRhsFunctionHandler
{
    /**
     */
    public FromManualTypeXml()
    {
        super("from-mt-xml", 1, 100);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
     */
    @Override
    public Symbol execute(final RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        
        final String xml = arguments.get(0).toString();
        if(xml == null)
        {
            throw new RhsFunctionException("Only argument to '" + getName() + "' RHS function must be an XML string.");
        }
        
        final Document doc;
        try
        {
            doc = XmlTools.parse(xml);
        }
        catch (SAXException e)
        {
            throw new RhsFunctionException(e.getMessage(), e);
        }
        catch (IOException e)
        {
            throw new RhsFunctionException(e.getMessage(), e);
        }
        ManualTypeXmlToWme mt = new ManualTypeXmlToWme(context);
        Iterator<Symbol> args = arguments.iterator();
        args.next();
        try{
        while(args.hasNext()) {
        	String type = args.next().toString();
        	if(type.equals("float")) {
        		mt.addFloatTag(args.next().toString());
        	} else if (type.equals("int")) {
        		mt.addIntTag(args.next().toString());
        	}
        }
        }catch(TagAlreadyAddedException e) {
        	throw new RhsFunctionException("Int and float tags must be mutually exclusive.", e);
        }
        
        return mt.fromXml(doc.getDocumentElement());
    }
}

/*
 * Copyright (c) 2012 Soar Technology Inc.
 *
 * Created on July 10, 2012
 */
package org.jsoar.kernel.rhs.functions;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.jsoar.kernel.io.xml.ManualTypeXmlToWme;
import org.jsoar.kernel.io.xml.TagAlreadyAddedException;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.XmlTools;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * A RHS function that parses an XML string using {@link ManualTypeXmlToWme} and
 * returns a working memory representation of the XML. The root element of the
 * XML input is ignored.<br>
 * <br>
 * The type of XML data is specified by providing additional parameters after
 * the XML string. In the following production population and myInt are added as
 * integers while myFloat is added as a floating point number.<br>
 * <br>
 * 
 * <pre>
 * {@code
 * sp "testFromXml
 * (state <s> ^superstate nil ^io.input-link <il>) 
 * -->
 * (<il> ^xml (from-mt-xml |
 *   <ignored>
 *     <location>
 *       <name>Ann Arbor</name>
 *       <population>100000</population>
 *     </location>
 *     <person>
 *       <name>Bill</name>
 *     </person>
 *     <attribute name="test" myInt="1" myFloat="5.23"/>
 *   </ignored>| 
 *   |int| |ignored.location.population| 
 *   |int| |ignored.attribute.myInt| 
 *   |float| |ignored.attribute.myFloat|))"
 * }
 * </pre>
 * 
 * @author chris.kawatsu
 */
public class FromManualTypeXml extends AbstractRhsFunctionHandler
{
    /**
     */
    public FromManualTypeXml()
    {
        super("from-mt-xml", 1, Integer.MAX_VALUE);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel
     * .rhs.functions.RhsFunctionContext, java.util.List)
     */
    @Override
    public Symbol execute(final RhsFunctionContext context,
            List<Symbol> arguments) throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        
        final String xml = arguments.get(0).toString();
        if(xml == null)
        {
            throw new RhsFunctionException("Only argument to '" + getName()
                    + "' RHS function must be an XML string.");
        }
        
        final Document doc;
        try
        {
            doc = XmlTools.parse(xml);
        }
        catch(SAXException e)
        {
            throw new RhsFunctionException(e.getMessage(), e);
        }
        catch(IOException e)
        {
            throw new RhsFunctionException(e.getMessage(), e);
        }
        ManualTypeXmlToWme mt = new ManualTypeXmlToWme(context);
        Iterator<Symbol> args = arguments.iterator();
        args.next();
        try
        {
            while(args.hasNext())
            {
                String type = args.next().toString();
                if(type.equals("float"))
                {
                    mt.addFloatTag(args.next().toString());
                }
                else if(type.equals("int"))
                {
                    mt.addIntTag(args.next().toString());
                }
            }
        }
        catch(TagAlreadyAddedException e)
        {
            throw new RhsFunctionException(
                    "Int and float tags must be mutually exclusive.", e);
        }
        
        return mt.fromXml(doc.getDocumentElement());
    }
}

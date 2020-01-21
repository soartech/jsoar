/*
 * Copyright (c) 2012 Soar Technology Inc.
 *
 * Created on July 10, 2012
 */
package org.jsoar.kernel.io.xml;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.io.xml.XmlToWme;
import org.jsoar.kernel.memory.WmeFactory;
import org.jsoar.kernel.memory.WmeBuilder;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.Symbols;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 * Abstract class which contains functions to load XML from file and add a
 * parsed XML document to JSoar working memory.
 * 
 * @author chris.kawatsu
 * 
 */
abstract class AbstractXmlFileToWme implements XmlFileToWme, XmlToWme
{

    private WmeBuilder<?> builder = null;

    private WmeFactory<?> factory = null;

    /**
     * Overloaded {@link #AbstractXmlFileToWme(WmeFactory)}.
     * 
     * @param io
     */
    public AbstractXmlFileToWme(InputOutput io)
    {
        factory = io.asWmeFactory();
    }

    /**
     * Create an xmlToWme object which adds WMEs to an arbitrary location. The
     * root WME is returned by {@link #fromXml(Element)}.<br>
     * <br>
     * 
     * Note: {@link #xmlToWme(File, InputOutput) xmlToWme} adds WMEs directly to
     * the input link instead of returning a reference to a WME.
     * 
     * @param wmeFactory
     */
    public AbstractXmlFileToWme(WmeFactory<?> wmeFactory)
    {
        factory = wmeFactory;
    }

    @Override
    public void xmlToWme(File file, InputOutput io)
    {
        final Element root = getRootElement(file);
        final Identifier ret = fromXml(root);
        final SymbolFactory sf = io.getSymbols();
        io.addInputWme(io.getInputLink(),
                Symbols.create(sf, root.getNodeName()), ret);
    }

    @Override
    public Identifier fromXml(Element element)
    {
        builder = WmeBuilder.create(factory, element.getNodeName());
        // builder = builder.push(element.getNodeName());
        addAttributes(element.getAttributes(), builder);
        getXmlTree(element.getChildNodes(), builder);
        return builder.topId();
    }

    /**
     * Add all of the nodes in the list to the builder. If the nodes contain
     * children, make a recursive call to add them. Attributes of XML tags are
     * also added to the builder.<br>
     * <br>
     * 
     * Leaf tags containing text are added to the builder. Empty leaf tags are
     * added to the builder as an empty string, provided the tag contains no
     * attributes.
     * 
     * @param nodeList - the list of nodes
     * @param builder - the JSoar builder
     */
    abstract void getXmlTree(NodeList nodeList, WmeBuilder<?> builder);

    /**
     * Add the attributes contained in the <code>NamedNodeMap</code> to the
     * builder.
     * 
     * @param nnm
     *            - a collection of attribute nodes
     * @param builder
     *            - the JSoar builder
     */
    abstract void addAttributes(NamedNodeMap nnm, WmeBuilder<?> builder);

    /**
     * Parse an XML file and get its root {@link Element}.
     * 
     * @param f
     *            - the XML file to parse
     * @return The root <code>Element</code> of the XML file. Returns
     *         <code>null</code> if the file cannot be read or an error occurs
     *         during parsing.
     */
    protected static Element getRootElement(File f)
    {
        try
        {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document dom = db.parse(f);
            return dom.getDocumentElement();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return null;
    }
}

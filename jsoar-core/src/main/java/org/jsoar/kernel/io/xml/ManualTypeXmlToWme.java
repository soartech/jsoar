/*
 * Copyright (c) 2012 Soar Technology Inc.
 *
 * Created on July 10, 2012
 */
package org.jsoar.kernel.io.xml;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.memory.WmeBuilder;
import org.jsoar.kernel.memory.WmeFactory;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.kernel.symbols.Symbols;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An {@link AbstractXmlFileToWme} implementation in which the XML tags with
 * types other than string are manually specified. The class does <i>not</i>
 * support arbitrary graph structure using link attributes which is present in
 * {@link SoarTechXmlToWme}. XML tags are specified using their full path. For
 * example in:<br>
 * <br>
 * 
 * &lt;Message id="1"><br>
 * &nbsp;&nbsp;&nbsp;&lt;MessageValue>1.0&lt;/MessageValue><br>
 * &lt;/Message><br>
 * <br>
 * 
 * the values of MessageValue and id would be specified as a floating point or
 * integer using the paths <code>Message.MessageValue</code> and
 * <code>Message.id</code> respectively.<br>
 * <br>
 * 
 * Floating point and integer XML paths are added using
 * {@link #addFloatTag(String) addFloatTag} and {@link #addIntTag(String)
 * addIntTag}. The values added as floats and those added as integers are
 * enforced to be mutually exclusive.<br>
 * <br>
 * 
 * Using {@link #fromXml(Element) fromXml} the WME structure from the above XML
 * would be:<br>
 * <br>
 * 
 * <code>^id |1|</code><br>
 * <code>^MessageValue |1.0|</code><br>
 * <br>
 * 
 * Note that the root XML tag is ignored, but its attributes are still added. If
 * {@link #xmlToWme(java.io.File, InputOutput) xmlToWme} is used, the message is
 * added directly to the input link and the root tag is not ignored. This
 * results in the following WME structure:<br>
 * <br>
 * 
 * <code>^io</code><br>
 * &nbsp;&nbsp;&nbsp;<code>^input-link</code><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<code>^Message</code><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<code>^id |1|</code><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<code>^MessageValue |1.0|</code><br>
 * <br>
 * 
 * @author chris.kawatsu
 * 
 */
public class ManualTypeXmlToWme extends AbstractXmlFileToWme
{

    private XmlPath xmlPath;

    private Set<String> floatTags;

    private Set<String> intTags;

    public ManualTypeXmlToWme(InputOutput io)
    {
        super(io);
        xmlPath = new XmlPath();
        floatTags = new HashSet<String>();
        intTags = new HashSet<String>();
    }

    public ManualTypeXmlToWme(WmeFactory<?> wmeFactory)
    {
        super(wmeFactory);
        xmlPath = new XmlPath();
        floatTags = new HashSet<String>();
        intTags = new HashSet<String>();
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
        xmlPath.pushTag(element.getNodeName());
        Identifier ret = super.fromXml(element);
        xmlPath.popTag();
        return ret;
    }

    @Override
    protected void getXmlTree(NodeList nodeList, WmeBuilder<?> builder)
    {

        for (int i = 0; i < nodeList.getLength(); i++)
        {
            Node current = nodeList.item(i);
            // ignore nodes that are not element nodes (text and attribute nodes
            // are handled as a special case)
            if (current.getNodeType() == Node.ELEMENT_NODE)
            {
                xmlPath.pushTag(current.getNodeName());
                boolean pushed = false;

                // add attribute nodes, if any
                if (current.hasAttributes())
                {
                    pushed = true;
                    builder = builder.push(current.getNodeName());
                    addAttributes(current.getAttributes(), builder);
                }
                if (current.getChildNodes().getLength() == 1
                        && current.getFirstChild().getNodeValue() != null)
                {
                    // leaf node containing text
                    if (pushed)
                    {
                        builder = builder.pop();
                    }
                    addWme(builder, current.getNodeName(), current
                            .getFirstChild().getNodeValue().trim());
                }
                else if (!current.hasChildNodes() && !current.hasAttributes())
                {
                    // empty leaf node
                    if (pushed)
                    {
                        builder = builder.pop();
                    }
                    addWme(builder, current.getNodeName(), "");
                }
                else if (current.hasChildNodes()
                        && current.getNodeName() != null)
                {
                    // recursive call if not a leaf node
                    if (!pushed)
                    {
                        builder = builder.push(current.getNodeName());
                    }
                    getXmlTree(current.getChildNodes(), builder);
                    builder = builder.pop();
                }
                else
                {
                    // pop if none of the above are true
                    if (pushed)
                    {
                        builder = builder.pop();
                    }
                }
                xmlPath.popTag();
            }
        }
        return;
    }

    @Override
    protected void addAttributes(NamedNodeMap nnm, WmeBuilder<?> builder)
    {
        for (int i = 0; i < nnm.getLength(); i++)
        {
            Node n = nnm.item(i);

            xmlPath.pushTag(n.getNodeName());
            addWme(builder, n.getNodeName(), n.getNodeValue());
            xmlPath.popTag();
        }
    }

    /**
     * Add an XML path which should be treated as a float when added to JSoar
     * working memory.
     * 
     * @param path
     *            - the XML path
     * @throws TagAlreadyAddedException
     *             if the path was added as an integer path
     */
    public void addFloatTag(String path) throws TagAlreadyAddedException
    {
        if (!intTags.contains(path))
        {
            floatTags.add(path);
            return;
        }
        throw new TagAlreadyAddedException(path + " already added as integer.");
    }

    /**
     * Add an XML path which should be treated as an integer when added to JSoar
     * working memory.
     * 
     * @param path
     *            - the XML path
     * @throws TagAlreadyAddedException
     *             if the path was added as a float path
     */
    public void addIntTag(String path) throws TagAlreadyAddedException
    {
        if (!floatTags.contains(path))
        {
            intTags.add(path);
            return;
        }
        throw new TagAlreadyAddedException(path + " already added as float.");
    }

    /**
     * Add a WME to the builder. The type of the WME value is assumed to be a
     * <code>String</code> unless the XML path of the attribute has been added
     * as a integer or float type.
     * 
     * @param builder
     *            - the JSoar builder
     * @param attribute
     *            - the attribute of the WME
     * @param value
     *            - the value of the WME
     */
    private void addWme(WmeBuilder<?> builder, String attribute, String value)
    {
        final String path = xmlPath.toString();
        if (floatTags.contains(path))
        {
            final Double doubleVal = Double.parseDouble(value);
            builder = builder.add(attribute, doubleVal);
        }
        else if (intTags.contains(path))
        {
            final Long intVal = Long.parseLong(value);
            builder = builder.add(attribute, intVal);
        }
        else
        {
            builder = builder.add(attribute, value);
        }
    }
}

/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 21, 2009
 */
package org.jsoar.kernel.io.xml;

import org.jsoar.kernel.events.InputEvent;
import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.io.InputWmes;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.jsoar.util.events.SoarEvents;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Utilities for working with XML to WME conversions
 * 
 * @see XmlToWme
 * @author ray
 */
public class XmlToWmeTools
{
    /**
     * Add an XML document to the input link using {@link DefaultWmeToXml} as a converter.
     * 
     * <p>This is a convenience version of {@link #addXmlInput(InputOutput, XmlToWme, Element, String)}
     * equivalent to: {@code addXmlInput(io, converter, root, null)}.
     * 
     * @param io the io interface
     * @param root the root of the XML tree to convert, i.e. a Document or Element
     * @throws IllegalArgumentException if root is not a Document or Element object
     * @see #addXmlInput(InputOutput, Node, XmlToWme, String)
     */
    public static void addXmlInput(final InputOutput io, final Node root)
    {
        addXmlInput(io, root, DefaultXmlToWme.forInput(io), null);
    }
    
    /**
     * Add an XML document to the input link.
     * 
     * <p>This is a convenience version of {@link #addXmlInput(InputOutput, XmlToWme, Element, String)}
     * equivalent to: {@code addXmlInput(io, converter, root, null)}.
     * 
     * @param io the io interface
     * @param converter XML converter to use
     * @param root the root of the XML tree to convert, i.e. a Document or Element
     * @throws IllegalArgumentException if root is not a Document or Element object
     * @see #addXmlInput(InputOutput, Node, XmlToWme, String)
     */
    public static void addXmlInput(final InputOutput io, final Node root, final XmlToWme converter)
    {
        addXmlInput(io, root, converter, null);
    }
    
    /**
     * Add an XML document to the input link. The XML will be converted to working memory
     * during the next {@link InputEvent} callback. Note that since this may occur in a
     * separate thread, care must be taken to ensure that the XML tree is not modified
     * after this method is called.
     * 
     * <p>Here is an example for reading an XML file onto the input link:
     * <pre>{@code
     *  final InputOutput io = agent.getInputOutput();
     *  addXmlInput(io, XmlTools.parse(new FileReader("data.xml")), DefaultXmlToWme.forInput(io), "data");
     *  // In real life, be sure to close the file, handle errors, etc.
     * }</pre>
     * 
     * @param io the io interface
     * @param converter XML converter to use
     * @param root the root of the XML tree to convert, i.e. a Document or Element
     * @param attr the attribute of the root WME that is created. If {@code null}, then
     *      the tag name of the root element is used
     * @throws IllegalArgumentException if root is not a Document or Element object
     */
    public static void addXmlInput(final InputOutput io, Node root, final XmlToWme converter, final String attr)
    {
        final Element element;
        if(root instanceof Document)
        {
            element = ((Document) root).getDocumentElement();
        }
        else if(root instanceof Element)
        {
            element = (Element) root;
        }
        else
        {
            throw new IllegalArgumentException("root must be of type Document or Element, got " + root.getClass());
        }
        
        SoarEvents.listenForSingleEvent(io.getEvents(), InputEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                InputWmes.add(io, attr != null ? attr : element.getTagName(), converter.fromXml(element));
            }});
    }
}

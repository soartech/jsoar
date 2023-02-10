/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on Apr 27, 2009
 */
package org.jsoar.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author ray
 */
public class XmlTools
{
    private static final Logger logger = LoggerFactory.getLogger(XmlTools.class);
    
    /**
     * Construct a new document builder
     * 
     * @return a new document builder
     * @throws RuntimeException if there is an error
     */
    public static DocumentBuilder createDocumentBuilder()
    {
        final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        docFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        try
        {
            return docFactory.newDocumentBuilder();
        }
        catch(ParserConfigurationException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    public static Document parse(InputStream is) throws SAXException, IOException
    {
        return createDocumentBuilder().parse(is);
    }
    
    public static Document parse(Reader reader) throws SAXException, IOException
    {
        return createDocumentBuilder().parse(new InputSource(reader));
    }
    
    public static Document parse(String input) throws SAXException, IOException
    {
        return parse(new StringReader(input));
    }
    
    /**
     * Serialize the node to a stream
     * 
     * @param node the node
     * @param out the output stream
     * @throws IOException if there is an error writing to the stream
     */
    public static void write(Node node, OutputStream out) throws IOException
    {
        if(node == null)
        {
            throw new NullPointerException("doc must not be null");
        }
        if(out == null)
        {
            throw new NullPointerException("out must not be null");
        }
        try
        {
            TransformerFactory xformFactory = TransformerFactory.newInstance();
            xformFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            xformFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            Transformer idTransform = xformFactory.newTransformer();
            idTransform.setOutputProperty(OutputKeys.VERSION, "1.0");
            idTransform.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            idTransform.setOutputProperty(OutputKeys.STANDALONE, "no");
            Source input = new DOMSource(node);
            Result output = new StreamResult(out);
            idTransform.transform(input, output);
        }
        catch(TransformerConfigurationException e)
        {
            throw new IllegalStateException(e);
        }
        catch(TransformerFactoryConfigurationError e)
        {
            throw new IllegalStateException(e);
        }
        catch(TransformerException e)
        {
            throw new IllegalStateException(e);
        }
        out.flush();
    }
    
    /**
     * Attempt to pretty-print the given node to the given output stream. If
     * it can't pretty-print (no Load and Save impl), it will fall back to
     * non-pretty-printed.
     * 
     * @param node the XML node to write
     * @param out the output stream
     * @throws IOException
     */
    public static void writePretty(Node node, OutputStream out) throws IOException
    {
        // Pretty-prints a DOM document to XML using DOM Load and Save's LSSerializer.
        // Note that the "format-pretty-print" DOM configuration parameter can
        // only be set in JDK 1.6+.
        final DOMImplementation domImpl;
        if(node instanceof Document)
        {
            domImpl = ((Document) node).getImplementation();
        }
        else
        {
            domImpl = node.getOwnerDocument().getImplementation();
        }
        if(domImpl.hasFeature("LS", "3.0") && domImpl.hasFeature("Core", "2.0"))
        {
            final DOMImplementationLS domImplLS = (DOMImplementationLS) domImpl.getFeature("LS", "3.0");
            final LSSerializer lss = domImplLS.createLSSerializer();
            if(lss.getDomConfig().canSetParameter("format-pretty-print", true))
            {
                lss.getDomConfig().setParameter("format-pretty-print", true);
                
                final LSOutput lsOut = domImplLS.createLSOutput();
                lsOut.setEncoding("UTF-8");
                lsOut.setByteStream(out);
                
                lss.write(node, lsOut);
            }
            else
            {
                logger.warn("DOMConfiguration 'format-pretty-print' parameter isn't settable. Won't pretty print.");
                write(node, out);
            }
        }
        else
        {
            logger.warn("DOM 3.0 LS and/or DOM 2.0 Core not supported. Won't pretty print.");
            write(node, out);
        }
    }
    
    public static String toString(Node node)
    {
        try
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            write(node, out);
            return out.toString();
        }
        catch(IOException ex)
        {
            // ByteArrayOutputStream never throws IOException
            throw new IllegalStateException(ex);
        }
    }
    
    public static String toPrettyString(Node node)
    {
        try
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writePretty(node, out);
            return out.toString();
        }
        catch(IOException ex)
        {
            // ByteArrayOutputStream never throws IOException
            throw new IllegalStateException(ex);
        }
    }
    
    public static Element getFirstChild(Element parent)
    {
        return getFirstChild(parent, null);
    }
    
    public static Element getFirstChild(Element parent, String name)
    {
        return getNextChild(parent.getFirstChild(), name, true);
    }
    
    public static Element getNextChild(Node child)
    {
        return getNextChild(child, null);
    }
    
    public static Element getNextChild(Node child, String name)
    {
        return getNextChild(child, name, false);
    }
    
    public static Element getNextChild(Node child, String name,
            boolean checkFirst)
    {
        if(child == null)
        {
            return null;
        }
        if(checkFirst && child instanceof Element)
        {
            final Element e = (Element) child;
            if(name == null || name.equals(e.getTagName()))
            {
                return e;
            }
        }
        for(Node n = child.getNextSibling(); n != null; n = n.getNextSibling())
        {
            if(n instanceof Element)
            {
                final Element e = (Element) n;
                if(name == null || name.equals(e.getTagName()))
                {
                    return e;
                }
            }
        }
        return null;
    }
}

/*
 * (c) 2009  Soar Technology, Inc.
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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author ray
 */
public class XmlTools
{

    /**
     * Construct a new document builder
     * 
     * @return a new document builder
     * @throws RuntimeException if there is an error
     */
    public static DocumentBuilder createDocumentBuilder()
    {
        final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        try
        {
           return docFactory.newDocumentBuilder();
        }
        catch (ParserConfigurationException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    public static Document parse(InputStream is) throws SAXException, IOException {
        return createDocumentBuilder().parse(is);
    }

    public static Document parse(Reader reader) throws SAXException, IOException {
        return createDocumentBuilder().parse(new InputSource(reader));
    }

    public static Document parse(String input) throws SAXException, IOException {
        return parse(new StringReader(input));
    }
    
    public static void write(Node node, OutputStream out) throws TransformerConfigurationException, TransformerException, IOException 
    {
        if(node == null) 
        {
            throw new NullPointerException("doc must not be null");
        }
        if(out == null) 
        {
            throw new NullPointerException("out must not be null");
        }
        TransformerFactory xformFactory = TransformerFactory.newInstance();
        Transformer idTransform = xformFactory.newTransformer();
        Source input = new DOMSource(node);
        Result output = new StreamResult(out);
        idTransform.transform(input, output);
        out.flush();
    }

    public static String toString(Node node) {
        try 
        {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            write(node, out);
            return out.toString();
        } 
        catch (TransformerConfigurationException ex) 
        {
            throw new IllegalStateException(ex);
        }
        catch (TransformerException ex) 
        {
            throw new IllegalStateException(ex);
        } 
        catch (IOException ex) 
        {
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
        if (child == null)
        {
            return null;
        }
        if (checkFirst && child instanceof Element)
        {
            final Element e = (Element) child;
            if (name == null || name.equals(e.getTagName()))
            {
                return e;
            }
        }
        for (Node n = child.getNextSibling(); n != null; n = n.getNextSibling())
        {
            if (n instanceof Element)
            {
                final Element e = (Element) n;
                if (name == null || name.equals(e.getTagName()))
                {
                    return e;
                }
            }
        }
        return null;
    }

}

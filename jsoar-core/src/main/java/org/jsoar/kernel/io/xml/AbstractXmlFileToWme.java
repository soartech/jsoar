/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on July 10, 2012
 */
package org.jsoar.kernel.io.xml;

import java.io.File;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jsoar.kernel.io.InputBuilder;
import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.io.xml.XmlToWme;
import org.jsoar.kernel.symbols.Identifier;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

/**
 * Abstract class which uses JAXP to obtain a graph structure from an XML file.
 * 
 * @author chris.kawatsu
 * 
 */
abstract class AbstractXmlFileToWme implements XmlFileToWme, XmlToWme {

	protected InputBuilder builder;

	public AbstractXmlFileToWme(InputOutput io) {
		builder = InputBuilder.create(io);
	}

	@Override
	public void xmlToWme(File file) {
		Element root = getRootElement(file);
		fromXml(root);
	}

	@Override
	public Identifier fromXml(Element element) {
		builder = builder.push(element.getNodeName());
		addAttributes(element.getAttributes(), builder);
		getXmlTree(element.getChildNodes(), builder);
		return builder.top().id;
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
	 * @param nodeList
	 *            - the list of nodes
	 * 
	 * @param builder
	 *            - the JSoar builder
	 */
	abstract void getXmlTree(NodeList nodelList, InputBuilder builder);

	/**
	 * Add the attributes contained in the <code>NamedNodeMap</code> to the
	 * builder.
	 * 
	 * @param nnm
	 *            - a collection of attribute nodes
	 * @param builder
	 *            - the JSoar builder
	 */
	abstract void addAttributes(NamedNodeMap nnm, InputBuilder builder);

	/**
	 * Parse an XML file and get its root {@link Element}.
	 * 
	 * @param f
	 *            - the XML file to parse
	 * @return The root <code>Element</code> of the XML file. Returns
	 *         <code>null</code> if the file cannot be read or an error occurs
	 *         during parsing.
	 */
	protected static Element getRootElement(File f) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse(f);
			return dom.getDocumentElement();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}
}

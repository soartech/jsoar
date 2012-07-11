/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on July 10, 2012
 */
package org.jsoar.kernel.io.xml;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.memory.WmeFactory;
import org.jsoar.kernel.memory.WmeBuilder;
import org.jsoar.kernel.symbols.Identifier;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A {@link XmlFileToWme} implementation in which the XML tags with types other
 * than string are manually specified. XML tags are specified using their full
 * path. For example in:<br>
 * <br>
 * 
 * &lt;Message><br>
 * &nbsp;&nbsp;&nbsp;&lt;MessageValue>1&lt;/MessageValue><br>
 * &lt;/Message><br>
 * <br>
 * 
 * the value of MessageValue would be specified as a float using the path
 * <code>Message.MessageValue</code>.<br>
 * <br>
 * 
 * Float and integer XML paths are added using {@link #addFloatTag(String)
 * addFloatTag} and {@link #addIntTag(String) addIntTag}. The values added as
 * floats and those added as integers are enforced to be mutually exclusive.
 * 
 * @author chris.kawatsu
 * 
 */
public class ManualTypeXmlToWme extends AbstractXmlFileToWme {

	private XmlPath xmlPath;
	private Set<String> floatTags;
	private Set<String> intTags;

	public ManualTypeXmlToWme(InputOutput io) {
		super(io);
		xmlPath = new XmlPath();
		floatTags = new HashSet<String>();
		intTags = new HashSet<String>();
	}
	
	public ManualTypeXmlToWme(WmeFactory<?> wmeFactory) {
		super(wmeFactory);
		xmlPath = new XmlPath();
		floatTags = new HashSet<String>();
		intTags = new HashSet<String>();
	}

	@Override
	public Identifier xmlToWme(File file) {
		Element root = getRootElement(file);
		xmlPath.pushTag(root.getNodeName());
		return super.fromXml(root);
	}

	@Override
	public Identifier fromXml(Element element) {
		xmlPath.pushTag(element.getNodeName());
		return super.fromXml(element);
	}

	@Override
	protected void getXmlTree(NodeList nodeList,
			WmeBuilder<?> builder) {

		for (int i = 0; i < nodeList.getLength(); i++) {
			Node current = nodeList.item(i);
			// ignore nodes that are not element nodes (text and attribute nodes
			// are handled as a special case)
			if (current.getNodeType() == Node.ELEMENT_NODE) {
				xmlPath.pushTag(current.getNodeName());
				boolean pushed = false;

				// add attribute nodes, if any
				if (current.hasAttributes()) {
					pushed = true;
					builder = builder.push(current.getNodeName());
					addAttributes(current.getAttributes(), builder);
				}
				if (current.getChildNodes().getLength() == 1) {
					// leaf node containing text
					if (pushed)
						builder = builder.pop();
					addWme(builder, current.getNodeName(), current
							.getFirstChild().getNodeValue().trim());
				} else if (!current.hasChildNodes() && !current.hasAttributes()) {
					// empty leaf node
					if (pushed)
						builder = builder.pop();
					addWme(builder, current.getNodeName(), "");
				} else if (current.hasChildNodes()
						&& current.getNodeName() != null) {
					// recursive call if not a leaf node
					if (!pushed)
						builder = builder.push(current.getNodeName());
					getXmlTree(current.getChildNodes(), builder);
					builder = builder.pop();
				} else {
					// pop if none of the above are true
					if (pushed)
						builder = builder.pop();
				}
				xmlPath.popTag();
			}
		}
		return;
	}

	@Override
	protected void addAttributes(NamedNodeMap nnm,
			WmeBuilder<?> builder) {
		for (int i = 0; i < nnm.getLength(); i++) {
			Node n = nnm.item(i);
			String val = n.getNodeValue().trim();
			if (val.length() > 0) {
				xmlPath.pushTag(n.getNodeName());
				addWme(builder, n.getNodeName(), n.getNodeValue());
				xmlPath.popTag();
			}
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
	public void addFloatTag(String path) throws TagAlreadyAddedException {
		if (!intTags.contains(path)) {
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
	public void addIntTag(String path) throws TagAlreadyAddedException {
		if (!floatTags.contains(path)) {
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
	private void addWme(WmeBuilder<?> builder,
			String attribute, String value) {
		String path = xmlPath.toString();
		System.out.println(path+"."+attribute+" "+value);
		if (floatTags.contains(path)) {
			Double doubleVal = Double.parseDouble(value);
			builder = builder.add(attribute, doubleVal);
		} else if (intTags.contains(path)) {
			Integer intVal = Integer.parseInt(value);
			builder = builder.add(attribute, intVal);
		} else {
			builder = builder.add(attribute, value);
		}
	}
}

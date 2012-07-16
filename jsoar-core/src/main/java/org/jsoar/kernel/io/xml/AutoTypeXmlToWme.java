/*
 * Copyright (c) 2012 Dave Ray <daveray@gmail.com>
 *
 * Created on July 10, 2012
 */
package org.jsoar.kernel.io.xml;

import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.memory.WmeBuilder;
import org.jsoar.kernel.memory.WmeFactory;
import org.jsoar.kernel.rhs.functions.RhsFunctionContext;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A {@link XmlFileToWme} implementation which tries to automatically determine
 * the type (float, int, or string) of XML values.
 * <ul>
 * <li>If {@link Long#parseLong(String) parseLong} is successful the type is
 * <code>Integer</code>.
 * <li>Next, if {@link Double#parseDouble(String) parseDouble} is successful the type is
 * <code>Double</code>.
 * <li>Otherwise the type is <code>String</code>.
 * </ul>
 * 
 * For example, the following XML:<br><br>
 * 
 * &lt;Message id="1"><br>
 * &nbsp;&nbsp;&nbsp;&lt;MessageValue>1.0&lt;/MessageValue><br>
 * &lt;/Message><br><br>
 * 
 * results in<br><br>
 * 
 * <code>^id 1</code><br>
 * <code>^MessageValue 1.0</code><br><br>
 * 
 * when using {@link #fromXml(org.w3c.dom.Element) fromXml}. Note that the root XML tag is 
 * ignored, but its attributes are still added. If {@link #xmlToWme(java.io.File, InputOutput) xmlToWme}
 * is used, the message is added directly to the input link and the root tag is not ignored. This
 * results in the following WME structure:<br><br>
 * 
 * <code>^io</code><br>
 * &nbsp;&nbsp;&nbsp;<code>^input-link</code><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<code>^Message</code><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<code>^id 1</code><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<code>^MessageValue 1.0</code><br><br>
 * 
 * @author chris.kawatsu
 * 
 */
public class AutoTypeXmlToWme extends AbstractXmlFileToWme {

	public AutoTypeXmlToWme(InputOutput io) {
		super(io);
	}

	public AutoTypeXmlToWme(WmeFactory<?> io) {
		super(io);
	}

	/**
	 * Construct an XML to WME converter for RHS functions, i.e. it uses an
	 * instance of {@link RhsFunctionContext} to generate symbols and WMEs.
	 * 
	 * @param rhsContext
	 *            the RHS function context to use
	 * @return new converter
	 */
	public static AutoTypeXmlToWme forRhsFunction(RhsFunctionContext rhsContext) {
		return new AutoTypeXmlToWme(rhsContext);
	}

	@Override
	protected void getXmlTree(NodeList nodeList,
			WmeBuilder<?> builder) {

		for (int i = 0; i < nodeList.getLength(); i++) {
			Node current = nodeList.item(i);
			// ignore nodes that are not element nodes (text and attribute nodes
			// are handled as a special case)
			if (current.getNodeType() == Node.ELEMENT_NODE) {
				boolean pushed = false;

				// add attribute nodes, if any
				if (current.hasAttributes()) {
					pushed = true;
					builder = builder.push(current.getNodeName());
					addAttributes(current.getAttributes(), builder);
				}
				if (current.getChildNodes().getLength() == 1 && current.getFirstChild().getNodeValue() != null) {
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
				addWme(builder, n.getNodeName(), n.getNodeValue());
			}
		}
	}

	/**
	 * Add a WME to the builder. The type of the WME value is automatically
	 * determined. If <code>Integer.parse</code> is successful the type is
	 * <code>Integer</code>. Next, if <code>Double.parse</code> is successful
	 * the type is <code>Double</code>. Otherwise the type is
	 * <code>String</code>.
	 * 
	 * @param builder
	 * @param attribute
	 * @param value
	 */
	private void addWme(WmeBuilder<?> builder,
			String attribute, String value) {
		try {
			final Long intVal = Long.parseLong(value);
			builder = builder.add(attribute, intVal);
			return;
		} catch (NumberFormatException e) {
			// not an Integer if exception is thrown
		}

		try {
			final Double doubleVal = Double.parseDouble(value);
			builder = builder.add(attribute, doubleVal);
			return;
		} catch (NumberFormatException e) {
			// not a Double if exception is thrown
		}

		builder = builder.add(attribute, value);
		return;
	}
}

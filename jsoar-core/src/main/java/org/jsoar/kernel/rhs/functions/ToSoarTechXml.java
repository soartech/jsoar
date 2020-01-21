/*
 * Copyright (c) 2009  Dave Ray <daveray@gmail.com>
 *
 * Created on April 28, 2009
 */
package org.jsoar.kernel.rhs.functions;

import java.util.List;

import org.jsoar.kernel.io.xml.SoarTechWmeToXml;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.util.XmlTools;
import org.w3c.dom.Document;

/**
 * A RHS function that converts working memory to XML using {@link SoarTechWmeToXml}. 
 * 
 * @author ray
 */
public class ToSoarTechXml extends AbstractRhsFunctionHandler
{
    /**
     */
    public ToSoarTechXml()
    {
        super("to-st-xml", 1, Integer.MAX_VALUE);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rhs.functions.RhsFunctionHandler#execute(org.jsoar.kernel.rhs.functions.RhsFunctionContext, java.util.List)
     */
    @Override
    public Symbol execute(final RhsFunctionContext context, List<Symbol> arguments) throws RhsFunctionException
    {
        RhsFunctions.checkArgumentCount(this, arguments);
        
        String rootName = "xml";
        int i = 0;
        for(; i < arguments.size(); ++i)
        {
            final String arg = arguments.get(i).toString();
            if("--root".equals(arg))
            {
                if(i == arguments.size() - 1)
                {
                    throw new RhsFunctionException("Missing argument for --root option to '" + getName() + "'");
                }
                rootName = arguments.get(++i).toString();
            }
            else if(arg.startsWith("--"))
            {
                throw new RhsFunctionException("Unknown option '" + arg + "' to '" + getName() + "'");
            }
            else
            {
                break;
            }
        }
        
        if(i != arguments.size() - 1)
        {
            throw new RhsFunctionException("Expected single identifier argument to '" + getName() + "'");
        }
        final Identifier root = arguments.get(i).asIdentifier();
        if(root == null)
        {
            throw new RhsFunctionException("Argument to '" + getName() + "' RHS function must be an identifier.");
        }
        
        final SoarTechWmeToXml toXml = new SoarTechWmeToXml();
        final Document doc = toXml.toXml(root, rootName);
        
        return context.getSymbols().createString(XmlTools.toString(doc));
    }
}

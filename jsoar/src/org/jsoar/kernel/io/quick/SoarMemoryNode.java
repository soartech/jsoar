
package org.jsoar.kernel.io.quick;

import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbols;

class SoarMemoryNode
{
    // private static Logger logger = Logger.getLogger(SoarMemoryNode.class);
    
    private Wme wme;
    private Identifier idValue;

    private String name;
    
    private SoarMemoryNode parentNode;
    
    private MemoryNode memoryNode;
    
    private Identifier getIdValue()
    {
        return idValue;
    }
    
    public Wme getWME()
    {
        return wme;
    }

    private void setWME(Wme wme)
    {
        this.wme = wme;
        this.idValue = wme != null ? wme.getValue().asIdentifier() : null;
    }

    public SoarMemoryNode(Identifier idValue)
    {
        this.idValue = idValue;
        this.memoryNode = new MemoryNode();
    }

    public SoarMemoryNode(String name)
    {
        this.name = name;
        this.memoryNode = new MemoryNode();
    }
    
    public void setParentNode(SoarMemoryNode parentNode)
    {
        this.parentNode = parentNode;
    }
    
    private void createWME(InputOutput io, MemoryNode node)
    {
        assert(wme == null && parentNode != null);
        
        if(name == null)
        {
            return;
        }
        
        Identifier parentWME = (Identifier) parentNode.getIdValue();
        setWME(io.addInputWme(parentWME, 
                             Symbols.create(io.getSymbols(), name), 
                             Symbols.create(io.getSymbols(), node.getValue() != null ? node.getValue() : Symbols.NEW_ID)));
        
        if (node.isString())
        {
            String strVal = node.getStringValue();
            memoryNode.setStringValue(strVal);
        }
        else if (node.isInt())
        {
            int intVal = node.getIntValue();
            memoryNode.setIntValue(intVal);
        }
        else if (node.isDouble())
        {
            double doubleVal = node.getDoubleValue();
            memoryNode.setDoubleValue(doubleVal);
        }
        else
        {
            memoryNode.clearValue();
        }
    }

    private void updateWME(InputOutput io, MemoryNode node)
    {
        assert(wme != null && memoryNode.hasSameType(node));
        
        if(name == null)
        {
            return;
        }
        
        if(!memoryNode.valueIsEqual(node))
        {
            setWME(io.updateInputWme(wme, 
                    Symbols.create(io.getSymbols(), node.getValue() != null ? node.getValue() : Symbols.NEW_ID)));
            memoryNode.setValue(node);
        }
    }
    
    public void remove(InputOutput io)
    {
        if(wme != null)
        {
            io.removeInputWme(wme);
            setWME(null);
        }
    }
    
    public void synchronizeToMemoryNode(InputOutput io, MemoryNode node)
    {
        if (wme == null)
        {
            createWME(io, node);
        }
        else if (!memoryNode.hasSameType(node))
        {
            io.removeInputWme(wme);
            wme = null;
            createWME(io, node);
        }
        else
        {
            updateWME(io, node);
        }
    }
}


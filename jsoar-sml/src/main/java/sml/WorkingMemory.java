/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Nov 5, 2008
 */
package sml;

import java.util.Map;

import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.io.InputWme;
import org.jsoar.kernel.io.InputWmes;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.util.adaptables.Adaptables;

import com.google.common.collect.MapMaker;

/**
 * @author ray
 */
class WorkingMemory
{
    final Agent m_Agent ;
    private Identifier m_InputLink ;
    private Identifier m_OutputLink ; // this is initialized the first time an agent generates output; until then it is null
    
    private final Map<Wme, WMElement> wmeMap = new MapMaker().weakKeys().weakValues().makeMap();
    private final Map<org.jsoar.kernel.symbols.Identifier, IdentifierSymbol> idmap = new MapMaker().weakKeys().weakValues().makeMap();
    private final Map<Integer, WMElement> timetagMap = new MapMaker().weakValues().makeMap();;
       
    WorkingMemory(Agent agent)
    {
        this.m_Agent = agent;
    }
    
    IdentifierSymbol findId(org.jsoar.kernel.symbols.Identifier id)
    {
        IdentifierSymbol smlId = idmap.get(id);
        if(smlId == null)
        {
            smlId = new IdentifierSymbol(id);
            idmap.put(id, smlId);
        }
        return smlId;
    }

    WMElement findWme(Wme wme)
    {
        WMElement smlWme = wmeMap.get(wme);
        if(smlWme == null)
        {
            final Symbol v = wme.getValue();
            if(v.asIdentifier() != null)
            {
                smlWme = new Identifier(GetAgent(), findId(wme.getIdentifier()), wme);
            }
            else if(v.asString() != null)
            {
                smlWme = new StringElement(GetAgent(), findId(wme.getIdentifier()), wme);
            }
            else if(v.asInteger() != null)
            {
                smlWme = new IntElement(GetAgent(), findId(wme.getIdentifier()), wme);
                
            }
            else if(v.asDouble() != null)
            {
                smlWme = new FloatElement(GetAgent(), findId(wme.getIdentifier()), wme);
                
            }
            else
            {
                throw new RuntimeException("Unsupported WME value type " + v + ":" + v.getClass());
            }
            wmeMap.put(wme, smlWme);
        }
        return smlWme;
    }
    
    WMElement findWme(int timetag)
    {
        return timetagMap.get(timetag);
    }
    
    public void delete()
    {
        m_OutputLink.delete();
        m_InputLink.delete();
    }

    Agent          GetAgent()        { return m_Agent ; }

    InputOutput getIO()
    {
        return GetAgent().agent.getInputOutput();
    }
    // These functions are documented in the agent and handled here.
    Identifier     GetInputLink()
    {
        if (m_InputLink == null)
        {
            m_InputLink = new Identifier(GetAgent(), findId(getIO().getInputLink()));
        }

        return m_InputLink ;
        
    }
    Identifier     GetOutputLink()
    {
        if(m_OutputLink == null)
        {
            m_OutputLink = new Identifier(GetAgent(), findId(getIO().getOutputLink()));
        }
        return m_OutputLink;
    }
    
    StringElement  CreateStringWME(Identifier parent, String pAttribute, String pValue)
    {
        final StringElement result = new StringElement(GetAgent(), parent, InputWmes.add(getIO(), parent.m_pSymbol.id, pAttribute, pValue));
        wmeMap.put(result.wme, result);
        return result;
    }
    
    IntElement     CreateIntWME(Identifier parent, String pAttribute, int value)
    {
        final IntElement result = new IntElement(GetAgent(), parent, InputWmes.add(getIO(), parent.m_pSymbol.id, pAttribute, value));
        wmeMap.put(result.wme, result);
        return result;
    }
    FloatElement   CreateFloatWME(Identifier parent, String pAttribute, double value)
    {
        final FloatElement result = new FloatElement(GetAgent(), parent, InputWmes.add(getIO(), parent.m_pSymbol.id, pAttribute, value));
        wmeMap.put(result.wme, result);
        return result;
    }

    Identifier     CreateIdWME(Identifier parent, String pAttribute)
    {
        final Identifier result = new Identifier(GetAgent(), parent.m_pSymbol, InputWmes.add(getIO(), parent.m_pSymbol.id, pAttribute, Symbols.NEW_ID));
        wmeMap.put(result.wme, result);
        return result;
    }
    Identifier     CreateSharedIdWME(Identifier parent, String pAttribute, Identifier pSharedValue)
    {
        final Identifier result = new Identifier(GetAgent(), parent.m_pSymbol, InputWmes.add(getIO(), parent.m_pSymbol.id, pAttribute, pSharedValue.m_ID));
        wmeMap.put(result.wme, result);
        return result;
    }

    void UpdateString(StringElement pWME, String pValue)
    {
        if (pWME == null || pValue == null)
            return ;

        assert(m_Agent == pWME.GetAgent()) ;

        // If the value hasn't changed and we're set to not blink the wme (remove/add it again)
        // then there's no work to do.
        if (!m_Agent.IsBlinkIfNoChange())
        {
            if (pWME.GetValue().equals(pValue))
                return ;
        }

        final InputWme iw = Adaptables.adapt(pWME.wme, InputWme.class);
        if(iw == null)
        {
            throw new RuntimeException("Attempt to modify non-input WME: " + pWME.wme);
        }
        iw.update(GetAgent().agent.getSymbols().createString(pValue));
    }
    
    void UpdateInt(IntElement pWME, int value)
    {
        if (pWME == null)
            return ;

        assert(m_Agent == pWME.GetAgent()) ;

        // If the value hasn't changed and we're set to not blink the wme (remove/add it again)
        // then there's no work to do.
        if (!m_Agent.IsBlinkIfNoChange())
        {
            if (pWME.GetValue() == value)
                return ;
        }

        final InputWme iw = Adaptables.adapt(pWME.wme, InputWme.class);
        if(iw == null)
        {
            throw new RuntimeException("Attempt to modify non-input WME: " + pWME.wme);
        }
        iw.update(GetAgent().agent.getSymbols().createInteger(value));
    }
    
    void UpdateFloat(FloatElement pWME, double value)
    {
        if (pWME == null)
            return ;

        assert(m_Agent == pWME.GetAgent()) ;

        // If the value hasn't changed and we're set to not blink the wme (remove/add it again)
        // then there's no work to do.
        if (!m_Agent.IsBlinkIfNoChange())
        {
            // Note: There's no error margin allowed on this, so the value must match exactly or
            // the wme will blink.
            if (pWME.GetValue() == value)
                return ;
        }

        final InputWme iw = Adaptables.adapt(pWME.wme, InputWme.class);
        if(iw == null)
        {
            throw new RuntimeException("Attempt to modify non-input WME: " + pWME.wme);
        }
        iw.update(GetAgent().agent.getSymbols().createDouble(value));
    }

    boolean DestroyWME(WMElement pWME)
    {
        assert(m_Agent == pWME.GetAgent()) ;
        
        final InputWme iw = Adaptables.adapt(pWME.wme, InputWme.class);
        if(iw == null)
        {
            throw new RuntimeException("Attempt to destroy non-input WME: " + pWME.wme);
        }

        wmeMap.remove(pWME);
        iw.remove();
        
        return true ;        
    }
    
    boolean IsCommitRequired() { return false; }
    
    boolean Commit()
    {
        return true;
    }
    boolean IsAutoCommitEnabled(){ return m_Agent.IsAutoCommitEnabled() ; }

}

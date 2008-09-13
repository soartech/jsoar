/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 12, 2008
 */
package org.jsoar.kernel;

import org.jsoar.kernel.memory.OSupport;
import org.jsoar.kernel.memory.PreferenceMemory;
import org.jsoar.kernel.memory.RecognitionMemory;
import org.jsoar.kernel.memory.TemporaryMemory;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.rete.Rete;
import org.jsoar.kernel.rete.SoarReteListener;
import org.jsoar.kernel.symbols.SymbolFactory;

/**
 * @author ray
 */
public class SoarContext
{
    public final PredefinedSymbols predefinedSyms = new PredefinedSymbols();
    public final SymbolFactory syms = predefinedSyms.getSyms();
    public final VariableGenerator variableGenerator = new VariableGenerator(syms);
    public final Rete rete = new Rete(variableGenerator);
    public final WorkingMemory workingMemory = new WorkingMemory(rete, predefinedSyms.operator_symbol, predefinedSyms.name_symbol);
    public final TemporaryMemory tempMemory = new TemporaryMemory();
    public final PreferenceMemory prefMemory = new PreferenceMemory(tempMemory, predefinedSyms.operator_symbol);
    public final OSupport osupport = new OSupport(predefinedSyms);
    public final SoarReteListener soarReteListener = new SoarReteListener(rete, predefinedSyms.operator_symbol);
    public final RecognitionMemory recMemory = new RecognitionMemory(this);
    
    public final Decider decider = new Decider(this);
    public final Consistency consistency = new Consistency();
    
    /**
     * agent.h:728:operand2_mode
     */
    public boolean operand2_mode = true;
    
    /**
     * agent.h:688:attribute_preferences_mode
     */
    public int attribute_preferences_mode = 0;
}

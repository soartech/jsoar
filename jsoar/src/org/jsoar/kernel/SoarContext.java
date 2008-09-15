/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 12, 2008
 */
package org.jsoar.kernel;

import java.io.OutputStreamWriter;

import org.jsoar.kernel.io.InputOutput;
import org.jsoar.kernel.learning.Chunker;
import org.jsoar.kernel.learning.ReinforcementLearning;
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
    public int MAX_GOAL_DEPTH = 100;
    
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
    
    public final Chunker chunker = new Chunker();
    public final ReinforcementLearning rl = new ReinforcementLearning();
    public final DecisionManipulation decisionManip = new DecisionManipulation();
    public final Exploration exploration = new Exploration();
    public final InputOutput io = new InputOutput();
    
    private Printer printer = new Printer(new OutputStreamWriter(System.out));
    
    /**
     * agent.h:728:operand2_mode
     */
    public boolean operand2_mode = true;
    
    /**
     * agent.h:688:attribute_preferences_mode
     */
    public int attribute_preferences_mode = 0;

    /**
     * @return the printer
     */
    public Printer getPrinter()
    {
        return printer;
    }
    
    
}

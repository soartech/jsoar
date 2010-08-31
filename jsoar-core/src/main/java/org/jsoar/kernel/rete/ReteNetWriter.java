/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 28, 2009
 */
package org.jsoar.kernel.rete;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.symbols.DoubleSymbol;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.Arguments;
import org.jsoar.util.adaptables.Adaptables;

/**
 * @author ray
 */
public class ReteNetWriter
{
    private final Agent context;
    private final SymbolFactoryImpl syms;
    private final Rete rete;
    
    public ReteNetWriter(Agent context)
    {
        Arguments.checkNotNull(context, "context");
        
        this.context = context;
        this.syms = Adaptables.require(getClass(), this.context, SymbolFactoryImpl.class);
        this.rete = Adaptables.require(getClass(), context, Rete.class);
    }
    
    
    /**
     * Write a rete net to the given output stream. All rules and
     * symbols are written.
     * 
     * <p>The stream is not closed on completion
     * 
     * @param os the output stream to write to
     * @throws IOException if an error occurs while writing
     */
    public void write(OutputStream os) throws IOException
    {
        final DataOutputStream dos = new DataOutputStream(os);
        
        try {
            System.gc(); // This is only to try to get rid of any symbols that may be hanging around
                         // they won't hurt the rete if they stay, just make the image bigger.
            dos.writeUTF(ReteNetReader.MAGIC_STRING);
            dos.writeInt(ReteNetReader.FORMAT_VERSION);
            final Map<Symbol, Integer> symbolIndex = writeAllSymbols(dos);
            final Map<AlphaMemory, Integer> amIndex = writeAlphaMemories(dos, rete.getAllAlphaMemories(), symbolIndex);
        }
        finally {
        }
        
    }
    
    private static interface SymbolWriter<T>
    {
        void write(DataOutputStream dos, T s) throws IOException;
    }
    
    private Map<Symbol, Integer> writeAllSymbols(DataOutputStream dos) throws IOException
    {
        final Map<Symbol, Integer> symbolIndex = new HashMap<Symbol, Integer>();
        
        // symbol index numbers start at one so we can use zero for "no symbol" in alpha memories
        int nextIndex = writeSymbolList(dos, symbolIndex, 1,
            syms.getSymbols(StringSymbol.class),
            new SymbolWriter<StringSymbol>() {
                public void write(DataOutputStream dos, StringSymbol s) throws IOException { dos.writeUTF(s.getValue()); }
        });
        nextIndex = writeSymbolList(dos, symbolIndex, nextIndex,
                syms.getSymbols(Variable.class),
                new SymbolWriter<Variable>() {
                    public void write(DataOutputStream dos, Variable s) throws IOException { dos.writeUTF(s.name); }
            });
        nextIndex = writeSymbolList(dos, symbolIndex, nextIndex,
                syms.getSymbols(IntegerSymbol.class),
                new SymbolWriter<IntegerSymbol>() {
                    public void write(DataOutputStream dos, IntegerSymbol s) throws IOException { dos.writeInt(s.getValue()); }
            });
        nextIndex = writeSymbolList(dos, symbolIndex, nextIndex,
                syms.getSymbols(DoubleSymbol.class),
                new SymbolWriter<DoubleSymbol>() {
                    public void write(DataOutputStream dos, DoubleSymbol s) throws IOException { dos.writeDouble(s.getValue()); }
            });
        
        return symbolIndex;
    }
    
    private static <T extends Symbol> int writeSymbolList(DataOutputStream dos,
            Map<Symbol, Integer> symbolIndex, int nextIndex, List<T> symbols, SymbolWriter<T> writer) throws IOException
    {
        dos.writeInt(symbols.size());
        for(T s : symbols)
        {
            writer.write(dos, s);
            nextIndex = indexSymbol(s, symbolIndex, nextIndex);
        }
        
        return nextIndex;
    }

    private static int indexSymbol(Symbol s, Map<Symbol, Integer> symbolIndex, int nextIndex)
    {
        symbolIndex.put(s, nextIndex);
        return nextIndex++;
    }    
    
    private static int getSymbolIndex(Map<Symbol, Integer> symbolIndex, Symbol s)
    {
        if(s == null)
        {
            return 0;
        }
        final Integer i = symbolIndex.get(s);
        
        return i != null ? i.intValue() : 0;
    }
    
    private static Map<AlphaMemory, Integer> writeAlphaMemories(DataOutputStream dos, List<AlphaMemory> ams, Map<Symbol, Integer> symbolIndex) throws IOException
    {
        final Map<AlphaMemory, Integer> amIndex = new HashMap<AlphaMemory, Integer>();
        
        dos.writeInt(ams.size());
        int nextIndex = 1; 
        for(AlphaMemory am : ams)
        {
            writeAlphaMemory(dos, am, symbolIndex);
            amIndex.put(am, nextIndex++);
        }
        return amIndex;
    }

    private static void writeAlphaMemory(DataOutputStream dos, AlphaMemory am,
            Map<Symbol, Integer> symbolIndex) throws IOException
    {
        dos.writeInt(getSymbolIndex(symbolIndex, am.id));
        dos.writeInt(getSymbolIndex(symbolIndex, am.attr));
        dos.writeInt(getSymbolIndex(symbolIndex, am.value));
        dos.writeBoolean(am.acceptable);
    }
    
    /**
     *    type (1 byte): 0=null, 1=one var, 2=list
     *    if one var: 4 bytes (symindex)
     *    if list: 4 bytes (number of items) + list of symindices
     *
     * @param dos
     * @param varNames
     * @throws IOException 
     */
    private static void writeVarNames(DataOutputStream dos, Object varNames, Map<Symbol, Integer> symbolIndex) throws IOException
    {
        if(varNames == null)
        {
            dos.write((byte) 0);
        }
        else if(VarNames.varnames_is_one_var(varNames))
        {
            dos.write((byte) 1);
            dos.write(symbolIndex.get(VarNames.varnames_to_one_var(varNames)).intValue());
        }
        else
        {
            dos.write((byte) 2);
            final List<Variable> vars = VarNames.varnames_to_var_list(varNames);
            dos.write(vars.size());
            for(Variable v : vars)
            {
                dos.write(symbolIndex.get(v).intValue());
            }
        }
    }
    
    private static void writeNodeVarNames(DataOutputStream dos, Map<Symbol, Integer> symbolIndex, NodeVarNames nvn, ReteNode node) throws IOException
    {
        while (true) 
        {
            if (node.node_type == ReteNodeType.DUMMY_TOP_BNODE) return;
            if (node.node_type == ReteNodeType.CN_BNODE) 
            {
                node = node.b_cn().partner.parent;
                nvn = nvn.bottom_of_subconditions;
                continue;
            }
            writeVarNames(dos, nvn.fields.id_varnames, symbolIndex);
            writeVarNames(dos, nvn.fields.attr_varnames, symbolIndex);
            writeVarNames(dos, nvn.fields.value_varnames, symbolIndex);
            nvn = nvn.parent;
            node = node.real_parent_node();
        }        
    }
}

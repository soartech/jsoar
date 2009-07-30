/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jul 28, 2009
 */
package org.jsoar.kernel.rete;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.symbols.DoubleSymbol;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.Arguments;
import org.jsoar.util.adaptables.Adaptables;

/**
 * @author ray
 */
public class ReteNetReader
{
    public static final String MAGIC_STRING = "JSoarCompactReteNet";
    public static final int FORMAT_VERSION = 1;
    
    private final Agent context;
    private final SymbolFactoryImpl syms;
    private final Rete rete;
    
    public ReteNetReader(Agent context)
    {
        Arguments.checkNotNull(context, "context");
        
        this.context = context;
        this.syms = Adaptables.require(getClass(), context, SymbolFactoryImpl.class);
        this.rete = Adaptables.require(getClass(), context, Rete.class);
    }
    
    /**
     * Read a rete network from the given input stream. The agent
     * will be reinitialized, all productions will be excised and then
     * the rete network will be loaded. 
     * 
     * <p>This method does not close the input stream
     * 
     * @param is the input stream to read from
     * @throws IOException
     * @throws SoarException if an error occurs
     */
    public void read(InputStream is) throws IOException, SoarException
    {
        final DataInputStream dis = new DataInputStream(is);
        final String magic = dis.readUTF();
        if(!MAGIC_STRING.equals(magic))
        {
            throw new SoarException("Input does not appear to be a valid JSoar rete net");
        }
        final int version = dis.readInt();
        if(version != FORMAT_VERSION)
        {
            throw new SoarException(String.format("Unsupported JSoar rete net version. Expected %d, got %d", FORMAT_VERSION, version));
        }
        
        final List<Symbol> symbolMap = readAllSymbols(dis);
        final List<AlphaMemory> alphaMemories = readAlphaMemories(dis, symbolMap);
    }
    
    private static interface SymbolReader<T extends Symbol>
    {
        T read(DataInputStream dis) throws IOException;
    }
    
    /**
     * Read all symbols, indexed by their rete-net symbol table index.
     * 
     * @param dis
     * @return
     * @throws IOException 
     * @throws SoarException 
     * @see ReteNetWriter#writeAllSymbols
     */
    private List<Symbol> readAllSymbols(DataInputStream dis) throws IOException, SoarException
    {
        final List<Symbol> result = new ArrayList<Symbol>();
        result.add(null); // symbol 0 is null (see writeAllSymbols)
        
        result.addAll(readSymbolList(dis, new SymbolReader<StringSymbol>(){

            public StringSymbol read(DataInputStream dis) throws IOException
            {
                return syms.createString(dis.readUTF());
            }}));
        result.addAll(readSymbolList(dis, new SymbolReader<Variable>(){

            public Variable read(DataInputStream dis) throws IOException
            {
                return syms.make_variable(dis.readUTF());
            }}));
        result.addAll(readSymbolList(dis, new SymbolReader<IntegerSymbol>(){

            public IntegerSymbol read(DataInputStream dis) throws IOException
            {
                return syms.createInteger(dis.readInt());
            }}));
        result.addAll(readSymbolList(dis, new SymbolReader<DoubleSymbol>(){

            public DoubleSymbol read(DataInputStream dis) throws IOException
            {
                return syms.createDouble(dis.readDouble());
            }}));
        
        return result;
    }
    
    private <T extends Symbol> List<T> readSymbolList(DataInputStream dis, SymbolReader<T> reader) throws IOException, SoarException
    {
        final int size = dis.readInt();
        if(size < 0)
        {
            throw new SoarException(String.format("Invalid symbol list size %d", size));
        }
        final List<T> result = new ArrayList<T>(size);
        for(int i = 0; i < size; ++i)
        {
            result.add(reader.read(dis));
        }
        return result;
    }
    
    private static SymbolImpl getSymbol(List<Symbol> symbolMap, int index) throws SoarException
    {
        if(index < 0 || index >= symbolMap.size())
        {
            throw new SoarException(String.format("Invalid symbol index %d", index));
        }
        return (SymbolImpl) symbolMap.get(index);
    }
    
    private List<AlphaMemory> readAlphaMemories(DataInputStream dis, List<Symbol> symbolMap) throws IOException, SoarException
    {
        final int count = dis.readInt();
        if(count < 0)
        {
            throw new SoarException(String.format("Invalid alpha memory list size %d", count));
        }
        
        final List<AlphaMemory> ams = new ArrayList<AlphaMemory>(count);
        ams.add(null); // am index values start at 1. See writeAlphaMemories
        for(int i = 0; i < count; ++i)
        {
            final SymbolImpl id = getSymbol(symbolMap, dis.readInt());
            final SymbolImpl attr = getSymbol(symbolMap, dis.readInt());
            final SymbolImpl value = getSymbol(symbolMap, dis.readInt());
            final boolean acceptable = dis.readBoolean();
            ams.add(rete.find_or_make_alpha_mem(id, attr, value, acceptable));
        }
        return ams;
    }
    
    private static Object readVarNames(DataInputStream dis, List<Symbol> symbolMap) throws SoarException, IOException
    {
        final byte type = dis.readByte();
        if(type == 0)
        {
            return null;
        }
        else if(type == 1)
        {
            final int index = dis.readInt(); 
            return VarNames.one_var_to_varnames(getSymbol(symbolMap, index).asVariable());
        }
        else if(type == 2)
        {
            final int count = dis.readInt();
            if(count < 0)
            {
                throw new SoarException(String.format("Count of varnames list record must be positive, got %d", count));
            }
            final LinkedList<Variable> vars = new LinkedList<Variable>();
            for(int i = 0; i < count; ++i)
            {
                vars.add(getSymbol(symbolMap, i).asVariable());
            }
            return VarNames.var_list_to_varnames(vars);
        }
        else
        {
            throw new SoarException(String.format("Invalid varnames record type. Expected 0, 1, or 2, got %d", type));
        }
    }
    
    private static NodeVarNames readNodeVarNames(DataInputStream dis, ReteNode node, List<Symbol> symbolMap) throws SoarException, IOException 
    {

        if (node.node_type == ReteNodeType.DUMMY_TOP_BNODE)
        {
            return null;
        }
        if (node.node_type == ReteNodeType.CN_BNODE) 
        {
            ReteNode temp = node.b_cn.partner.parent;
            NodeVarNames nvn_for_ncc = readNodeVarNames(dis, temp, symbolMap);
            final NodeVarNames bottom_of_subconditions = nvn_for_ncc;
            while (temp != node.parent)
            {
                temp = temp.real_parent_node();
                nvn_for_ncc = nvn_for_ncc.parent;
            }
            return NodeVarNames.createForNcc(nvn_for_ncc,
                    bottom_of_subconditions);
        } 
        return NodeVarNames.newInstance(
                  readNodeVarNames(dis, node.real_parent_node(), symbolMap),
                  readVarNames(dis, symbolMap),
                  readVarNames(dis, symbolMap),
                  readVarNames(dis, symbolMap));
    }
}

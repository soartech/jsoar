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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.SoarProperties;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.FunctionAction;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.ReteLocation;
import org.jsoar.kernel.rhs.RhsFunctionCall;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.kernel.rhs.RhsValue;
import org.jsoar.kernel.rhs.UnboundVariable;
import org.jsoar.kernel.symbols.DoubleSymbol;
import org.jsoar.kernel.symbols.IntegerSymbol;
import org.jsoar.kernel.symbols.StringSymbol;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactoryImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.symbols.Variable;
import org.jsoar.util.Arguments;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.properties.PropertyKey;

/**
 * @author ray
 */
public class ReteNetWriter
{
    private final Agent context;
    private final SymbolFactoryImpl syms;
    private final Rete rete;
    
    private Map<Symbol, Integer> symbolIndex;
    private Map<AlphaMemory, Integer> amIndex;

    protected ReteNetWriter(Agent context)
    {
        Arguments.checkNotNull(context, "context");

        this.context = context;
        this.syms = Adaptables.require(getClass(), this.context, SymbolFactoryImpl.class);
        this.rete = Adaptables.require(getClass(), context, Rete.class);
    }

    // Properties to write out.
    @SuppressWarnings("serial")
    protected final HashSet<PropertyKey<?>> propertiesToInclude = new HashSet<PropertyKey<?>>() {{
        add(SoarProperties.WAITSNC);
        add(SoarProperties.LEARNING_ON);
        add(SoarProperties.EXPLAIN);
        add(SoarProperties.MAX_ELABORATIONS);
    }}; 

    /**
     * Write a rete net to the given output stream. All rules and
     * symbols are written.
     * 
     * <p>The stream is not closed on completion
     * 
     * @param os the output stream to write to
     * @throws IOException if an error occurs while writing
     * @throws SoarException 
     */
    protected void write(OutputStream os) throws IOException, SoarException
    {
        DataOutputStream dos = new DataOutputStream(os);
        GZIPOutputStream gos = null;
        try {
            // This is only to try to get rid of any symbols that may be hanging around
            // they won't hurt the rete if they stay, just make the image bigger.
            System.gc(); 
            
            // Write out version information uncompressed.
            dos.writeUTF(ReteNetReader.MAGIC_STRING);
            dos.writeInt(ReteNetReader.FORMAT_VERSION);
            dos.writeInt(ReteNetReader.COMPRESSION_TYPE);
            
            // Write out symbols, alpha memories, beta network, and some important properties.
            // This portion of the rete file is compressed.
            gos = new GZIPOutputStream(os);
            dos = new DataOutputStream(gos);
            writeAllSymbols(dos);
            writeAlphaMemories(dos, rete.getAllAlphaMemories());
            writeChildrenOfNode(dos, rete.dummy_top_node);
            writeProperties(dos, propertiesToInclude);
        }
        finally {
            dos.flush();
            if (gos != null)
            { 
                gos.finish();
            }
        }
    }

    private void writeProperties(DataOutputStream dos, HashSet<PropertyKey<?>> properties) throws IOException, SoarException
    {
       dos.writeInt(properties.size());
       for(PropertyKey<?> prop : properties) 
       {
           dos.writeUTF(prop.getName());
           if (prop.getType().equals(Boolean.class))
           {
               dos.writeBoolean((Boolean) context.getProperties().get(prop));
           }
           else if (prop.getType().equals(Integer.class))
           {
               dos.writeInt((Integer) context.getProperties().get(prop));
           }
           else
           {
               throw new SoarException("Unhandled property type: " + prop.getType());
           }
       }
    }

    private void writeChildrenOfNode(DataOutputStream dos, ReteNode node) throws IOException, SoarException
    {
        ReteNode child;
        int i = 0;

        // --- Count number of non-CN-node children. --- 
        for (child = node.first_child; child != null; child = child.next_sibling)
        {
            if (child.node_type != ReteNodeType.CN_BNODE)
            {
                i++;
            }
        }
        dos.writeInt(i);

        // --- Count number of non-CN-node children. --- 
        for (child = node.first_child; child != null; child = child.next_sibling)
        {
            if (child.node_type != ReteNodeType.CN_BNODE)
            {
                writeNodeAndChildren(dos, child);
            }
        }

    }

    private void writeNodeAndChildren(DataOutputStream dos, ReteNode node) throws IOException, SoarException
    {
        int i;
        Production prod;
        ReteNode temp;

        if (node.node_type == ReteNodeType.CN_BNODE)
            return; // ignore CN nodes 

        dos.writeUTF(node.node_type.toString());

        switch (node.node_type) {
        case MEMORY_BNODE:
            writeLeftHashLoc(dos, node);
            // ... and fall through to the next case below ... 
        case UNHASHED_MEMORY_BNODE:
            break;

        case MP_BNODE:
            writeLeftHashLoc(dos, node);
            // ... and fall through to the next case below ... 
        case UNHASHED_MP_BNODE:
            dos.writeInt(getAlphaMemoryIndex(node.b_posneg().alpha_mem_));
            writeTestList(dos, node.b_posneg().other_tests);
            dos.writeBoolean(node.a_np().is_left_unlinked);
            break;

        case POSITIVE_BNODE:
        case UNHASHED_POSITIVE_BNODE:
            dos.writeInt(getAlphaMemoryIndex(node.b_posneg().alpha_mem_));
            writeTestList(dos, node.b_posneg().other_tests);
            dos.writeBoolean(node.node_is_left_unlinked());
            break;

        case NEGATIVE_BNODE:
            writeLeftHashLoc(dos, node);
            // ... and fall through to the next case below ... 
        case UNHASHED_NEGATIVE_BNODE:
            dos.writeInt(getAlphaMemoryIndex(node.b_posneg().alpha_mem_));
            writeTestList(dos, node.b_posneg().other_tests);
            break;

        case CN_PARTNER_BNODE:
            i=0;
            temp = node.real_parent_node();
            while (temp != node.b_cn().partner.parent) {
                temp = temp.real_parent_node();
                i++;
            }
            dos.writeInt(i);
            break;

        case P_BNODE:
            prod = node.b_p().prod;
            // Production names in JSoar are strings, not symbols.
            dos.writeUTF(prod.getName());
            dos.writeUTF(prod.getDocumentation());
            dos.writeUTF(prod.getType().toString());
            dos.writeUTF(prod.getDeclaredSupport().toString());
            writeRHSActionList(dos, prod.getFirstAction());
            dos.writeInt(prod.getRhsUnboundVariables().size());
            for (Variable unboundVar : prod.getRhsUnboundVariables())
            {
                dos.writeInt(getSymbolIndex(unboundVar));
            }
            if (node.b_p().parents_nvn != null) {
                dos.writeBoolean(true);
                writeNodeVarNames(dos, symbolIndex, node.b_p().parents_nvn, node.parent);
            } else {
                dos.writeBoolean(false);
            }
            break;
        default:
            throw new SoarException("Unhandled ReteNodeType: " + node.node_type);
        }

        // --- For cn_p nodes, write out the CN node's children instead --- 
        if (node.node_type == ReteNodeType.CN_PARTNER_BNODE)
            node = node.b_cn().partner;
        // --- Write out records for all the node's children. --- 
        writeChildrenOfNode(dos, node);
    }

    private void writeLeftHashLoc(DataOutputStream dos, ReteNode node) throws IOException
    {
        dos.writeInt(node.left_hash_loc_field_num);
        dos.writeInt(node.left_hash_loc_levels_up);
    }


    private void writeRHSActionList(DataOutputStream dos, Action firstAction) throws IOException, SoarException
    {
        int i = 0;
        Action a;

        for (a = firstAction; a != null; a = a.next)
        {
            i++;
        }
        dos.writeInt(i);
        
        for (a = firstAction; a != null; a = a.next)
        {
            writeRHSAction(dos, a);
        }
    }

    private void writeRHSAction(DataOutputStream dos, Action a) throws IOException, SoarException
    {
        // JSoar's Action doesn't have a type field. These constants match MAKE_ACTION and FUNCALL_ACTION.
        if (a instanceof MakeAction)
        {
            dos.writeInt(0);
        }
        else
        {
            dos.writeInt(1);
        }
        if (a.preference_type != null)
        {
            dos.writeBoolean(true);
            dos.writeUTF(a.preference_type.toString());
        }
        else
        {
            dos.writeBoolean(false);
        }
        dos.writeUTF(a.support.toString());
        
        if (a instanceof FunctionAction) 
        {
            retesave_rhs_value(dos, a.asFunctionAction().call);
        }
        else if (a instanceof MakeAction)
        { 
          retesave_rhs_value(dos, a.asMakeAction().id);
          retesave_rhs_value(dos, a.asMakeAction().attr);
          retesave_rhs_value(dos, a.asMakeAction().value);
          if (a.preference_type != null && a.preference_type.isBinary())
          {
            retesave_rhs_value(dos, a.asMakeAction().referent);
          }
        }
        else
        {
            throw new SoarException("Unhandled action type.");
        }
    }
    
    private void retesave_rhs_value(DataOutputStream dos, RhsValue rv) throws IOException, SoarException
    {
        Symbol sym;
        
        if (rv instanceof RhsSymbolValue)
        {
          dos.writeInt(0);
          sym = rv.asSymbolValue().getSym();
          dos.writeInt(getSymbolIndex(sym));
        }
        else if (rv instanceof RhsFunctionCall) {
          dos.writeInt(1);
          dos.writeInt(getSymbolIndex(rv.asFunctionCall().getName()));
          dos.writeBoolean(rv.asFunctionCall().isStandalone());
          List<RhsValue> arguments = rv.asFunctionCall().getArguments();
          dos.writeInt(arguments.size());
          for (RhsValue value : arguments)
          {
              retesave_rhs_value(dos, value);
          }
        }
        else if (rv instanceof ReteLocation)
        {
          dos.writeInt(2);
          dos.writeInt(rv.asReteLocation().getFieldNum());
          dos.writeInt(rv.asReteLocation().getLevelsUp());
        }
        else if (rv instanceof UnboundVariable)
        {
          dos.writeInt(3);
          dos.writeInt(rv.asUnboundVariable().getIndex());
        }
        else
        {
            throw new SoarException("Unhandled RHS value");
        }
      }



    private void writeTestList(DataOutputStream dos, ReteTest firstRete) throws IOException, SoarException
    {
        int i = 0;
        ReteTest rt;

        for (rt = firstRete; rt != null; rt = rt.next)
        {
            i++;
        }
        dos.writeInt(i);
        
        for (rt = firstRete; rt != null; rt = rt.next)
        {
            writeTest(dos, rt);
        }
    }

    private void writeTest(DataOutputStream dos, ReteTest rt) throws IOException, SoarException
    {
        dos.writeInt(rt.type);
        dos.writeInt(rt.right_field_num);
        if (rt.test_is_constant_relational_test())
        {
            dos.writeInt(getSymbolIndex(rt.constant_referent));
        }
        else if (rt.test_is_variable_relational_test())
        {
            dos.writeInt(rt.variable_referent.field_num);
            dos.writeInt(rt.variable_referent.levels_up);
        }
        else if (rt.type == ReteTest.DISJUNCTION) 
        {
            List<SymbolImpl> disjunctions = rt.disjunction_list;
            dos.writeInt(disjunctions.size());
            for (SymbolImpl disjunction : disjunctions)
            {
                dos.writeInt(getSymbolIndex(disjunction));
            }
        } 
        else if (rt.type == ReteTest.ID_IS_GOAL)
        {
            // Nothing to write.
        }
        else if (rt.type == ReteTest.ID_IS_IMPASSE)
        {
            // Nothing to write.
        }
        else
        {
            throw new SoarException("Unhandled ReteTest: " + rt);
        }
    }

    private static interface SymbolWriter<T>
    {
        void write(DataOutputStream dos, T s) throws IOException;
    }

    private void writeAllSymbols(DataOutputStream dos) throws IOException
    {
        symbolIndex = new HashMap<Symbol, Integer>();

        // symbol index numbers start at one so we can use zero for "no symbol" in alpha memories
        int nextIndex = writeSymbolList(dos, 1,
                syms.getSymbols(StringSymbol.class),
                new SymbolWriter<StringSymbol>() {
            public void write(DataOutputStream dos, StringSymbol s) throws IOException { dos.writeUTF(s.getValue()); }
        });
        nextIndex = writeSymbolList(dos, nextIndex,
                syms.getSymbols(Variable.class),
                new SymbolWriter<Variable>() {
            public void write(DataOutputStream dos, Variable s) throws IOException { dos.writeUTF(s.name); }
        });
        nextIndex = writeSymbolList(dos, nextIndex,
                syms.getSymbols(IntegerSymbol.class),
                new SymbolWriter<IntegerSymbol>() {
            public void write(DataOutputStream dos, IntegerSymbol s) throws IOException { dos.writeLong(s.getValue()); }
        });
        nextIndex = writeSymbolList(dos, nextIndex,
                syms.getSymbols(DoubleSymbol.class),
                new SymbolWriter<DoubleSymbol>() {
            public void write(DataOutputStream dos, DoubleSymbol s) throws IOException { dos.writeDouble(s.getValue()); }
        });
    }

    private <T extends Symbol> int writeSymbolList(DataOutputStream dos, int nextIndex, List<T> symbols, SymbolWriter<T> writer) throws IOException
            {
        dos.writeInt(symbols.size());
        for(T s : symbols)
        {
            writer.write(dos, s);
            nextIndex = indexSymbol(s, symbolIndex, nextIndex);
        }

        return nextIndex;
            }

    private int indexSymbol(Symbol s, Map<Symbol, Integer> symbolIndex, int nextIndex)
    {
        symbolIndex.put(s, nextIndex);
        return ++nextIndex;
    }    

    private int getSymbolIndex(Symbol s)
    {
        if(s == null)
        {
            return 0;
        }
        final Integer i = symbolIndex.get(s);

        return i != null ? i : 0;
    }

    private void writeAlphaMemories(DataOutputStream dos, List<AlphaMemory> ams) throws IOException
    {
        amIndex = new HashMap<AlphaMemory, Integer>();

        dos.writeInt(ams.size());
        int nextIndex = 1; 
        for(AlphaMemory am : ams)
        {
            writeAlphaMemory(dos, am);
            amIndex.put(am, nextIndex++);
        }
    }
    
    private int getAlphaMemoryIndex(AlphaMemory am) throws SoarException
    {
        if (am == null)
        {
            throw new SoarException("Alpha memory is null.");
        }
        
        final Integer i = amIndex.get(am);
        
        if (i == null)
        {
            throw new SoarException("Unknown alpha memory.");
        }
        
        return i;
    }

    private void writeAlphaMemory(DataOutputStream dos, AlphaMemory am) throws IOException
            {
        dos.writeInt(getSymbolIndex(am.id));
        dos.writeInt(getSymbolIndex(am.attr));
        dos.writeInt(getSymbolIndex(am.value));
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
    private void writeVarNames(DataOutputStream dos, Object varNames) throws IOException
    {
        if(varNames == null)
        {
            dos.writeByte((byte) 0);
        }
        else if(VarNames.varnames_is_one_var(varNames))
        {
            dos.writeByte((byte) 1);
            dos.writeInt(getSymbolIndex(VarNames.varnames_to_one_var(varNames)));
        }
        else
        {
            dos.writeByte((byte) 2);
            final List<Variable> vars = VarNames.varnames_to_var_list(varNames);
            dos.writeInt(vars.size());
            for(Variable v : vars)
            {
                dos.writeInt(getSymbolIndex(v));
            }
        }
    }

    private void writeNodeVarNames(DataOutputStream dos, Map<Symbol, Integer> symbolIndex, NodeVarNames nvn, ReteNode node) throws IOException
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
            
            writeVarNames(dos, nvn.fields.id_varnames);
            writeVarNames(dos, nvn.fields.attr_varnames);
            writeVarNames(dos, nvn.fields.value_varnames);
            nvn = nvn.parent;
            node = node.real_parent_node();
        }
    }
}

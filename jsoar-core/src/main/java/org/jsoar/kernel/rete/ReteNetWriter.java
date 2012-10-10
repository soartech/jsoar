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
import org.jsoar.kernel.Production;
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
            writeChildrenOfNode(dos, rete.dummy_top_node, amIndex, symbolIndex);
        }
        finally {
        }

    }

    private void writeChildrenOfNode(DataOutputStream dos,
            ReteNode node, Map<AlphaMemory, Integer> amIndex, Map<Symbol, Integer> symbolIndex) throws IOException
    {
        ReteNode child;
        int i = 0;

        /* --- Count number of non-CN-node children. --- */
        for (child = node.first_child; child != null; child = child.next_sibling)
        {
            if (child.node_type != ReteNodeType.CN_BNODE)
            {
                i++;
            }
        }
        dos.writeInt(i);

        /* --- Count number of non-CN-node children. --- */
        for (child = node.first_child; child != null; child = child.next_sibling)
        {
            if (child.node_type != ReteNodeType.CN_BNODE)
            {
                writeNodeAndChildren(dos, child, amIndex, symbolIndex);
            }
        }

    }

    private void writeNodeAndChildren(DataOutputStream dos, ReteNode node,
            Map<AlphaMemory, Integer> amIndex, Map<Symbol, Integer> symbolIndex) throws IOException
    {
        int i;
        Production prod;
        ReteNode temp;

        if (node.node_type == ReteNodeType.CN_BNODE)
            return; /* ignore CN nodes */

        dos.writeUTF(node.node_type.toString());

        switch (node.node_type) {
        case MEMORY_BNODE:
            dos.writeInt(node.left_hash_loc_field_num);
            dos.writeInt(node.left_hash_loc_levels_up);
            /* ... and fall through to the next case below ... */
        case UNHASHED_MEMORY_BNODE:
            break;

        case MP_BNODE:
            dos.writeInt(node.left_hash_loc_field_num);
            dos.writeInt(node.left_hash_loc_levels_up);
            /* ... and fall through to the next case below ... */
        case UNHASHED_MP_BNODE:
            dos.writeInt(amIndex.get(node.b_posneg().alpha_mem_));
            writeTestList(dos, node.b_posneg().other_tests, amIndex, symbolIndex);
            dos.writeBoolean(node.a_np().is_left_unlinked);
            break;

        case POSITIVE_BNODE:
        case UNHASHED_POSITIVE_BNODE:
            dos.writeInt(amIndex.get(node.b_posneg().alpha_mem_));
            writeTestList(dos, node.b_posneg().other_tests, amIndex, symbolIndex);
            dos.writeBoolean(node.node_is_left_unlinked());
            break;

        case NEGATIVE_BNODE:
            dos.writeInt(node.left_hash_loc_field_num);
            dos.writeInt(node.left_hash_loc_levels_up);
            /* ... and fall through to the next case below ... */
        case UNHASHED_NEGATIVE_BNODE:
//            dos.writeInt(node.b_posneg().alpha_mem_.retesave_amindex);
            dos.writeInt(amIndex.get(node.b_posneg().alpha_mem_));
            writeTestList(dos, node.b_posneg().other_tests, amIndex, symbolIndex);
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
            // TODO: How do we get a StringSymbol for our production name?
//            retesave_eight_bytes (prod.name.common.a.retesave_symindex,f); 
            dos.writeUTF(prod.getName());
            dos.writeUTF(prod.getDocumentation());
            dos.writeUTF(prod.getType().toString());
            dos.writeUTF(prod.getDeclaredSupport().toString());
            writeRHSActionList(dos, prod.getFirstAction(), amIndex, symbolIndex);
            dos.writeInt(prod.getRhsUnboundVariables().size());
            for (Variable c : prod.getRhsUnboundVariables())
            {
                dos.writeInt(getSymbolIndex(symbolIndex, c));
//                retesave_eight_bytes (static_cast<Symbol *>(c.first).common.a.retesave_symindex,f);
            }
            if (node.b_p().parents_nvn != null) {
                dos.writeBoolean(true);
                // TODO: Is this the right method? (It was pre-existing!)
                writeNodeVarNames(dos, symbolIndex, node.b_p().parents_nvn, node);
//                retesave_node_varnames (node.b.p.parents_nvn, node.parent, f);
            } else {
                dos.writeBoolean(false);
            }
            break;
        }

        /* --- For cn_p nodes, write out the CN node's children instead --- */
        if (node.node_type == ReteNodeType.CN_PARTNER_BNODE)
            node = node.b_cn().partner;
        /* --- Write out records for all the node's children. --- */
        writeChildrenOfNode(dos, node, amIndex, symbolIndex);
    }

    private void writeRHSActionList(DataOutputStream dos, Action firstAction, Map<AlphaMemory, Integer> amIndex, Map<Symbol, Integer> symbolIndex) throws IOException
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
            writeRHSAction(dos, a, amIndex, symbolIndex);
        }
    }

    private void writeRHSAction(DataOutputStream dos, Action a, Map<AlphaMemory, Integer> amIndex, Map<Symbol, Integer> symbolIndex) throws IOException
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
        dos.writeUTF(a.preference_type.toString());
        dos.writeUTF(a.support.toString());
        
        if (a instanceof FunctionAction) 
        {
            retesave_rhs_value(dos, a.asFunctionAction().call, amIndex, symbolIndex);
        }
        else if (a instanceof MakeAction)
        { 
          retesave_rhs_value(dos, a.asMakeAction().id, amIndex, symbolIndex);
          retesave_rhs_value(dos, a.asMakeAction().attr, amIndex, symbolIndex);
          retesave_rhs_value(dos, a.asMakeAction().value, amIndex, symbolIndex);
          if (a.preference_type.isBinary())
          {
            retesave_rhs_value(dos, a.asMakeAction().referent, amIndex, symbolIndex);
          }
        }
    }
    
    private void retesave_rhs_value(DataOutputStream dos, RhsValue rv, Map<AlphaMemory, Integer> amIndex, Map<Symbol, Integer> symbolIndex) throws IOException
    {
        Symbol sym;
        
        if (rv instanceof RhsSymbolValue)
        {
          dos.writeInt(0);
          sym = rv.asSymbolValue().getSym();
          dos.writeInt(amIndex.get(sym));
        }
        else if (rv instanceof RhsFunctionCall) {
          dos.writeInt(1);
          // TODO: amIndex or symbol table?
          dos.writeInt(symbolIndex.get(rv.asFunctionCall().getName()));
          dos.writeBoolean(rv.asFunctionCall().isStandalone());
          List<RhsValue> arguments = rv.asFunctionCall().getArguments();
          dos.writeInt(arguments.size());
          for (RhsValue value : arguments)
          {
              retesave_rhs_value(dos, value, amIndex, symbolIndex);
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
          // TODO: Is this right?
          dos.writeInt(rv.asUnboundVariable().getIndex());
//          retesave_eight_bytes (rhs_value_to_unboundvar(rv),f);
        }
      }



    private void writeTestList(DataOutputStream dos, ReteTest firstRete, Map<AlphaMemory, Integer> amIndex, Map<Symbol, Integer> symbolIndex) throws IOException
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
            writeTest(dos, rt, amIndex, symbolIndex);
        }
    }

    private void writeTest(DataOutputStream dos, ReteTest rt, Map<AlphaMemory, Integer> amIndex, Map<Symbol, Integer> symbolIndex) throws IOException
    {
        dos.writeInt(rt.type);
        dos.writeInt(rt.right_field_num);
        if (rt.test_is_constant_relational_test())
        {
            dos.writeInt(symbolIndex.get(rt.constant_referent));
//          retesave_eight_bytes(rt->data.constant_referent->common.a.retesave_symindex, f);
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
                dos.writeInt(symbolIndex.get(disjunction));
            }
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
            public void write(DataOutputStream dos, IntegerSymbol s) throws IOException { dos.writeLong(s.getValue()); }
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

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
 * @author charles.newton
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
     * <p>The stream is not closed on completion.
     * 
     * <p>rete.cpp:7548:save_rete_net
     * 
     * @param os the output stream to write to
     * @throws IOException if an error occurs while writing
     * @throws SoarException 
     * @see ReteNetReader#read(java.io.InputStream)
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
            // This portion of the rete file is compressed (GZIP).
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

    /**
     * Writes a set of {@link PropertyKey}s and their current values to the output stream.
     * @see ReteNetReader#readProperties
     * @see org.jsoar.util.properties.PropertyKey
     * @see org.jsoar.kernel.Agent#getProperties()
     */
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

    /**
     * Writes out the children of a particular {@link ReteNode}.
     * <p>rete.cpp:7270:retesave_children_of_node
     * @see ReteNetReader#readChildrenOfNode
     */
    private void writeChildrenOfNode(DataOutputStream dos, ReteNode node) throws IOException, SoarException
    {
        ReteNode child;
        int numChildren = 0;

        // --- Count number of non-CN-node children. --- 
        for (child = node.first_child; child != null; child = child.next_sibling)
        {
            if (child.node_type != ReteNodeType.CN_BNODE)
            {
                numChildren++;
            }
        }
        dos.writeInt(numChildren);

        // --- Count number of non-CN-node children. --- 
        for (child = node.first_child; child != null; child = child.next_sibling)
        {
            if (child.node_type != ReteNodeType.CN_BNODE)
            {
                writeNodeAndChildren(dos, child);
            }
        }

    }

    /**
     * Writes out a rete node (and its descendants.)
     * <p>rete.cpp:7284:retesave_rete_node_and_children
     * @see ReteNetReader#readNodeAndChildren
     */
    private void writeNodeAndChildren(DataOutputStream dos, ReteNode node) throws IOException, SoarException
    {
        Production prod;
        ReteNode temp;

        if (node.node_type == ReteNodeType.CN_BNODE)
            return; // ignore CN nodes 

        dos.writeUTF(node.node_type.toString());

        switch (node.node_type)
        {
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
            int i = 0;
            temp = node.real_parent_node();
            while (temp != node.b_cn().partner.parent)
            {
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
            writeActionList(dos, prod.getFirstAction());
            dos.writeInt(prod.getRhsUnboundVariables().size());
            for (Variable unboundVar : prod.getRhsUnboundVariables())
            {
                dos.writeInt(getSymbolIndex(unboundVar));
            }
            if (node.b_p().parents_nvn != null)
            {
                dos.writeBoolean(true);
                writeNodeVarNames(dos, node.b_p().parents_nvn, node.parent);
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

    
    /**
     * Convenience function for writing out the field_num and levels_up
     * for a {@link ReteNode}.
     * @see ReteNetReader#readLeftHashLoc
     */
    private void writeLeftHashLoc(DataOutputStream dos, ReteNode node) throws IOException
    {
        dos.writeInt(node.left_hash_loc_field_num);
        dos.writeInt(node.left_hash_loc_levels_up);
    }

    /**
     * Writes all the actions in a provided list of actions.
     * 
     * Format: Writes an integer containing the number of actions to be
     * written, then writes each action.
     *  
     * <p>rete.cpp:7107:retesave_action_list
     * 
     * @param dos
     * @param firstAction the head of the action linked list.
     * @see ReteNetWriter#writeAction(DataOutputStream, Action)
     * @see ReteNetReader#readActionList
     */
    private void writeActionList(DataOutputStream dos, Action firstAction) throws IOException, SoarException
    {
        int numActions = 0;
        Action a;

        for (a = firstAction; a != null; a = a.next)
        {
            numActions++;
        }
        dos.writeInt(numActions);

        for (a = firstAction; a != null; a = a.next)
        {
            writeAction(dos, a);
        }
    }

    /**
     * Writes out an {@link Action}.
     * 
     * <p>An integer is written to represent the type of action, followed by a
     * string representation of its preference type {@link PreferenceType} (if any),
     * and the appropriate RHS values {@link RhsValue}.
     * 
     * <p>rete.cpp:7070:retesave_rhs_action
     * 
     * @param a the action to write.
     * @see ReteNetReader#readAction
     */
    private void writeAction(DataOutputStream dos, Action a) throws IOException, SoarException
    {
        // JSoar's Action doesn't have a type field. These constants match MAKE_ACTION and FUNCALL_ACTION.
        if (a instanceof MakeAction)
        {
            dos.writeInt(ReteNetReader.MAKE_ACTION);
        }
        else if (a instanceof FunctionAction) 
        {
            dos.writeInt(ReteNetReader.FUNCALL_ACTION);
        }
        else
        {
            throw new SoarException("Unhandled action type.");
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
            writeRHSValue(dos, a.asFunctionAction().call);
        }
        else if (a instanceof MakeAction)
        { 
            writeRHSValue(dos, a.asMakeAction().id);
            writeRHSValue(dos, a.asMakeAction().attr);
            writeRHSValue(dos, a.asMakeAction().value);
            if (a.preference_type != null && a.preference_type.isBinary())
            {
                writeRHSValue(dos, a.asMakeAction().referent);
            }
        }
    }

    /**
     * Writes a RHSValue {@link RhsValue}.
     * 
     * <p>Record:
     * 1 int: type (0={@link RhsSymbolValue}, 1={@link RhsFunctionCall}, 2={@link ReteLocation},
     * 3={@link UnboundVariable})
     * 
     * <p>for symbols, an int of the symbol index.
     * 
     * <p>for function calls, an int of the function name's symbol index, if it's a standalone function,
     * the number of arguments, and a rhs value record for each argument.
     * 
     * <p>for rete locations, one int for field number and one int for levels up.
     * 
     * <p>for unbound variables, one int for their index.
     * 
     * <p>rete.cpp:6954:retesave_rhs_value
     * 
     * @see ReteNetReader#readRHSValue
     */
    private void writeRHSValue(DataOutputStream dos, RhsValue rv) throws IOException, SoarException
    {
        Symbol sym;
        
        if (rv instanceof RhsSymbolValue)
        {
            dos.writeInt(ReteNetReader.RHS_SYMBOL);
            sym = rv.asSymbolValue().getSym();
            dos.writeInt(getSymbolIndex(sym));
        }
        else if (rv instanceof RhsFunctionCall)
        {
            dos.writeInt(ReteNetReader.RHS_FUNCALL);
            dos.writeInt(getSymbolIndex(rv.asFunctionCall().getName()));
            dos.writeBoolean(rv.asFunctionCall().isStandalone());
            List<RhsValue> arguments = rv.asFunctionCall().getArguments();
            dos.writeInt(arguments.size());
            for (RhsValue value : arguments)
            {
                writeRHSValue(dos, value);
            }
        }
        else if (rv instanceof ReteLocation)
        {
            dos.writeInt(ReteNetReader.RHS_RETELOC);
            dos.writeInt(rv.asReteLocation().getFieldNum());
            dos.writeInt(rv.asReteLocation().getLevelsUp());
        }
        else if (rv instanceof UnboundVariable)
        {
            dos.writeInt(ReteNetReader.RHS_UNBOUND_VAR);
            dos.writeInt(rv.asUnboundVariable().getIndex());
        }
        else
        {
            throw new SoarException("Unhandled RHS value");
        }
    }

    /**
     * Saves a linked list of rete tests ({@link ReteTest}).
     * 
     * The number (int) of nodes in the linked list is saved, followed
     * by each {@link ReteTest}.
     * 
     * <p>rete.cpp:7195:retesave_rete_test_list
     * 
     * @param firstRete head of the linked list.
     * @see #writeTest(DataOutputStream, ReteTest)
     * @see ReteNetReader#readTestList
     */
    private void writeTestList(DataOutputStream dos, ReteTest firstRete) throws IOException, SoarException
    {
        int numTests = 0;
        ReteTest rt;

        for (rt = firstRete; rt != null; rt = rt.next)
        {
            numTests++;
        }
        dos.writeInt(numTests);

        for (rt = firstRete; rt != null; rt = rt.next)
        {
            writeTest(dos, rt);
        }
    }

    /**
     * Writes out an individual {@link ReteTest}.
     * 
     * Record syntax: one int for {@link ReteTest#type}, and one int for {@link ReteTest#right_field_num}.
     * 
     * <p>For relational tests to variables: two ints for field number and levels up.
     * <p>For relational tests to constants: one int for the symbol's index.
     * <p>For disjunctions: one int for number of disjunts, and then list of ints, one for
     * the symindex of each disjunct.
     * 
     * <p>rete.cpp:7148:retesave_rete_test
     * 
     * @param rt the ReteTest to save.
     * @see ReteNetReader#readTest
     */
    private void writeTest(DataOutputStream dos, ReteTest rt) throws IOException, SoarException
    {
        dos.writeInt(rt.type);
        dos.writeInt(rt.right_field_num);
        // Relational tests to constants.
        if (rt.test_is_constant_relational_test())
        {
            dos.writeInt(getSymbolIndex(rt.constant_referent));
        }
        // Relational tests to variables.
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
        // These both aren't included in CSoar's retesave_rete_test
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

    /**
     * Writes out all instances of {@link StringSymbol}, {@link Variable},
     * {@link IntegerSymbol}, and {@link DoubleSymbol}.
     * 
     * <p>rete.cpp:6672:retesave_symbol_table
     * @see ReteNetReader#readAllSymbols
     */
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

    /**
     * Writes out a list of symbols.
     * @see ReteNetReader#readSymbolList
     */
    private <T extends Symbol> int writeSymbolList(DataOutputStream dos, int nextIndex, List<T> symbols,
            SymbolWriter<T> writer) throws IOException
    {
        dos.writeInt(symbols.size());
        for(T s : symbols)
        {
            writer.write(dos, s);
            nextIndex = indexSymbol(s, nextIndex);
        }

        return nextIndex;
    }

    /**
     * Creates a unique index for a symbol.
     */
    private int indexSymbol(Symbol s, int nextIndex)
    {
        symbolIndex.put(s, nextIndex);
        return ++nextIndex;
    }    

    /**
     * Gets the symbol index for a symbol s. If s is null, returns 0.
     * If s has no existing index, returns 0.
     */
    private int getSymbolIndex(Symbol s)
    {
        if(s == null)
        {
            return 0;
        }
        final Integer i = symbolIndex.get(s);

        return i != null ? i : 0;
    }

    /**
     * Writes out a list of alpha memories.
     * 
     * <p>rete.cpp:6792:retesave_alpha_memories
     * @see ReteNetReader#readAlphaMemories
     */
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

    /**
     * Gets the index for a particular alpha memory.
     * 
     * @throws SoarException if the alpha memory is not found.
     */
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

    /**
     * Writes out a particular alpha memory.
     * 
     * <p>Record format: three ints for the symbol index of each of the id, attr, and values.
     * One boolean for the acceptable preference.
     * 
     * <p>rete.cpp:6858:retesave_node_varnames
     * @see ReteNetReader#readAlphaMemories
     */
    private void writeAlphaMemory(DataOutputStream dos, AlphaMemory am) throws IOException
    {
        dos.writeInt(getSymbolIndex(am.id));
        dos.writeInt(getSymbolIndex(am.attr));
        dos.writeInt(getSymbolIndex(am.value));
        dos.writeBoolean(am.acceptable);
    }

    /**
     * Writes varnames.
     * 
     * <p> Record format:
     *    type (1 byte): 0=null, 1=one var, 2=list
     *    if one var: 4 bytes (symindex)
     *    if list: 4 bytes (number of items) + list of symindices
     * 
     * <p>rete.cpp:6858:retesave_varnames
     * @see ReteNetReader#readVarNames
     */
    private void writeVarNames(DataOutputStream dos, Object varNames) throws IOException
    {
        if(varNames == null)
        {
            dos.writeInt(ReteNetReader.VARNAME_NULL);
        }
        else if(VarNames.varnames_is_one_var(varNames))
        {
            dos.writeInt(ReteNetReader.VARNAME_ONE_VAR);
            dos.writeInt(getSymbolIndex(VarNames.varnames_to_one_var(varNames)));
        }
        else
        {
            dos.writeInt(ReteNetReader.VARNAME_LIST);
            final List<Variable> vars = VarNames.varnames_to_var_list(varNames);
            dos.writeInt(vars.size());
            for(Variable v : vars)
            {
                dos.writeInt(getSymbolIndex(v));
            }
        }
    }

    /**
     * Writes a node varname (NVN) structure.
     * 
     * <p>rete.cpp:6902:retesave_node_varnames
     * @see ReteNetReader#readNodeVarNames
     */
    private void writeNodeVarNames(DataOutputStream dos, NodeVarNames nvn,
            ReteNode node) throws IOException
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

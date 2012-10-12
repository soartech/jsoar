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
import org.jsoar.kernel.Production;
import org.jsoar.kernel.Production.Support;
import org.jsoar.kernel.ProductionManager;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.events.ProductionAddedEvent;
import org.jsoar.kernel.learning.rl.ReinforcementLearning;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.ActionSupport;
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
import org.jsoar.util.properties.PropertyManager;

/**
 * @author ray
 */
public class ReteNetReader
{
    public static final String MAGIC_STRING = "JSoarCompactReteNet";
    public static final int FORMAT_VERSION = 2;
    
    private final Agent context;
    private final SymbolFactoryImpl syms;
    private final Rete rete;
    private final ProductionManager productionManager;
    private ReinforcementLearning rl;
    
    protected ReteNetReader(Agent context)
    {
        Arguments.checkNotNull(context, "context");
        this.context = context;
        this.syms = Adaptables.require(getClass(), context, SymbolFactoryImpl.class);
        this.rete = Adaptables.require(getClass(), context, Rete.class);
        this.rl = Adaptables.require(getClass(), context, ReinforcementLearning.class);
        this.productionManager = context.getProductions();
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
        readBetaMemories(dis, symbolMap, alphaMemories);
        readProperties(dis);
    }
    
    @SuppressWarnings("unchecked")
    private void readProperties(DataInputStream dis) throws IOException, SoarException
    {
        int numProperties = dis.readInt();
        PropertyManager properties = context.getProperties();
        for(int i = 0; i < numProperties; i++)
        {
            String name = dis.readUTF();
            PropertyKey<?> propertyKey = properties.getKey(name);
            if (propertyKey == null)
            {
                throw new SoarException("Unknown property " + name);
            }
            
            if (propertyKey.getType().equals(Boolean.class))
            {
                boolean value = dis.readBoolean();
                properties.set((PropertyKey<Boolean>)propertyKey, value);
            }
            else if (propertyKey.getType().equals(Integer.class))
            {
                int value = dis.readInt();
                properties.set((PropertyKey<Integer>)propertyKey, value);
            }
            else
            {
                throw new SoarException("Unknown property type \"" + propertyKey.getType() + "\" for property " + name);
            }
        }
    }

    private void readBetaMemories(DataInputStream dis,
            List<Symbol> symbolMap, List<AlphaMemory> alphaMemories) throws IOException, SoarException
    {
        // Number of children.
        int numNodes = dis.readInt();
        
        for (int i = 0; i < numNodes; i++)
        {
            readNodeAndChildren(dis, rete.dummy_top_node, symbolMap, alphaMemories);
        }
    }

    private void readNodeAndChildren(DataInputStream dis,
            ReteNode parent, List<Symbol> symbolMap, List<AlphaMemory> alphaMemories) throws IOException, SoarException
    {
        final ReteNodeType type = ReteNodeType.valueOf(dis.readUTF());
        ReteNode New = null;
        AlphaMemory am;
        boolean left_unlinked_flag;
        ReteTest other_tests;
        Production prod;
        
        /* 
         * Initializing the left_hash_loc structure to flag values.
         * It gets passed into some of the various make_new_??? functions
         * below but is never used (hopefully) for UNHASHED node types.
         */
        VarLocation left_hash_loc = new VarLocation(-1, -1);
        switch (type) {
        case MEMORY_BNODE:
            left_hash_loc = readLeftHashLoc(dis);
          /* ... and fall through to the next case below ... */
        case UNHASHED_MEMORY_BNODE:
            New = ReteNode.make_new_mem_node(rete, parent, type, left_hash_loc);
          break;

        case MP_BNODE:
            left_hash_loc = readLeftHashLoc(dis);
          /* ... and fall through to the next case below ... */
        case UNHASHED_MP_BNODE:
            am = alphaMemories.get(dis.readInt());
            am.reference_count++;
            other_tests = readTestList(dis, symbolMap, alphaMemories);
            left_unlinked_flag = dis.readBoolean();
            New = ReteNode.make_new_mp_node(rete, parent, type, left_hash_loc, am, other_tests,
                    left_unlinked_flag);
            break;

        case POSITIVE_BNODE:
        case UNHASHED_POSITIVE_BNODE:
          am = alphaMemories.get(dis.readInt());
          am.reference_count++;
          other_tests = readTestList(dis, symbolMap, alphaMemories);
          left_unlinked_flag = dis.readBoolean();
          New = ReteNode.make_new_positive_node(rete, parent, type, am, other_tests,
                  left_unlinked_flag);
          break;

        case NEGATIVE_BNODE:
            left_hash_loc = readLeftHashLoc(dis);
          /* ... and fall through to the next case below ... */
        case UNHASHED_NEGATIVE_BNODE:
          am = alphaMemories.get(dis.readInt());
          am.reference_count++;

          other_tests = readTestList(dis, symbolMap, alphaMemories);
          New = ReteNode.make_new_negative_node(rete, parent, type, left_hash_loc, am,other_tests);
          break;

        case CN_PARTNER_BNODE:
          int count = dis.readInt();
          ReteNode ncc_top = parent;
          while (count-- > 0) ncc_top = ncc_top.real_parent_node();
          New = ReteNode.make_new_cn_node(rete, ncc_top, parent);
          break;

        case P_BNODE:
//          allocate_with_pool (thisAgent, &thisAgent.production_pool, &prod);
//          prod.reference_count = 1;
//          prod.firing_count = 0;
//          prod.trace_firings = FALSE;
//          prod.instantiations = NIL;
//          prod.filename = NIL;
//          prod.p_node = NIL;
//          prod.interrupt = FALSE;
          
            String name = dis.readUTF();
            String doc = dis.readUTF();
            ProductionType prodType = ProductionType.valueOf(dis.readUTF());
            Support declaredSupport = Support.valueOf(dis.readUTF());
//          sym = reteload_symbol_from_index (thisAgent,f);
//          symbol_add_ref (sym);
//          prod.name = sym;
//          sym.sc.production = prod;
            
//          if (reteload_one_byte(f)) {
//            reteload_string(f);
//            prod.documentation = make_memory_block_for_string (thisAgent, reteload_string_buf);
//          } else {
//            prod.documentation = NIL;
//          }
//          prod.type = reteload_one_byte (f);
//          prod.declared_support = reteload_one_byte (f);
            
//          prod.action_list = reteload_action_list (thisAgent,f);
            Action actionList = readActionList(dis, symbolMap, alphaMemories);
            // TODO: There's a hack here: fake conditions to get us to build the production.
//            prod = Production.newBuilder().name(name).documentation(doc).type(prodType).support(declaredSupport).actions(actionList).conditions(new ConjunctiveNegationCondition(), new ConjunctiveNegationCondition()).build();
            prod = Production.newBuilder().name(name).documentation(doc).type(prodType).support(declaredSupport).actions(actionList).build();

            int numUnboundVariables = dis.readInt();
            rete.update_max_rhs_unbound_variables(numUnboundVariables);
            List<Variable> unboundVars = new ArrayList<Variable>(numUnboundVariables);
            for(int i = 0; i < numUnboundVariables; i++)
            {
                // TODO: Are these in the right order?
//                prod.getRhsUnboundVariables().add((Variable)getSymbol(symbolMap, dis.readInt()));
                unboundVars.add(getSymbol(symbolMap, dis.readInt()).asVariable());
            }
            prod.setRhsUnboundVariables(unboundVars);
            productionManager.addProductionToNameTypeMaps(prod);
//            insert_at_head_of_dll (thisAgent->all_productions_of_type[prod->type],
//                    prod, next, prev);
//            thisAgent->num_productions_of_type[prod->type]++;
//          ubv_list = NIL;
//          while (count--) {
//            sym = reteload_symbol_from_index(thisAgent,f);
//            symbol_add_ref (sym);
//            push(thisAgent, sym, ubv_list);
//          }
//          prod.rhs_unbound_variables = destructively_reverse_list (ubv_list);

            
// TODO: Make these methods public?
//          insert_at_head_of_dll (thisAgent.all_productions_of_type[prod.type],
//                                 prod, next, prev);
//          thisAgent.num_productions_of_type[prod.type]++;

          // Soar-RL stuff
            rl.addProduction(prod);
//          prod.rl_update_count = 0.0;
//          prod.rl_delta_bar_delta_beta = -3.0;
//          prod.rl_delta_bar_delta_h = 0.0;
//          prod.rl_update_count = 0;
//          prod.rl_rule = false;
//          prod.rl_ecr = 0.0;
//          prod.rl_efr = 0.0;
//          if ( ( prod.getType() != ProductionType.JUSTIFICATION ) && ( prod.getType() != ProductionType.TEMPLATE ) )
//          {
//            prod.rl_rule = rl_valid_rule( prod );
//            if ( prod.rl_rule )
//            {
//              prod.rl_efr = get_number_from_symbol( rhs_value_to_symbol( prod.action_list.referent ) );
//
//              if ( prod.documentation )
//              {
//                rl_rule_meta( thisAgent, prod );
//              }
//            }
//          }
            
            New = ReteNode.make_new_production_node(rete, parent, prod);
            // TODO: Where does this live in JSoar?
//          adjust_sharing_factors_from_here_to_top (New, 1);
          if (dis.readBoolean()) {
              // TODO: Is this the right method call?
            New.b_p().parents_nvn = readNodeVarNames(dis, parent, symbolMap);
          } else {
            New.b_p().parents_nvn = null;
          }

          /* --- call new node's add_left routine with all the parent's tokens --- */
          rete.update_node_with_matches_from_above(New);

           /* --- invoke callback on the production --- */
          // TODO: Callback here or use the production manager?
          context.getEvents().fireEvent(new ProductionAddedEvent(context, prod));
          break;
        }

        /* --- read in the children of the node --- */
        int count = dis.readInt();
        while (count-- > 0) readNodeAndChildren(dis, New, symbolMap, alphaMemories);
    }

    private VarLocation readLeftHashLoc(DataInputStream dis) throws IOException
    {
       int field_num = dis.readInt(); 
       int levels_up = dis.readInt();
        
       return new VarLocation(levels_up, field_num);
    }

    private Action readActionList(DataInputStream dis,
            List<Symbol> symbolMap, List<AlphaMemory> alphaMemories) throws IOException, SoarException
    {
        // reteload_action_list
        Action a;
        Action prev_a = null;
        Action first_a = null;
        int count; 
        
        count = dis.readInt();
        
        while (count-- > 0) {
          a = readRHSAction(dis, symbolMap, alphaMemories);
          if (prev_a != null)
          {
              prev_a.next = a;
          }
          else
          {
              first_a = a;
          }
          prev_a = a;
        }
        if (prev_a != null)
        {
            prev_a.next = null; 
        }
        else
        {
            first_a = null;
        }
        return first_a; 
    }

    private Action readRHSAction(DataInputStream dis, List<Symbol> symbolMap,
            List<AlphaMemory> alphaMemories) throws IOException, SoarException
            {
        // JSoar's Action doesn't have a type field. These constants {0, 1} match MAKE_ACTION and FUNCALL_ACTION.
        int type = dis.readInt();
        Action a = null;
        if (type == 0)
        {
            a = new MakeAction();
        }
        else if (type == 1)
        {
            a = new FunctionAction(null);
        }
        
        boolean hasPreferenceType = dis.readBoolean();
        if (hasPreferenceType)
        {
            String preference_type = dis.readUTF();
            a.preference_type = PreferenceType.valueOf(preference_type);
        }
        else
        {
            a.preference_type = null;
        }
        a.support = ActionSupport.valueOf(dis.readUTF());

        // FUNCALL_ACTION
        if (type == 1)
        {
            FunctionAction fa = a.asFunctionAction();
            fa.call = reteload_rhs_value(dis, symbolMap, alphaMemories).asFunctionCall();
        }
        // MAKE_ACTION
        else if (type == 0)
        {
            MakeAction ma = a.asMakeAction();
            ma.id = reteload_rhs_value(dis, symbolMap, alphaMemories);
            ma.attr = reteload_rhs_value(dis, symbolMap, alphaMemories);
            ma.value = reteload_rhs_value(dis, symbolMap, alphaMemories);
            if (a.preference_type != null && a.preference_type.isBinary())
            {
                ma.referent = reteload_rhs_value(dis, symbolMap, alphaMemories);
            }
            else
            {
                ma.referent = null;
            }
        }

        return a;
            }

    private RhsValue reteload_rhs_value(DataInputStream dis,
            List<Symbol> symbolMap, List<AlphaMemory> alphaMemories) throws IOException, SoarException
    {
        RhsValue rv = null;
        SymbolImpl sym;
        int field_num;
        int type;
        int levels_up;

        type = dis.readInt();
        switch (type) {
        case 0: // RhsSymbolValue
            // TODO: Is it okay to just recreate the RhsSymbolValue?
          sym = getSymbol(symbolMap, dis.readInt());
          rv = new RhsSymbolValue(sym);
          break;
        case 1: // RhsFunctionCall
          sym = getSymbol(symbolMap, dis.readInt());
          boolean isStandalone = dis.readBoolean();
          
          // TODO: How do we check for non-existent RHS functions?
//          RhsFunctionHandler rf = context.getRhsFunctions().getHandler(sym.asString().getValue());
//          if (rf == null)
//          {
//              throw new SoarException("Undefined RHS function: " + sym.asString().getValue());
//          }
          RhsFunctionCall funCall = new RhsFunctionCall(sym.asString(), isStandalone);
          int count = dis.readInt();
          while (count-- > 0)
          {
              funCall.addArgument(reteload_rhs_value(dis, symbolMap, alphaMemories));
          }
          rv = funCall;
          break;
        case 2: // ReteLocation
          field_num = dis.readInt();
          levels_up = dis.readInt();
          rv = ReteLocation.create(field_num, levels_up);
          break;
        case 3: // UnboundVariable
          int i = dis.readInt(); // Index of the unbound variable.
          rete.update_max_rhs_unbound_variables(i+1);
          rv = UnboundVariable.create(i);
          break;
        }
        
        return rv;
    }

    private ReteTest readTestList(DataInputStream dis, List<Symbol> symbolMap,
            List<AlphaMemory> alphaMemories) throws IOException, SoarException
    {  
        ReteTest rt, prev_rt, first;
        int count;
    
        prev_rt = null;
        first = null;
        count = dis.readInt();
        while (count-- > 0) {
            rt = readTest(dis, symbolMap, alphaMemories);
            if (prev_rt != null)
            {
                prev_rt.next = rt; 
            }
            else
            {
                first = rt;
            }
            prev_rt = rt;
        }
        if (prev_rt != null)
        {
            prev_rt.next = null;
        }
        else
        {
            first = null;
        }
        
        return first;
    }
        private ReteTest readTest(DataInputStream dis, List<Symbol> symbolMap,
            List<AlphaMemory> alphaMemories) throws IOException, SoarException
    {
        SymbolImpl sym;

        int type = dis.readInt();
        int right_field_num = dis.readInt();
        
        ReteTest rt = new ReteTest(type);
        if (rt.test_is_constant_relational_test())
        {
            type -= ReteTest.CONSTANT_RELATIONAL; // ReteTest's constructor will add this back in.
            sym = getSymbol(symbolMap, dis.readInt());
            rt = ReteTest.createConstantTest(type, right_field_num, (SymbolImpl)sym);
        }
        else if (rt.test_is_variable_relational_test()) {
            type -= ReteTest.VARIABLE_RELATIONAL; // ReteTest's constructor will add this back in.
            int field_num = dis.readInt();
            int levels_up = dis.readInt();
            rt = ReteTest.createVariableTest(type, right_field_num, new VarLocation(levels_up, field_num));
        }
        else if (type == ReteTest.DISJUNCTION) {
          int count = dis.readInt();
          List<SymbolImpl> disjuncts = new ArrayList<SymbolImpl>(count);
          
          while (count-- > 0) {
            sym = getSymbol(symbolMap, dis.readInt());
            disjuncts.add((SymbolImpl)sym);
          }
          rt = ReteTest.createDisjunctionTest(right_field_num, disjuncts);
        }
        else if (type == ReteTest.ID_IS_GOAL)
        {
           rt = ReteTest.createGoalIdTest(); 
        }
        else if (type == ReteTest.ID_IS_IMPASSE)
        {
            rt = ReteTest.createImpasseIdTest();
        }
        else
        {
            throw new SoarException("Unknown test type: " + rt + " (" + type + ")");
        }
        
        return rt;
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
                return syms.createInteger(dis.readLong());
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
            ReteNode temp = node.b_cn().partner.parent;
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
        Object id = readVarNames(dis, symbolMap);
        Object attr = readVarNames(dis, symbolMap);
        Object value = readVarNames(dis, symbolMap);
        NodeVarNames parent = readNodeVarNames(dis, node.real_parent_node(), symbolMap);
        return NodeVarNames.newInstance(parent, id, attr, value);
    }
}

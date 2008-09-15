/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 5, 2008
 */
package org.jsoar.kernel.rete;

import org.jsoar.kernel.AssertListType;
import org.jsoar.kernel.MatchSetChange;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.ProductionSupport;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.SavedFiringType;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.ReteLocation;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.kernel.symbols.SymConstant;
import org.jsoar.util.ListHead;

/**
 * Soar-specific implementation of the {@link ReteListener} interface. This includes
 * all of the stuff like Soar 7 mode, match set changes, etc.
 * 
 * @author ray
 */
public class SoarReteListener implements ReteListener
{
    private final Rete rete;
    
    /**
     * false is Soar 7 mode
     * 
     * agent.h:728
     */
    private boolean operand2_mode = true;
    
    /**
     * agent.h:733
     * dll of all retractions for removed (ie nil) goals
     */
    public final ListHead<MatchSetChange> nil_goal_retractions = new ListHead<MatchSetChange>();
    
    /**
     * changes to match set
     * 
     * agent.h:231
     */
    private final ListHead<MatchSetChange> ms_assertions = new ListHead<MatchSetChange>();
    
    /**
     * agent.h:231
     */
    private final ListHead<MatchSetChange> ms_retractions = new ListHead<MatchSetChange>();
    
    /**
     * changes to match set
     * 
     * agent.h:723
     */
    private final ListHead<MatchSetChange> ms_o_assertions = new ListHead<MatchSetChange>();
    private final ListHead<MatchSetChange> ms_i_assertions = new ListHead<MatchSetChange>();
    
    private final SymConstant operator_symbol;
    private int o_support_calculation_type = 4;

    /**
     * @param operator_symbol
     */
    public SoarReteListener(Rete rete, SymConstant operator_symbol)
    {
        this.rete = rete;
        this.operator_symbol = operator_symbol;
    }

    
    /**
     * @return the rete
     */
    public Rete getRete()
    {
        return rete;
    }


    /* (non-Javadoc)
     * @see org.jsoar.kernel.rete.ReteListener#finishRefraction(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.Production, org.jsoar.kernel.rete.Instantiation, org.jsoar.kernel.rete.ReteNode)
     */
    @Override
    public boolean finishRefraction(Rete rete, Production p, Instantiation refracted_inst, ReteNode p_node)
    {
        refracted_inst.inProdList.remove(p.instantiations);
        boolean refactedInstMatched = p_node.b_p.tentative_retractions.isEmpty();
        if (!refactedInstMatched)
        {
            MatchSetChange msc = p_node.b_p.tentative_retractions.first.get();
            p_node.b_p.tentative_retractions.first = null;
            msc.next_prev.remove(ms_retractions);
            /* REW: begin 10.03.97 *//* BUGFIX 2.125 */
            if (operand2_mode)
            {
                if (msc.goal != null)
                {
                    msc.in_level.remove(msc.goal.ms_retractions);
                }
                else
                {
                    msc.in_level.remove(nil_goal_retractions);
                }
            }
            /* REW: end   10.03.97 */
        }
        return refactedInstMatched;
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rete.ReteListener#p_node_left_addition(org.jsoar.kernel.rete.ReteNode, org.jsoar.kernel.rete.Token, org.jsoar.kernel.Wme)
     */
    @Override
    public void p_node_left_addition(Rete rete, ReteNode node, Token tok, Wme w)
    {
        /*
         * Algorithm:
         * 
         * Does this token match (wme's equal) one of tentative_retractions?
         *   (We have to check instantiation structure for this--when an
         *   instantiation retracts then re-asserts in one e-cycle, the
         *   token itself will be different, but all the wme's tested positively
         *   will be the same.)
         * If so, remove that tentative_retraction.
         * If not, store this new token in tentative_assertions.
         */
        /* --- check for match in tentative_retractions --- */
        boolean match_found = false;
        MatchSetChange msc = null;
        for (MatchSetChange mscTemp : node.b_p.tentative_retractions)
        {
            msc = mscTemp;
            match_found = true;
            Condition cond = msc.inst.bottom_of_instantiated_conditions;
            Token current_token = tok;
            Wme current_wme = w;
            ReteNode current_node = node.parent;
            while (current_node.node_type != ReteNodeType.DUMMY_TOP_BNODE)
            {
                if (current_node.node_type.bnode_is_positive())
                {
                    if (current_wme != cond.bt.wme_)
                    {
                        match_found = false;
                        break;
                    }
                }
                current_node = current_node.real_parent_node();
                current_wme = current_token.w;
                current_token = current_token.parent;
                cond = cond.prev;
            }
            if (match_found)
            {
                break;
            }
        }

        // TODO: Is BUG_139_WORKAROUND needed?
        // #ifdef BUG_139_WORKAROUND
        /*
         * --- test workaround for bug #139: don't rematch justifications; let
         * them be removed ---
         */
        /*
         * note that the justification is added to the retraction list when it
         * is first created, so we let it match the first time, but not after
         * that
         */
        if (match_found && node.b_p.prod.type == ProductionType.JUSTIFICATION_PRODUCTION_TYPE)
        {
            if (node.b_p.prod.already_fired)
            {
                return;
            }
            else
            {
                node.b_p.prod.already_fired = true;
            }
        }
        // #endif

        /* --- if match found tentative_retractions, remove it --- */
        if (match_found)
        {
            msc.inst.rete_token = tok;
            msc.inst.rete_wme = w;
            msc.of_node.remove(node.b_p.tentative_retractions);
            msc.next_prev.remove(ms_retractions);
            /* REW: begin 08.20.97 */
            if (msc.goal != null)
            {
                msc.in_level.remove(msc.goal.ms_retractions);
            }
            else
            {
                // BUGBUG FIXME BADBAD TODO
                // RPM 6/05
                // This if statement is to avoid a crash we get on most
                // platforms in Soar 7 mode
                // It's unknown what consequences it has, but the Soar 7 demos
                // seem to work
                // To return things to how they were, simply remove the if
                // statement (but leave
                // the remove_from_dll line).
                if (!nil_goal_retractions.isEmpty())
                {
                    msc.in_level.remove(nil_goal_retractions);
                }
            }
            /* REW: end 08.20.97 */

            // #ifdef DEBUG_RETE_PNODES
            // print_with_symbols (thisAgent, "\nRemoving tentative retraction:
            // %y",
            // node->b.p.prod->name);
            // #endif
            return;
        }

        /* --- no match found, so add new assertion --- */
        // #ifdef DEBUG_RETE_PNODES
        // print_with_symbols (thisAgent, "\nAdding tentative assertion: %y",
        // node->b.p.prod->name);
        // #endif
        msc = new MatchSetChange();
        msc.tok = tok;
        msc.w = w;
        msc.p_node = node;

        /* RCHONG: begin 10.11 */

        /*
         * (this is a RCHONG comment, but might also apply to Operand2...?)
         * 
         * what we have to do now is to, essentially, determine the kind of
         * support this production would get based on its present complete
         * matches. once i know the support, i can then know into which match
         * set list to put "msc".
         * 
         * this code is used to make separate PE productions from IE productions
         * by putting them into different match set lists. in non-OPERAND, these
         * matches would all go into one list.
         * 
         * BUGBUG i haven't tested this with a production that has more than one
         * match where the matches could have different support. is that even
         * possible???
         * 
         */

        /* operand code removed 1/22/99 - kjc */

        /* REW: begin 09.15.96 */
        if (operand2_mode)
        {

            /* REW: begin 08.20.97 */
            /* Find the goal and level for this ms change */
            msc.goal = msc.find_goal_for_match_set_change_assertion(rete.dummy_top_token);
            msc.level = msc.goal.level;
            /* REW: end 08.20.97 */

            SavedFiringType prod_type = SavedFiringType.IE_PRODS;

            if (node.b_p.prod.declared_support == ProductionSupport.DECLARED_O_SUPPORT)
            {
                prod_type = SavedFiringType.PE_PRODS;
            }
            else if (node.b_p.prod.declared_support == ProductionSupport.DECLARED_I_SUPPORT)
            {
                prod_type = SavedFiringType.IE_PRODS;
            }
            else if (node.b_p.prod.declared_support == ProductionSupport.UNDECLARED_SUPPORT)
            {

                /*
                 * check if the instantiation is proposing an operator. if it
                 * is, then this instantiation is i-supported.
                 */

                boolean operator_proposal = false;

                for (Action act = node.b_p.prod.action_list; act != null; act = act.next)
                {
                    MakeAction ma = act.asMakeAction();
                    if (ma != null && (ma.attr.asSymbolValue() != null))
                    {
                        if ("operator".equals(ma.attr.asSymbolValue().toString())
                                && (act.preference_type == PreferenceType.ACCEPTABLE_PREFERENCE_TYPE))
                        {
                            operator_proposal = true;
                            prod_type = SavedFiringType.IE_PRODS; // TODO ???
                                                                    // !PE_PRODS;
                                                                    // ???
                            break;
                        }
                    }
                }

                if (!operator_proposal)
                {

                    // examine all the different matches for this productions

                    for (Token OPERAND_curr_tok : node.a_np.tokens)
                    {

                        /*
                         * 
                         * i'll need to make two passes over each set of wmes
                         * that match this production. the first pass looks for
                         * the lowest goal identifier. the second pass looks for
                         * a wme of the form:
                         *  (<lowest-goal-id> ^operator ...)
                         * 
                         * if such a wme is found, then this production is a
                         * PE_PROD. otherwise, it's a IE_PROD.
                         * 
                         * admittedly, this implementation is kinda sloppy. i
                         * need to clean it up some.
                         * 
                         * BUGBUG this check only looks at positive conditions.
                         * we haven't really decided what testing the absence of
                         * the operator will do. this code assumes that such a
                         * productions (instantiation) would get i-support.
                         * 
                         * Modified 1/00 by KJC for operand2_mode == TRUE AND
                         * o-support-mode == 3: prods that have ONLY operator
                         * elaborations (<o> ^attr ^value) are IE_PROD. If prod
                         * has both operator applications and <o> elabs, then
                         * it's PE_PROD and the user is warned that <o> elabs
                         * will be o-supported.
                         * 
                         */
                        boolean op_elab = false;
                        Wme lowest_goal_wme = null;

                        for (int pass = 0; pass != 2; pass++)
                        {

                            Token temp_tok = OPERAND_curr_tok;
                            while (temp_tok != null)
                            {
                                while (temp_tok.w == null)
                                {
                                    temp_tok = temp_tok.parent;
                                    if (temp_tok == null)
                                    {
                                        break;
                                    }
                                }
                                if (temp_tok == null)
                                {
                                    break;
                                }
                                if (temp_tok.w == null)
                                {
                                    break;
                                }

                                if (pass == 0)
                                {
                                    if (temp_tok.w.id.isa_goal)
                                    {
                                        if (lowest_goal_wme == null)
                                        {
                                            lowest_goal_wme = temp_tok.w;
                                        }
                                        else
                                        {
                                            if (temp_tok.w.id.level > lowest_goal_wme.id.level)
                                            {
                                                lowest_goal_wme = temp_tok.w;
                                            }
                                        }
                                    }
                                }
                                else
                                {
                                    if ((temp_tok.w.attr == operator_symbol) && (temp_tok.w.acceptable == false)
                                            && (temp_tok.w.id == lowest_goal_wme.id))
                                    {
                                        if ((o_support_calculation_type == 3) || (o_support_calculation_type == 4))
                                        {
                                            /*
                                             * iff RHS has only operator
                                             * elaborations then it's IE_PROD,
                                             * otherwise PE_PROD, so look for
                                             * non-op-elabs in the actions KJC
                                             * 1/00
                                             */

                                            /*
                                             * We also need to check reteloc's
                                             * to see if they are referring to
                                             * operator augmentations before
                                             * determining if this is an
                                             * operator elaboration
                                             */

                                            for (Action act = node.b_p.prod.action_list; act != null; act = act.next)
                                            {
                                                MakeAction ma = act.asMakeAction();
                                                if (ma != null)
                                                {
                                                    RhsSymbolValue rhsSym = ma.id.asSymbolValue();
                                                    ReteLocation rl = ma.id.asReteLocation();
                                                    if ((rhsSym != null) &&

                                                    /***************************
                                                     * shouldn't this be either
                                                     * symbol_to_rhs_value
                                                     * (act->id) == or act->id ==
                                                     * rhs_value_to_symbol(temp..)
                                                     **************************/
                                                    (rhsSym.sym == temp_tok.w.value))
                                                    {
                                                        op_elab = true;
                                                    }
                                                    else if ((o_support_calculation_type == 4)
                                                            && (rl != null)
                                                            && (temp_tok.w.value == rete.get_symbol_from_rete_loc(rl
                                                                    .getLevelsUp(), rl.getFieldNum(), tok, w)))
                                                    {
                                                        op_elab = true;
                                                    }
                                                    else
                                                    {
                                                        /*
                                                         * this is not an
                                                         * operator elaboration
                                                         */
                                                        prod_type = SavedFiringType.PE_PRODS;
                                                    }
                                                } // act->type == MAKE_ACTION
                                            } // for
                                        }
                                        else
                                        {
                                            prod_type = SavedFiringType.PE_PRODS;
                                            break;
                                        }
                                    }
                                } /* end if (pass == 0) ... */
                                temp_tok = temp_tok.parent;
                            } /* end while (temp_tok != NIL) ... */

                            if (prod_type == SavedFiringType.PE_PRODS)
                                if ((o_support_calculation_type != 3) && (o_support_calculation_type != 4))
                                {
                                    break;
                                }
                                else if (op_elab)
                                {

                                    /* warn user about mixed actions */

                                    if ((o_support_calculation_type == 3) /*
                                                                             * &&
                                                                             * thisAgent->sysparams[PRINT_WARNINGS_SYSPARAM]
                                                                             */)
                                    {
                                        // TODO: Warning
                                        // print_with_symbols(thisAgent,
                                        // "\nWARNING: operator elaborations
                                        // mixed with operator applications\nget
                                        // o_support in prod %y",
                                        // node->b.p.prod->name);
                                        //                                    
                                        // // XML generation
                                        // growable_string gs =
                                        // make_blank_growable_string(thisAgent);
                                        // add_to_growable_string(thisAgent,
                                        // &gs, "WARNING: operator elaborations
                                        // mixed with operator applications\nget
                                        // o_support in prod ");
                                        // add_to_growable_string(thisAgent,
                                        // &gs, symbol_to_string(thisAgent,
                                        // node->b.p.prod->name, true, 0, 0));
                                        // xml_generate_warning(thisAgent,
                                        // text_of_growable_string(gs));
                                        // free_growable_string(thisAgent, gs);

                                        prod_type = SavedFiringType.PE_PRODS;
                                        break;
                                    }
                                    else if ((o_support_calculation_type == 4) /*
                                                                                 * &&
                                                                                 * thisAgent->sysparams[PRINT_WARNINGS_SYSPARAM]
                                                                                 */)
                                    {
                                        // TODO: Warning
                                        // print_with_symbols(thisAgent,
                                        // "\nWARNING: operator elaborations
                                        // mixed with operator applications\nget
                                        // i_support in prod %y",
                                        // node->b.p.prod->name);
                                        //
                                        // // XML generation
                                        // growable_string gs =
                                        // make_blank_growable_string(thisAgent);
                                        // add_to_growable_string(thisAgent,
                                        // &gs, "WARNING: operator elaborations
                                        // mixed with operator applications\nget
                                        // i_support in prod ");
                                        //                                    add_to_growable_string(thisAgent, &gs, symbol_to_string(thisAgent, node->b.p.prod->name, true, 0, 0));
                                        //                                    xml_generate_warning(thisAgent, text_of_growable_string(gs));
                                        //                                    free_growable_string(thisAgent, gs);

                                        prod_type = SavedFiringType.IE_PRODS;
                                        break;
                                    }
                                }
                        } /* end for pass =  */
                    } /* end for loop checking all matches */

                    /* BUG:  IF you print lowest_goal_wme here, you don't get what
                    you'd expect.  Instead of the lowest goal WME, it looks like
                    you get the lowest goal WME in the first/highest assertion of
                    all the matches for this production.  So, if there is a single
                    match, you get the right number.  If there are multiple matches
                    for the same production, you get the lowest goal of the
                    highest match goal production (or maybe just the first to
                    fire?).  I don;t know for certain if this is the behavior
                    Ron C. wanted or if it's a bug --
                    i need to talk to him about it. */

                } /* end if (operator_proposal == FALSE) */

            } /* end UNDECLARED_SUPPORT */

            if (prod_type == SavedFiringType.PE_PRODS)
            {
                msc.next_prev.insertAtHead(ms_o_assertions);

                /* REW: begin 08.20.97 */
                msc.in_level.insertAtHead(msc.goal.ms_o_assertions);
                /* REW: end   08.20.97 */

                node.b_p.prod.OPERAND_which_assert_list = AssertListType.O_LIST;

                // TODO: verbose
                //        if (thisAgent->soar_verbose_flag == TRUE) {
                //           print_with_symbols(thisAgent, "\n   RETE: putting [%y] into ms_o_assertions",
                //                              node->b.p.prod->name);
                //           char buf[256];
                //           SNPRINTF(buf, 254, "RETE: putting [%s] into ms_o_assertions", symbol_to_string(thisAgent, node->b.p.prod->name, true, 0, 0));
                //           xml_generate_verbose(thisAgent, buf);
                //        }
            }

            else
            {
                msc.next_prev.insertAtHead(ms_i_assertions);

                /* REW: end 08.20.97 */
                msc.in_level.insertAtHead(msc.goal.ms_i_assertions);
                /* REW: end 08.20.97 */

                node.b_p.prod.OPERAND_which_assert_list = AssertListType.I_LIST;

                // TODO: Verbose
                //        if (thisAgent->soar_verbose_flag == TRUE) {
                //           print_with_symbols(thisAgent, "\n   RETE: putting [%y] into ms_i_assertions",
                //                              node->b.p.prod->name);
                //           char buf[256];
                //           SNPRINTF(buf, 254, "RETE: putting [%s] into ms_i_assertions", symbol_to_string(thisAgent, node->b.p.prod->name, true, 0, 0));
                //           xml_generate_verbose(thisAgent, buf);
                //        }
            }
        }
        /* REW: end   09.15.96 */

        else
        { /* non-Operand* flavor Soar */
            msc.next_prev.insertAtHead(ms_assertions);
        }
        ///
        // Location for Match Interrupt

        /* RCHONG: end 10.11 */

        msc.of_node.insertAtHead(node.b_p.tentative_assertions);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rete.ReteListener#p_node_left_removal(org.jsoar.kernel.rete.ReteNode, org.jsoar.kernel.rete.Token, org.jsoar.kernel.Wme)
     */
    @Override
    public void p_node_left_removal(Rete rete, ReteNode node, Token tok, Wme w)
    {
        /*
         * Does this token match (eq) one of the tentative_assertions?
         * If so, just remove that tentative_assertion.
         * If not, find the instantiation corresponding to this token
         * and add it to tentative_retractions.
         */
        
        /* --- check for match in tentative_assertions --- */
        for (MatchSetChange msc : node.b_p.tentative_assertions)
        {
            if ((msc.tok == tok) && (msc.w == w))
            {
                /* --- match found in tentative_assertions, so remove it --- */
                msc.of_node.remove(node.b_p.tentative_assertions);

                /* REW: begin 09.15.96 */
                if (operand2_mode)
                {
                    if (node.b_p.prod.OPERAND_which_assert_list == AssertListType.O_LIST)
                    {
                        msc.next_prev.remove(ms_o_assertions);
                        /* REW: begin 08.20.97 */
                        /*
                         * msc already defined for the assertion so the goal
                         * should be defined as well.
                         */
                        msc.in_level.remove(msc.goal.ms_o_assertions);
                        /* REW: end 08.20.97 */
                    }
                    else if (node.b_p.prod.OPERAND_which_assert_list == AssertListType.I_LIST)
                    {
                        msc.next_prev.remove(ms_i_assertions);
                        /* REW: begin 08.20.97 */
                        msc.in_level.remove(msc.goal.ms_i_assertions);
                        /* REW: end 08.20.97 */
                    }
                }
                /* REW: end 09.15.96 */

                else
                {
                    msc.next_prev.remove(ms_assertions);
                }
                // #ifdef DEBUG_RETE_PNODES
                // print_with_symbols (thisAgent, "\nRemoving tentative
                // assertion: %y",
                // node->b.p.prod->name);
                // #endif
                return;
            }
        } /* end of for loop */

        /* --- find the instantiation corresponding to this token --- */
        Instantiation inst = null;
        for (Instantiation instTemp : node.b_p.prod.instantiations)
        {
            inst = instTemp;
            if ((inst.rete_token == tok) && (inst.rete_wme == w))
            {
                break;
            }
        }

        if (inst != null)
        {
            /* --- add that instantiation to tentative_retractions --- */
            // #ifdef DEBUG_RETE_PNODES
            // print_with_symbols (thisAgent, "\nAdding tentative retraction:
            // %y",
            // node->b.p.prod->name);
            // #endif
            inst.rete_token = null;
            inst.rete_wme = null;
            MatchSetChange msc = new MatchSetChange();
            msc.inst = inst;
            msc.p_node = node;
            msc.of_node.insertAtHead(node.b_p.tentative_retractions);

            /* REW: begin 08.20.97 */

            if (operand2_mode)
            {
                /*
                 * Determine what the goal of the msc is and add it to that
                 * goal's list of retractions
                 */
                msc.goal = msc.find_goal_for_match_set_change_retraction();
                msc.level = msc.goal.level;

                // #ifdef DEBUG_WATERFALL
                // print("\n Level of retraction is: %d", msc->level);
                // #endif

                if (msc.goal.link_count == 0)
                {
                    /*
                     * BUG (potential) (Operand2/Waterfall: 2.101) When a goal
                     * is removed in the stack, it is not immediately garbage
                     * collected, meaning that the goal pointer is still valid
                     * when the retraction is created. So the goal for a
                     * retraction will always be valid, even though, for
                     * retractions caused by goal removals, the goal will be
                     * removed at the next WM phase. (You can see this by
                     * printing the identifier for the goal in the elaboration
                     * cycle after goal removal. It's still there, although
                     * nothing is attacjed to it. One elab later, the identifier
                     * itself is removed.) Because Waterfall needs to know if
                     * the goal is valid or not, I look at the link_count on the
                     * symbol. A link_count of 0 is the trigger for the garbage
                     * collection so this solution should work -- I just make
                     * the pointer NIL to ensure that the retractions get added
                     * to the NIL_goal_retraction list. However, if the
                     * link_count is never not* zero for an already removed
                     * goal, this solution will fail, resulting in both the
                     * retraction never being able to fire and a memory leak
                     * (because the items on the ms_change list on the symbol
                     * will never be freed).
                     */
                    /*
                     * print("\nThis goal is being removed. Changing msc goal
                     * pointer to NIL.");
                     */
                    msc.goal = null;
                }

                /* Put on the original retraction list */
                msc.next_prev.insertAtHead(ms_retractions);
                if (msc.goal != null)
                { /* Goal exists */
                    msc.in_level.insertAtHead(msc.goal.ms_retractions);
                }
                else
                { /* NIL Goal; put on the NIL Goal list */
                    msc.in_level.insertAtHead(nil_goal_retractions);
                }

                // #ifdef DEBUG_WATERFALL
                // print_with_symbols(thisAgent, "\nRetraction: %y",
                // msc->inst->prod->name);
                // print(" is active at level %d\n", msc->level);
                //
                // { ms_change *assertion;
                // print("\n Retractions list:\n");
                // for (assertion=thisAgent->ms_retractions;
                // assertion;
                // assertion=assertion->next) {
                // print_with_symbols(thisAgent, " Retraction: %y ",
                // assertion->p_node->b.p.prod->name);
                // print(" at level %d\n", assertion->level);
                // }
                //
                // if (thisAgent->nil_goal_retractions) {
                // print("\nCurrent NIL Goal list:\n");
                // assertion = NIL;
                // for (assertion=thisAgent->nil_goal_retractions;
                // assertion;
                // assertion=assertion->next_in_level) {
                //            print_with_symbols(thisAgent, "     Retraction: %y ",
                //                               assertion->p_node->b.p.prod->name);
                //            print(" at level %d\n", assertion->level);
                //            if (assertion->goal) print("This assertion has non-NIL goal pointer.\n");
                //          } 
                //        }
                //        }
                //#endif 
                /* REW: end   08.20.97 */

            }
            else
            { /* For Reg. Soar just add it to the list */
                msc.next_prev.insertAtHead(ms_retractions);
            }
            return;
        }

        /* REW: begin 09.15.96 */

        // TODO: verbose
        //  if (operand2_mode &&
        //      (thisAgent->soar_verbose_flag == TRUE)) {
        //          print_with_symbols (thisAgent, "\n%y: ",node->b.p.prod->name);
        //          char buf[256];
        //          SNPRINTF(buf, 254, "%s: ", symbol_to_string(thisAgent, node->b.p.prod->name, true, 0, 0));
        //          xml_generate_verbose(thisAgent, buf);
        //      }
        /* REW: end   09.15.96 */
        //#ifdef BUG_139_WORKAROUND
        if (node.b_p.prod.type == ProductionType.JUSTIFICATION_PRODUCTION_TYPE)
        {
            // TODO Warning
            //#ifdef BUG_139_WORKAROUND_WARNING
            //        print(thisAgent, "\nWarning: can't find an existing inst to retract (BUG 139 WORKAROUND)\n");
            //        xml_generate_warning(thisAgent, "Warning: can't find an existing inst to retract (BUG 139 WORKAROUND)");
            //#endif
            return;
        }
        //#endif

        throw new IllegalStateException("Internal error: can't find existing instantiation to retract");
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rete.ReteListener#startRefraction(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.Production, org.jsoar.kernel.rete.Instantiation, org.jsoar.kernel.rete.ReteNode)
     */
    @Override
    public void startRefraction(Rete rete, Production p, Instantiation refracted_inst, ReteNode p_node)
    {
        refracted_inst.inProdList.insertAtHead(p.instantiations);
        refracted_inst.rete_token = null;
        refracted_inst.rete_wme = null;
        MatchSetChange msc = new MatchSetChange();
        msc.inst = refracted_inst;
        msc.p_node = p_node;
        /* REW: begin 08.20.97 */
        /*
         * Because the RETE 'artificially' refracts this instantiation (ie,
         * it is not actually firing -- the original instantiation fires but
         * not the chunk), we make the refracted instantiation of the chunk
         * a nil_goal retraction, rather than associating it with the
         * activity of its match goal. In p_node_left_addition, where the
         * tentative assertion will be generated, we make it a point to look
         * at the goal value and exrtac from the appropriate list; here we
         * just make a a simplifying assumption that the goal is NIL
         * (although, in reality), it never will be.
         */

        /*
         * This initialization is necessary (for at least safety reasons,
         * for all msc's, regardless of the mode
         */
        msc.level = 0;
        msc.goal = null;
        if (operand2_mode)
        {

            // #ifdef DEBUG_WATERFALL
            // print_with_symbols(thisAgent, "\n %y is a refracted
            // instantiation",
            // refracted_inst->prod->name);
            // #endif
            msc.in_level.insertAtHead(nil_goal_retractions);
        }
        /* REW: end 08.20.97 */

        // TODO: Is BUG_139_WORKAROUND needed?
        // #ifdef BUG_139_WORKAROUND
        msc.p_node.b_p.prod.already_fired = false; // RPM workaround for bug #139; mark prod as not fired yet */
        // #endif
        msc.next_prev.insertAtHead(ms_retractions);
        msc.of_node.insertAtHead(p_node.b_p.tentative_retractions);
    }

    /* (non-Javadoc)
     * @see org.jsoar.kernel.rete.ReteListener#removingProductionNode(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.rete.ReteNode)
     */
    @Override
    public void removingProductionNode(Rete rete, ReteNode p_node)
    {
        // Originally in excise_production_from_rete() in rete.cpp. Extracted to
        // decouple generic rete from Soar-specific stuff.
        
        /*
         * --- At this point, there are no tentative_assertion's. Now set the
         * p_node field of all tentative_retractions to NIL, to indicate that
         * the p_node is being excised ---
         */
        
        for (MatchSetChange msc : p_node.b_p.tentative_retractions)
        {
            msc.p_node = null;
        }
    }


    /**
     * @return
     */
    public SoarReteAssertion get_next_assertion()
    {
        // TODO implement get_next_assertion
        throw new UnsupportedOperationException("get_next_assertion not implemented");
    }


    /**
     * @return
     */
    public Instantiation get_next_retraction()
    {
        // TODO implement get_next_retraction
        throw new UnsupportedOperationException("get_next_retraction not implemented");
    }

    public boolean hasNilGoalRetractions()
    {
        return !nil_goal_retractions.isEmpty();
    }

    /**
     * @return
     */
    public Instantiation get_next_nil_goal_retraction()
    {
        // TODO implement get_next_nil_goal_retraction
        throw new UnsupportedOperationException("get_next_nil_goal_retraction not implemented");
    }


    /**
     * @return
     */
    public boolean any_assertions_or_retractions_ready()
    {
        // TODO implement any_assertions_or_retractions_ready
        throw new UnsupportedOperationException("any_assertions_or_retractions_ready not implemented");
    }
}

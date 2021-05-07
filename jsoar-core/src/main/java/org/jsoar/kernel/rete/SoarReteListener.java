/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 5, 2008
 */
package org.jsoar.kernel.rete;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.Decider;
import org.jsoar.kernel.DecisionCycle;
import org.jsoar.kernel.MatchSet;
import org.jsoar.kernel.MatchSetEntry;
import org.jsoar.kernel.MatchSetEntry.EntryType;
import org.jsoar.kernel.PredefinedSymbols;
import org.jsoar.kernel.Production;
import org.jsoar.kernel.Production.Support;
import org.jsoar.kernel.ProductionType;
import org.jsoar.kernel.SavedFiringType;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.memory.Instantiation;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.memory.RecognitionMemory;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.rete.ProductionNodeData.AssertListType;
import org.jsoar.kernel.rhs.Action;
import org.jsoar.kernel.rhs.MakeAction;
import org.jsoar.kernel.rhs.ReteLocation;
import org.jsoar.kernel.rhs.RhsSymbolValue;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.kernel.tracing.Trace.MatchSetTraceType;
import org.jsoar.kernel.tracing.Trace.WmeTraceType;
import org.jsoar.kernel.wma.WorkingMemoryActivation;
import org.jsoar.util.ListHead;
import org.jsoar.util.adaptables.Adaptables;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 *
 * <p>Soar-specific implementation of the {@link ReteListener} interface. This includes all of the
 * stuff like Soar 7 mode, match set changes, etc.
 *
 * @author ray
 */
public class SoarReteListener implements ReteListener {
  private final Agent context;
  private final Rete rete;
  private PredefinedSymbols predefinedSyms;
  private Decider decider;
  private RecognitionMemory recMemory;
  private WorkingMemoryActivation wma;

  /** agent.h:733 dll of all retractions for removed (ie nil) goals */
  public ListHead<MatchSetChange> nil_goal_retractions = ListHead.newInstance();

  /**
   * changes to match set
   *
   * <p>agent.h:231
   *
   * @see MatchSetChange#next_of_all
   */
  MatchSetChange ms_assertions;

  /**
   * agent.h:231
   *
   * @see MatchSetChange#next_of_all
   */
  public MatchSetChange ms_retractions;

  /**
   * changes to match set
   *
   * <p>agent.h:723
   *
   * @see MatchSetChange#next_of_all
   */
  public MatchSetChange ms_o_assertions;
  /** @see MatchSetChange#next_of_all */
  public MatchSetChange ms_i_assertions;

  /** New waterfall model: postponed assertions that can be restored if they don't fire */
  public final ListHead<MatchSetChange> postponed_assertions = ListHead.newInstance();

  /**
   * Constuct a new rete listener. {@link #initialize()} must be called as well.
   *
   * @param context the owning agent
   * @param rete the rete
   */
  public SoarReteListener(Agent context, Rete rete) {
    this.context = context;
    this.rete = rete;
    this.rete.setReteListener(this);
  }

  public void initialize() {
    this.predefinedSyms = Adaptables.adapt(context, PredefinedSymbols.class);
    this.decider = Adaptables.adapt(context, Decider.class);
    this.recMemory = Adaptables.adapt(context, RecognitionMemory.class);
    this.wma = Adaptables.adapt(context, WorkingMemoryActivation.class);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.rete.ReteListener#finishRefraction(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.Production, org.jsoar.kernel.rete.Instantiation, org.jsoar.kernel.rete.ReteNode)
   */
  @Override
  public boolean finishRefraction(
      Rete rete, Production p, Instantiation refracted_inst, ReteNode p_node) {
    p.instantiations = refracted_inst.removeFromProdList(p.instantiations);
    final boolean refactedInstMatched = p_node.b_p().tentative_retractions == null;
    if (!refactedInstMatched) {
      final MatchSetChange msc = p_node.b_p().tentative_retractions;
      p_node.b_p().tentative_retractions = null;
      ms_retractions = msc.removeFromAllList(ms_retractions);

      if (msc.goal != null) {
        msc.in_level.remove(msc.goal.goalInfo.ms_retractions);
      } else {
        msc.in_level.remove(nil_goal_retractions);
      }
    }
    return refactedInstMatched;
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.rete.ReteListener#p_node_left_addition(org.jsoar.kernel.rete.ReteNode, org.jsoar.kernel.rete.Token, org.jsoar.kernel.Wme)
   */
  @Override
  public void p_node_left_addition(Rete rete, ReteNode node, Token tok, WmeImpl w) {
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
    // check for match in tentative_retractions
    boolean match_found = false;
    MatchSetChange msc = null;
    for (msc = node.b_p().tentative_retractions; msc != null; msc = msc.next_of_node) {
      match_found = true;
      Condition cond = msc.inst.bottom_of_instantiated_conditions;
      Token current_token = tok;
      WmeImpl current_wme = w;
      ReteNode current_node = node.parent;
      while (current_node.node_type != ReteNodeType.DUMMY_TOP_BNODE) {
        if (current_node.node_type.bnode_is_positive()) {
          if (current_wme != cond.asPositiveCondition().bt().wme_) {
            match_found = false;
            break;
          }
        }
        current_node = current_node.real_parent_node();
        current_wme = current_token.w;
        current_token = current_token.parent;
        cond = cond.prev;
      }
      if (match_found) {
        break;
      }
    }

    /*
     * note that the justification is added to the retraction list when it
     * is first created, so we let it match the first time, but not after
     * that
     */
    if (match_found && node.b_p().prod.getType() == ProductionType.JUSTIFICATION) {
      if (node.b_p().justificationAlreadyFired) {
        return;
      } else {
        node.b_p().justificationAlreadyFired = true;
      }
    }

    // if match found tentative_retractions, remove it
    if (match_found) {
      msc.inst.rete_token = tok;
      msc.inst.rete_wme = w;
      node.b_p().tentative_retractions = msc.removeFromNodeList(node.b_p().tentative_retractions);
      ms_retractions = msc.removeFromAllList(ms_retractions);

      if (msc.goal != null) {
        msc.in_level.remove(msc.goal.goalInfo.ms_retractions);
      } else {
        // RPM 6/05
        // This if statement is to avoid a crash we get on most platforms in Soar 7 mode
        // It's unknown what consequences it has, but the Soar 7 demos seem to work
        // To return things to how they were, simply remove the if statement (but leave
        // the remove_from_dll line).
        // voigtjr 2009: returning things to how they were now that soar7 is removed
        // if (!nil_goal_retractions.isEmpty())
        // {
        msc.in_level.remove(nil_goal_retractions);
        // }
      }

      // #ifdef DEBUG_RETE_PNODES
      // print_with_symbols (thisAgent, "\nRemoving tentative retraction: %y",
      // node->b.p.prod->name);
      // #endif
      return;
    }

    // no match found, so add new assertion
    // #ifdef DEBUG_RETE_PNODES
    // print_with_symbols (thisAgent, "\nAdding tentative assertion: %y", node->b.p.prod->name);
    // #endif
    msc = MatchSetChange.createAssertion(node, tok, w);

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
     */

    // Find the goal and level for this ms change
    msc.goal = msc.find_goal_for_match_set_change_assertion(rete.dummy_top_token);
    msc.level = msc.goal.level;

    SavedFiringType prod_type = SavedFiringType.IE_PRODS;

    if (node.b_p().prod.getDeclaredSupport() == Support.DECLARED_O_SUPPORT) {
      prod_type = SavedFiringType.PE_PRODS;
    } else if (node.b_p().prod.getDeclaredSupport() == Support.DECLARED_I_SUPPORT) {
      prod_type = SavedFiringType.IE_PRODS;
    } else if (node.b_p().prod.getDeclaredSupport() == Support.UNDECLARED) {
      // check if the instantiation is proposing an operator. if it
      // is, then this instantiation is i-supported.
      boolean operator_proposal = false;

      for (Action act = node.b_p().prod.getFirstAction(); act != null; act = act.next) {
        final MakeAction ma = act.asMakeAction();
        final RhsSymbolValue attr = ma != null ? ma.attr.asSymbolValue() : null;
        if (attr != null) {
          if (predefinedSyms.operator_symbol == attr.getSym()
              && act.preference_type == PreferenceType.ACCEPTABLE) {
            operator_proposal = true;
            prod_type = SavedFiringType.IE_PRODS;
            break;
          }
        }
      }

      if (!operator_proposal) {
        // examine all the different matches for this productions

        for (Token it = node.a_np().tokens; it != null; it = it.next_of_node) {
          final Token OPERAND_curr_tok = it;
          /*
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
           * Modified 1/00 by KJC for context.operand2_mode == TRUE AND
           * o-support-mode == 3: prods that have ONLY operator
           * elaborations (<o> ^attr ^value) are IE_PROD. If prod
           * has both operator applications and <o> elabs, then
           * it's PE_PROD and the user is warned that <o> elabs
           * will be o-supported.
           */
          boolean op_elab = false;
          WmeImpl lowest_goal_wme = null;

          for (int pass = 0; pass != 2; pass++) {
            Token temp_tok = OPERAND_curr_tok;
            while (temp_tok != null) {
              while (temp_tok.w == null) {
                temp_tok = temp_tok.parent;
                if (temp_tok == null) {
                  break;
                }
              }
              if (temp_tok == null) {
                break;
              }
              if (temp_tok.w == null) {
                break;
              }

              if (pass == 0) {
                if (temp_tok.w.id.isGoal()) {
                  if (lowest_goal_wme == null) {
                    lowest_goal_wme = temp_tok.w;
                  } else {
                    if (temp_tok.w.id.level > lowest_goal_wme.id.level) {
                      lowest_goal_wme = temp_tok.w;
                    }
                  }
                }
              } else {
                if ((temp_tok.w.attr == predefinedSyms.operator_symbol)
                    && (temp_tok.w.acceptable == false)
                    && (temp_tok.w.id == lowest_goal_wme.id)) {
                  // former o_support_calculation_type (3 or 4)  test site

                  // iff RHS has only operator elaborations then it's IE_PROD,
                  // otherwise PE_PROD, so look for non-op-elabs in the actions KJC 1/00

                  // We also need to check reteloc's to see if they are referring to
                  // operator augmentations before determining if this is an
                  // operator elaboration

                  for (Action act = node.b_p().prod.getFirstAction(); act != null; act = act.next) {
                    MakeAction ma = act.asMakeAction();
                    if (ma != null) {
                      final RhsSymbolValue rhsSym = ma.id.asSymbolValue();
                      final ReteLocation rl = ma.id.asReteLocation();
                      if (rhsSym != null
                          &&

                          /***************************
                           * TODO shouldn't this be either
                           * symbol_to_rhs_value
                           * (act->id) == or act->id ==
                           * rhs_value_to_symbol(temp..)
                           **************************/
                          rhsSym.sym == temp_tok.w.value) {
                        op_elab = true;
                      } else if (
                      /* osupport.o_support_calculation_type == 4 &&*/
                      rl != null && temp_tok.w.value == rl.lookupSymbol(tok, w)) {
                        op_elab = true;
                      } else {
                        // this is not an operator elaboration
                        prod_type = SavedFiringType.PE_PRODS;
                      }
                    } // act->type == MAKE_ACTION
                  } // for
                }
              } /* end if (pass == 0) ... */
              temp_tok = temp_tok.parent;
            } /* end while (temp_tok != NIL) ... */

            if (prod_type == SavedFiringType.PE_PRODS) {
              // former o_support_calculation_type (3 or 4) test site
              if (op_elab) {
                // warn user about mixed actions
                if (context.getPrinter().isPrintWarnings()) {
                  context
                      .getPrinter()
                      .warn(
                          "\nWARNING: operator elaborations mixed with operator applications\n"
                              + "get i_support in prod %s",
                          node.b_p().prod.getName());
                }
                prod_type = SavedFiringType.IE_PRODS;
                break;
              }
            }
          }
          /* end for pass =  */ } /* end for loop checking all matches */

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

    final Trace trace = context.getTrace();
    if (prod_type == SavedFiringType.PE_PRODS) {
      ms_o_assertions = msc.addToHeadOfAllList(ms_o_assertions);
      msc.in_level.insertAtHead(msc.goal.goalInfo.ms_o_assertions);
      node.b_p().OPERAND_which_assert_list = AssertListType.O_LIST;

      trace.print(
          Category.VERBOSE,
          "\n   RETE: putting [%s] into ms_o_assertions",
          node.b_p().prod.getName());
    } else {
      ms_i_assertions = msc.addToHeadOfAllList(ms_i_assertions);
      msc.in_level.insertAtHead(msc.goal.goalInfo.ms_i_assertions);
      node.b_p().OPERAND_which_assert_list = AssertListType.I_LIST;

      trace.print(
          Category.VERBOSE,
          "\n   RETE: putting [%s] into ms_i_assertions",
          node.b_p().prod.getName());
    }
    ///
    // Location for Match Interrupt

    if (node.b_p().prod.isBreakpointEnabled()) {
      final DecisionCycle dc = Adaptables.adapt(context, DecisionCycle.class);
      if (dc != null) {
        dc.interrupt(node.b_p().prod.getName());
      }
    }

    node.b_p().tentative_assertions = msc.addToHeadOfNodeList(node.b_p().tentative_assertions);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.rete.ReteListener#p_node_left_removal(org.jsoar.kernel.rete.ReteNode, org.jsoar.kernel.rete.Token, org.jsoar.kernel.Wme)
   */
  @Override
  public void p_node_left_removal(Rete rete, ReteNode node, Token tok, WmeImpl w) {
    // rete.cpp:5893:p_node_left_removal

    /*
     * Does this token match (eq) one of the tentative_assertions?
     * If so, just remove that tentative_assertion.
     * If not, find the instantiation corresponding to this token
     * and add it to tentative_retractions.
     */

    // check for match in tentative_assertions
    for (MatchSetChange msc = node.b_p().tentative_assertions;
        msc != null;
        msc = msc.next_of_node) {
      if ((msc.tok == tok) && (msc.w == w)) {
        // match found in tentative_assertions, so remove it
        node.b_p().tentative_assertions = msc.removeFromNodeList(node.b_p().tentative_assertions);

        if (node.b_p().OPERAND_which_assert_list == AssertListType.O_LIST) {
          ms_o_assertions = msc.removeFromAllList(ms_o_assertions);
          // msc already defined for the assertion so the goal
          // should be defined as well.
          msc.in_level.remove(msc.goal.goalInfo.ms_o_assertions);
        } else if (node.b_p().OPERAND_which_assert_list == AssertListType.I_LIST) {
          ms_i_assertions = msc.removeFromAllList(ms_i_assertions);
          msc.in_level.remove(msc.goal.goalInfo.ms_i_assertions);
        }
        // #ifdef DEBUG_RETE_PNODES
        // print_with_symbols (thisAgent, "\nRemoving tentative assertion: %y",
        // node->b.p.prod->name);
        // #endif
        return;
      }
    }

    // find the instantiation corresponding to this token
    Instantiation inst = null;
    for (Instantiation instTemp = node.b_p().prod.instantiations;
        instTemp != null;
        instTemp = instTemp.nextInProdList) {
      inst = instTemp;
      if ((inst.rete_token == tok) && (inst.rete_wme == w)) {
        break;
      }
    }

    if (inst != null) {
      // add that instantiation to tentative_retractions

      // #ifdef DEBUG_RETE_PNODES
      // print_with_symbols (thisAgent, "\nAdding tentative retraction: %y", node->b.p.prod->name);
      // #endif
      inst.rete_token = null;
      inst.rete_wme = null;
      MatchSetChange msc = MatchSetChange.createRetraction(node, inst);
      node.b_p().tentative_retractions = msc.addToHeadOfNodeList(node.b_p().tentative_retractions);

      // Determine what the goal of the msc is and add it to that
      // goal's list of retractions
      msc.goal = msc.find_goal_for_match_set_change_retraction();
      msc.level = msc.goal.level;

      // #ifdef DEBUG_WATERFALL
      // print("\n Level of retraction is: %d", msc->level);
      // #endif

      if (msc.goal.link_count == 0) {
        /*
         * BUG (potential) (Operand2/Waterfall: 2.101) When a goal
         * is removed in the stack, it is not immediately garbage
         * collected, meaning that the goal pointer is still valid
         * when the retraction is created. So the goal for a
         * retraction will always be valid, even though, for
         * retractions caused by goal removals, the goal will be
         * removed at the next WM phases. (You can see this by
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
        // print("\nThis goal is being removed. Changing msc goal pointer to NIL.");
        msc.goal = null;
      }

      /* Put on the original retraction list */
      ms_retractions = msc.addToHeadOfAllList(ms_retractions);
      if (msc.goal != null) {
        /* Goal exists */
        msc.in_level.insertAtHead(msc.goal.goalInfo.ms_retractions);
      } else {
        /* NIL Goal; put on the NIL Goal list */
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
      // #endif
      return;
    }

    context.getTrace().print(Category.VERBOSE, "\n%s: ", node.b_p().prod.getName());

    if (node.b_p().prod.getType() == ProductionType.JUSTIFICATION) {
      return;
    }

    throw new IllegalStateException("Internal error: can't find existing instantiation to retract");
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.rete.ReteListener#startRefraction(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.Production, org.jsoar.kernel.rete.Instantiation, org.jsoar.kernel.rete.ReteNode)
   */
  @Override
  public void startRefraction(
      Rete rete, Production p, Instantiation refracted_inst, ReteNode p_node) {
    // rete.cpp:3628:add_production_to_rete
    p.instantiations = refracted_inst.insertAtHeadOfProdList(p.instantiations);
    refracted_inst.rete_token = null;
    refracted_inst.rete_wme = null;

    MatchSetChange msc = MatchSetChange.createRefracted(p_node, refracted_inst);

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
    // #ifdef DEBUG_WATERFALL
    // print_with_symbols(thisAgent, "\n %y is a refracted instantiation",
    // refracted_inst->prod->name);
    // #endif
    msc.in_level.insertAtHead(nil_goal_retractions);

    msc.p_node.b_p().justificationAlreadyFired = false; // mark prod as not fired yet

    ms_retractions = msc.addToHeadOfAllList(ms_retractions);
    p_node.b_p().tentative_retractions =
        msc.addToHeadOfNodeList(p_node.b_p().tentative_retractions);
  }

  /* (non-Javadoc)
   * @see org.jsoar.kernel.rete.ReteListener#removingProductionNode(org.jsoar.kernel.rete.Rete, org.jsoar.kernel.rete.ReteNode)
   */
  @Override
  public void removingProductionNode(Rete rete, ReteNode p_node) {
    // Originally in excise_production_from_rete() in rete.cpp. Extracted to
    // decouple generic rete from Soar-specific stuff.

    // At this point, there are no tentative_assertion's. Now set the
    // p_node field of all tentative_retractions to NIL, to indicate that
    // the p_node is being excised
    for (MatchSetChange msc = p_node.b_p().tentative_retractions;
        msc != null;
        msc = msc.next_of_node) {
      msc.p_node = null;
    }
  }

  /* New waterfall model:
   *
   * postpone_assertion: formerly get_next_assertion. Removes the first
   * assertion from the assertion lists and adds it to the postponed
   * assertions list. Returns false if there are no assertions.
   *
   * consume_last_postponed_assertion: removes the first assertion from the
   * postponed assertions list, making it go away permanently.
   *
   * restore_postponed_assertions: replaces the postponed assertions back on
   * the assertion lists.
   */
  public SoarReteAssertion postpone_assertion() {
    MatchSetChange msc = null;

    // In Waterfall, we return only assertions that match in the
    // currently active goal

    if (decider.active_goal != null) {
      /* Just do asserts for current goal */
      if (recMemory.FIRING_TYPE == SavedFiringType.PE_PRODS) {
        if (decider.active_goal.goalInfo.ms_o_assertions.isEmpty()) return null;

        msc = decider.active_goal.goalInfo.ms_o_assertions.pop();
        ms_o_assertions = msc.removeFromAllList(ms_o_assertions);
      } else {
        /* IE PRODS */
        if (decider.active_goal.goalInfo.ms_i_assertions.isEmpty()) return null;

        msc = decider.active_goal.goalInfo.ms_i_assertions.pop();
        ms_i_assertions = msc.removeFromAllList(ms_i_assertions);
      }
    } else {
      // If there is not an active goal, then there should not be any
      // assertions.  If there are, then we generate and error message
      // and abort.

      if ((ms_i_assertions != null) || (ms_o_assertions != null)) {
        // Commented out 11/2007
        // laird: I would like us to remove that error message that happens
        // in Obscurebot. It just freaks people out and we have yet
        // to see an error in Soar because of it.

        // char msg[BUFFER_MSG_SIZE];
        // strncpy(msg,"\nrete.c: Error: No active goal, but
        // assertions are on the assertion list.", BUFFER_MSG_SIZE);
        // msg[BUFFER_MSG_SIZE - 1] = 0; /* ensure null termination
        // */
        // abort_with_fatal_error(thisAgent, msg);
      }

      return null; // if we are in an initiazation and there are no
      // assertions, just retrurn FALSE to terminate the procedure.
    }

    msc.p_node.b_p().tentative_assertions =
        msc.removeFromNodeList(msc.p_node.b_p().tentative_assertions);

    // save the assertion on the postponed list
    postponed_assertions.push(msc);

    return new SoarReteAssertion(msc.p_node.b_p().prod, msc.tok, msc.w);
  }

  public void consume_last_postponed_assertion() {
    MatchSetChange msc = postponed_assertions.pop();
    assert msc != null;
  }

  public void restore_postponed_assertions() {
    while (!postponed_assertions.isEmpty()) {
      MatchSetChange msc = postponed_assertions.pop();
      assert msc != null;

      msc.p_node.b_p().tentative_assertions =
          msc.addToHeadOfNodeList(msc.p_node.b_p().tentative_assertions);

      assert decider.active_goal != null;

      if (recMemory.FIRING_TYPE == SavedFiringType.PE_PRODS) {
        ms_o_assertions = msc.addToHeadOfAllList(ms_o_assertions);
        decider.active_goal.goalInfo.ms_o_assertions.push(msc);
      } else {
        /* IE PRODS */
        ms_i_assertions = msc.addToHeadOfAllList(ms_i_assertions);
        decider.active_goal.goalInfo.ms_i_assertions.push(msc);
      }
    }
  }

  /**
   * rete.cpp:1238:get_next_retraction
   *
   * @return the next retraction
   */
  public Instantiation get_next_retraction() {
    /* just do the retractions for the current level */

    /* initialization condition (2.107/2.111) */
    if (decider.active_level == 0) return null;

    if (decider.active_goal.goalInfo.ms_retractions.isEmpty()) return null;

    // remove from the Waterfall-specific list */
    final MatchSetChange msc = decider.active_goal.goalInfo.ms_retractions.pop();
    // and remove from the complete retraction list
    ms_retractions = msc.removeFromAllList(ms_retractions);

    if (msc.p_node != null) {
      msc.p_node.b_p().tentative_retractions =
          msc.removeFromNodeList(msc.p_node.b_p().tentative_retractions);
    }

    return msc.inst;
  }

  public boolean hasNilGoalRetractions() {
    return !nil_goal_retractions.isEmpty();
  }

  /**
   * Retract an instantiation on the nil goal list. If there are no retractions on the nil goal
   * retraction list, return FALSE. This procedure is only called in Operand2 mode, so there is no
   * need for any checks for Operand2-specific processing.
   *
   * <p>rete.cpp:1293:get_next_nil_goal_retraction
   *
   * @return Retracted instantiation, or null if there are none.
   */
  public Instantiation get_next_nil_goal_retraction() {
    if (nil_goal_retractions.isEmpty()) return null;

    // Remove this retraction from the NIL goal list
    final MatchSetChange msc = nil_goal_retractions.pop();

    // next and prev set and used in Operand2 exactly as used in Soar 7 --
    // so we have to make sure and delete this retraction from the regular
    // list
    ms_retractions = msc.removeFromAllList(ms_retractions);

    if (msc.p_node != null) {
      msc.p_node.b_p().tentative_retractions =
          msc.removeFromNodeList(msc.p_node.b_p().tentative_retractions);
    }
    return msc.inst;
  }

  /**
   * returns TRUE iff there are any pending changes to the match set. This is used to test for
   * quiescence.
   *
   * <p>rete.cpp:1109:any_assertions_or_retractions_ready
   *
   * @return true if there are any pending changes to the match set
   */
  public boolean any_assertions_or_retractions_ready() {
    // Determining if assertions or retractions are ready require looping over
    // all goals in Waterfall/Operand2

    if (!nil_goal_retractions.isEmpty()) return true;

    // Loop from bottom to top because we expect activity at
    // the bottom usually

    for (IdentifierImpl goal = decider.bottom_goal;
        goal != null;
        goal = goal.goalInfo.higher_goal) {
      // if there are any assertions or retrctions for this goal,
      // return TRUE
      if (!goal.goalInfo.ms_o_assertions.isEmpty()
          || !goal.goalInfo.ms_i_assertions.isEmpty()
          || !goal.goalInfo.ms_retractions.isEmpty()) return true;
    }

    /* if there are no nil_goal_retractions and no assertions or retractions
    for any  goal then return FALSE -- there aren't any productions
    ready to fire or retract */

    return false;
  }

  private void print_whole_token(Printer printer, MatchSetChange msc, WmeTraceType wtt) {
    this.rete.print_whole_token(printer, msc.tok, wtt);
    Rete.print_whole_token_wme(printer, msc.w, wtt);
  }

  /** rete.cpp:7728:MS_trace */
  private static class MS_trace {
    final Symbol sym;
    final Symbol goal;
    int count;
    MS_trace next;

    public MS_trace(Symbol sym, Symbol goal, MS_trace next) {
      this.sym = sym;
      this.count = 1;
      this.goal = goal;
      this.next = next;
    }

    /**
     * rete.cpp:7747:in_ms_trace_same_goal
     *
     * @param sym
     * @param goal
     */
    public static MS_trace incrementOrCreate(MS_trace start, Symbol sym, Symbol goal) {
      for (MS_trace tmp = start; tmp != null; tmp = tmp.next) {
        if ((tmp.sym == sym) && (goal == tmp.goal)) {
          tmp.count++;
          return start;
        }
      }
      return new MS_trace(sym, goal, start);
    }
  }

  private void printAssertions(MatchSetChange assertions, Printer printer, WmeTraceType wtt) {
    MS_trace ms_trace = null;
    for (MatchSetChange msc = assertions; msc != null; msc = msc.next_of_all) {
      if (wtt != WmeTraceType.NONE) {
        printer.print("  %s [%s] ", msc.getProduction().getName(), msc.goal);
        print_whole_token(printer, msc, wtt);
        printer.print("\n");
      } else {
        ms_trace =
            MS_trace.incrementOrCreate(
                ms_trace,
                context
                    .getSymbols()
                    .createString(msc.getProduction().getName()), // TODO: This seems excessive
                msc.goal);
      }
    }

    if (wtt == WmeTraceType.NONE) {
      while (ms_trace != null) {
        final MS_trace tmp = ms_trace;
        ms_trace = tmp.next;
        //  BUG: for now this will print the goal of the first
        // assertion inspected, even though there can be multiple
        // assertions at different levels. See 2.110 in the OPERAND-CHANGE-LOG.
        printer.print("  %s [%s] ", tmp.sym, tmp.goal);
        if (tmp.count > 1) printer.print("(%d)", tmp.count);
        printer.print("\n");
      }
    }
  }

  private void printRetractions(Printer printer, WmeTraceType wtt) {
    MS_trace ms_trace = null;
    for (MatchSetChange msc = ms_retractions; msc != null; msc = msc.next_of_all) {
      if (wtt != WmeTraceType.NONE) {
        printer.print("  ");
        msc.inst.trace(printer.asFormatter(), wtt);
        printer.print("\n");
      } else {
        if (msc.inst.prod != null) {
          ms_trace =
              MS_trace.incrementOrCreate(
                  ms_trace,
                  context
                      .getSymbols()
                      .createString(msc.getProduction().getName()), // TODO: This seems excessive
                  msc.goal);
        }
      }
    }
    if (wtt == WmeTraceType.NONE) {
      while (ms_trace != null) {
        final MS_trace tmp = ms_trace;
        ms_trace = tmp.next;
        printer.print("  %s ", tmp.sym);
        //  BUG: for now this will print the goal of the first assertion
        //  inspected, even though there can be multiple assertions at
        //  different levels. See 2.110 in the OPERAND-CHANGE-LOG.
        if (tmp.goal != null) printer.print(" [%s] ", tmp.goal);
        else printer.print(" [NIL] ");
        if (tmp.count > 1) printer.print("(%d)", tmp.count);
        printer.print("\n");
      }
    }
  }

  /**
   * Print the current match set. Client code should use the method {@link
   * Agent#printMatchSet(Printer, WmeTraceType, EnumSet)}.
   *
   * <p>rete.cpp:7756:print_match_set
   *
   * @param printer
   * @param wtt
   * @param mst
   */
  public void print_match_set(Printer printer, WmeTraceType wtt, EnumSet<MatchSetTraceType> mst) {
    // Print assertions
    if (mst.contains(MatchSetTraceType.MS_ASSERT)) {
      printer.print("O Assertions:\n");
      printAssertions(ms_o_assertions, printer, wtt);
    }

    if (mst.contains(MatchSetTraceType.MS_ASSERT)) {
      printer.print("I Assertions:\n");
      printAssertions(ms_i_assertions, printer, wtt);
    }

    // Print retractions
    if (mst.contains(MatchSetTraceType.MS_RETRACT)) {
      printer.print("Retractions:\n");
      printRetractions(printer, wtt);
    }
  }

  private MatchSetEntry getAssertion(MatchSetChange msc, EntryType type) {
    final LinkedList<Wme> wmes = new LinkedList<Wme>();
    Token tok = msc.tok;
    while (tok != rete.dummy_top_token) {
      if (tok.w != null) {
        wmes.addFirst(tok.w);
      }
      tok = tok.parent;
    }
    if (msc.w != null) {
      wmes.add(msc.w);
    }

    return new DefaultMatchSetEntry(msc.getProduction(), type, wmes);
  }

  private List<MatchSetEntry> getAssertions(MatchSetChange assertions, EntryType type) {
    List<MatchSetEntry> entries = new ArrayList<MatchSetEntry>();
    for (MatchSetChange msc = assertions; msc != null; msc = msc.next_of_all) {
      entries.add(getAssertion(msc, type));
    }
    return entries;
  }

  private MatchSetEntry getRetraction(MatchSetChange msc) {
    return new DefaultMatchSetEntry(
        msc.getProduction(), EntryType.RETRACTION, msc.inst.getBacktraceWmes());
  }

  private List<MatchSetEntry> getRetractions() {
    List<MatchSetEntry> entries = new ArrayList<MatchSetEntry>();
    for (MatchSetChange msc = ms_retractions; msc != null; msc = msc.next_of_all) {
      entries.add(getRetraction(msc));
    }
    return entries;
  }

  public MatchSet getMatchSet() {
    final List<MatchSetEntry> entries = new ArrayList<MatchSetEntry>();
    entries.addAll(getAssertions(ms_i_assertions, EntryType.I_ASSERTION));
    entries.addAll(getAssertions(ms_o_assertions, EntryType.O_ASSERTION));
    entries.addAll(getRetractions());

    return new MatchSet() {

      @Override
      public List<MatchSetEntry> getEntries() {
        return Collections.unmodifiableList(entries);
      }
    };
  }

  /**
   * Increments the reference count of all WMEs that have been referenced this cycle.
   * wma.cpp:1082:wma_activate_wmes_tested_in_prods
   */
  public void wma_activate_wmes_tested_in_prods() {
    for (MatchSetChange msc = ms_o_assertions; msc != null; msc = msc.next_of_all) {
      Token t = Token.createMatchesToken(msc.tok, msc.w);

      while (t != rete.dummy_top_token) {
        if (t.w != null) {
          wma.wma_activate_wme(t.w);
        }

        t = t.parent;
      }
    }

    for (MatchSetChange msc = ms_i_assertions; msc != null; msc = msc.next_of_all) {
      Token t = Token.createMatchesToken(msc.tok, msc.w);

      while (t != rete.dummy_top_token) {
        if (t.w != null) {
          wma.wma_activate_wme(t.w);
        }

        t = t.parent;
      }
    }
  }
}

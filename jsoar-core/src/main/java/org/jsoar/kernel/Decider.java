/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 12, 2008
 */
package org.jsoar.kernel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import org.jsoar.kernel.epmem.EpisodicMemory;
import org.jsoar.kernel.events.GdsGoalRemovedEvent;
import org.jsoar.kernel.exploration.Exploration;
import org.jsoar.kernel.io.InputOutputImpl;
import org.jsoar.kernel.learning.Chunker;
import org.jsoar.kernel.learning.rl.ReinforcementLearning;
import org.jsoar.kernel.learning.rl.ReinforcementLearningInfo;
import org.jsoar.kernel.lhs.Condition;
import org.jsoar.kernel.lhs.PositiveCondition;
import org.jsoar.kernel.memory.Instantiation;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.PreferenceType;
import org.jsoar.kernel.memory.RecognitionMemory;
import org.jsoar.kernel.memory.Slot;
import org.jsoar.kernel.memory.TemporaryMemory;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.modules.SoarModule;
import org.jsoar.kernel.rete.MatchSetChange;
import org.jsoar.kernel.rete.SoarReteListener;
import org.jsoar.kernel.smem.SemanticMemory;
import org.jsoar.kernel.symbols.GoalIdentifierInfo;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.SymbolImpl;
import org.jsoar.kernel.tracing.Printer;
import org.jsoar.kernel.tracing.Trace;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.kernel.wma.WorkingMemoryActivation;
import org.jsoar.util.Arguments;
import org.jsoar.util.ByRef;
import org.jsoar.util.ListHead;
import org.jsoar.util.ListItem;
import org.jsoar.util.adaptables.Adaptables;
import org.jsoar.util.markers.DefaultMarker;
import org.jsoar.util.markers.Marker;
import org.jsoar.util.properties.BooleanPropertyProvider;

/**
 * <em>This is an internal interface. Don't use it unless you know what you're doing.</em>
 *
 * <p>decide.cpp
 *
 * @author ray
 */
public class Decider {
  /**
   * The decider often needs to mark symbols with certain flags, usually to record that the symbols
   * are in certain sets or have a certain status. The "common.decider_flag" field on symbols is
   * used for this, and is set to one of the following flag values. (Usually only two or three of
   * these values are used at once, and the meaning should be clear from the code.)
   *
   * <p>decide.cpp:120:DECIDER_FLAG
   */
  public enum DeciderFlag {
    /** decide.cpp:120:NOTHING_DECIDER_FLAG */
    NOTHING,

    /** decide.cpp:121:CANDIDATE_DECIDER_FLAG */
    CANDIDATE,

    /** decide.cpp:122:CONFLICTED_DECIDER_FLAG */
    CONFLICTED,

    /** decide.cpp:123:FORMER_CANDIDATE_DECIDER_FLAG */
    FORMER_CANDIDATE,

    /** decide.cpp:124:BEST_DECIDER_FLAG */
    BEST,

    /** decide.cpp:125:WORST_DECIDER_FLAG */
    WORST,

    /** decide.cpp:126:UNARY_INDIFFERENT_DECIDER_FLAG */
    UNARY_INDIFFERENT,

    /** decide.cpp:127:ALREADY_EXISTING_WME_DECIDER_FLAG */
    ALREADY_EXISTING_WME,

    /** decide.cpp:132:UNARY_INDIFFERENT_CONSTANT_DECIDER_FLAG */
    UNARY_INDIFFERENT_CONSTANT;

    /**
     * Helper to handle code that relies on NOTHING_DECIDER_FLAG being 0 in boolean contexts in C
     * (see above)
     *
     * @return true if this flag is not NOTHING
     */
    public boolean isSomething() {
      return this != NOTHING;
    }
  }

  /**
   * agent.h:62
   *
   * @author ray
   */
  public enum LinkUpdateType {
    UPDATE_LINKS_NORMALLY,
    UPDATE_DISCONNECTED_IDS_LIST,
    JUST_UPDATE_COUNT,
  }

  /** kernel.h:208:LOWEST_POSSIBLE_GOAL_LEVEL */
  private static final int LOWEST_POSSIBLE_GOAL_LEVEL = Integer.MAX_VALUE;

  /**
   * A dll of instantiations that will be used to determine the gds through a backtracing-style
   * procedure, evaluate_gds in decide.cpp
   *
   * <p>instantiations.h:106:pi_struct
   *
   * @author ray
   */
  private static class ParentInstantiation {
    ParentInstantiation next, prev;
    Instantiation inst;

    public String toString() {
      return inst != null ? inst.toString() : "null";
    }
  }

  private static final boolean DEBUG_GDS =
      Boolean.valueOf(System.getProperty("jsoar.gds.debug", "false"));
  private static final boolean DEBUG_GDS_HIGH = false;
  private static final boolean DEBUG_LINKS = false;

  private final Agent context;

  // These fields are all filled in initialize() through the agent with the
  // adaptable framework. See Agent.adaptables for more info
  private PredefinedSymbols predefinedSyms;
  private DecisionManipulation decisionManip;
  private Exploration exploration;
  private Chunker chunker;
  private InputOutputImpl io;
  private DecisionCycle decisionCycle;
  private WorkingMemory workingMemory;
  private TemporaryMemory tempMemory;
  private RecognitionMemory recMemory;
  private SoarReteListener soarReteListener;
  private ReinforcementLearning rl;
  private SemanticMemory smem;
  private EpisodicMemory epmem;
  private WorkingMemoryActivation wma;

  /**
   * gsysparam.h:164:MAX_GOAL_DEPTH
   *
   * <p>Defaults to 100 in init_soar()
   */
  private int MAX_GOAL_DEPTH = 100;

  /** agent.h:603:context_slots_with_changed_acceptable_preferences */
  private final ListHead<Slot> context_slots_with_changed_acceptable_preferences =
      ListHead.newInstance();
  /**
   * Note: In JSoar, changed to an array list, adding ids to end and traversing in reverse to
   * maintain same behavior as push-front conses...
   *
   * <p>agent.h:615:promoted_ids
   */
  private final List<IdentifierImpl> promoted_ids = new ArrayList<IdentifierImpl>();

  /** agent.h:616:link_update_mode */
  private LinkUpdateType link_update_mode = LinkUpdateType.UPDATE_LINKS_NORMALLY;
  /** agent.h:609:ids_with_unknown_level */
  private final ListHead<IdentifierImpl> ids_with_unknown_level = ListHead.newInstance();
  /** agent.h:607:disconnected_ids */
  private final ListHead<IdentifierImpl> disconnected_ids = ListHead.newInstance();

  private Marker mark_tc_number;
  private int level_at_which_marking_started;
  private int highest_level_anything_could_fall_from;
  private int lowest_level_anything_could_fall_to;
  private Marker walk_tc_number;
  private int walk_level;

  public IdentifierImpl top_goal;
  public IdentifierImpl bottom_goal;
  public IdentifierImpl top_state;
  public IdentifierImpl active_goal;
  IdentifierImpl previous_active_goal;
  public int active_level;
  int previous_active_level;

  // Used in new waterfall model inner preference loop
  /**
   * State for new waterfall model Represents the original active level of the elaboration cycle,
   * saved so that we can modify the active level during the inner preference loop and restore it
   * before working memory changes.
   */
  public int highest_active_level;
  /**
   * State for new waterfall model Same as highest_active_level, just the goal that the level
   * represents.
   */
  public IdentifierImpl highest_active_goal;
  /** State for new waterfall model Can't fire rules at this level or higher (lower int) */
  public int change_level;
  /**
   * State for new waterfall model Next change_level, in next iteration of inner preference loop.
   */
  public int next_change_level;

  /** agent.h:740:waitsnc */
  private BooleanPropertyProvider waitsnc = new BooleanPropertyProvider(SoarProperties.WAITSNC);

  /** agent.h:384:parent_list_head */
  private ParentInstantiation parent_list_head;

  /**
   * Construct a decider using the given agent. {@link #initialize()} <b>must</b> be called.
   *
   * @param context the owning agent
   */
  public Decider(Agent context) {
    Arguments.checkNotNull(context, "context");
    this.context = context;
  }

  public void initialize() {
    context.getProperties().setProvider(SoarProperties.WAITSNC, waitsnc);

    this.predefinedSyms = Adaptables.adapt(context, PredefinedSymbols.class);
    this.exploration = Adaptables.adapt(context, Exploration.class);
    this.decisionManip = Adaptables.adapt(context, DecisionManipulation.class);
    this.io = Adaptables.adapt(context, InputOutputImpl.class);
    this.decisionCycle = Adaptables.adapt(context, DecisionCycle.class);
    this.workingMemory = Adaptables.adapt(context, WorkingMemory.class);
    this.tempMemory = Adaptables.adapt(context, TemporaryMemory.class);
    this.recMemory = Adaptables.adapt(context, RecognitionMemory.class);
    this.soarReteListener = Adaptables.adapt(context, SoarReteListener.class);
    this.chunker = Adaptables.adapt(context, Chunker.class);
    this.rl = Adaptables.adapt(context, ReinforcementLearning.class);
    this.smem = Adaptables.require(getClass(), context, SemanticMemory.class);
    this.epmem = Adaptables.require(getClass(), context, EpisodicMemory.class);
    this.wma = Adaptables.require(getClass(), context, WorkingMemoryActivation.class);
  }

  public List<Goal> getGoalStack() {
    final List<Goal> result = new ArrayList<Goal>();
    for (IdentifierImpl g = top_goal; g != null; g = g.goalInfo.lower_goal) {
      final Goal goal = Adaptables.adapt(g, Goal.class);
      assert goal != null;
      result.add(goal);
    }
    return result;
  }

  /**
   * chunk.cpp:753:find_goal_at_goal_stack_level
   *
   * @param level
   * @return the goal at the given stack level
   */
  public IdentifierImpl find_goal_at_goal_stack_level(int level) {
    for (IdentifierImpl g = top_goal; g != null; g = g.goalInfo.lower_goal)
      if (g.level == level) return (g);
    return null;
  }

  /**
   * Whenever some acceptable or require preference for a context slot changes, we call
   * mark_context_slot_as_acceptable_preference_changed().
   *
   * <p>decide.cpp:146:mark_context_slot_as_acceptable_preference_changed
   *
   * @param s
   */
  public void mark_context_slot_as_acceptable_preference_changed(Slot s) {
    if (s.acceptable_preference_changed != null) return;

    ListItem<Slot> dc = new ListItem<Slot>(s);
    s.acceptable_preference_changed = dc;
    dc.insertAtHead(this.context_slots_with_changed_acceptable_preferences);
  }

  /**
   * This updates the acceptable preference wmes for a single slot.
   *
   * <p>decide.cpp:158:do_acceptable_preference_wme_changes_for_slot
   *
   * @param s
   */
  private void do_acceptable_preference_wme_changes_for_slot(Slot s) {
    // first, reset marks to "NOTHING"
    for (WmeImpl w = s.getAcceptablePreferenceWmes(); w != null; w = w.next)
      w.value.decider_flag = DeciderFlag.NOTHING;

    // now mark values for which we WANT a wme as "CANDIDATE" values
    for (Preference p = s.getPreferencesByType(PreferenceType.REQUIRE); p != null; p = p.next)
      p.value.decider_flag = DeciderFlag.CANDIDATE;
    for (Preference p = s.getPreferencesByType(PreferenceType.ACCEPTABLE); p != null; p = p.next)
      p.value.decider_flag = DeciderFlag.CANDIDATE;

    // remove any existing wme's that aren't CANDIDATEs; mark the rest as
    // ALREADY_EXISTING

    WmeImpl w = s.getAcceptablePreferenceWmes();
    while (w != null) {
      final WmeImpl next_w = w.next;
      if (w.value.decider_flag == DeciderFlag.CANDIDATE) {
        w.value.decider_flag = DeciderFlag.ALREADY_EXISTING_WME;
        w.value.decider_wme = w;
        w.preference = null; /* we'll update this later */
      } else {
        s.removeAcceptablePreferenceWme(w);

        /*
         * IF we lose an acceptable preference for an operator, then
         * that operator comes out of the slot immediately in OPERAND2.
         * However, if the lost acceptable preference is not for item in
         * the slot, then we don;t need to do anything special until
         * mini-quiescence.
         */
        remove_operator_if_necessary(s, w);

        this.workingMemory.remove_wme_from_wm(w);
      }
      w = next_w;
    }

    // add the necessary wme's that don't ALREADY_EXIST

    for (Preference p = s.getPreferencesByType(PreferenceType.REQUIRE); p != null; p = p.next) {
      if (p.value.decider_flag == DeciderFlag.ALREADY_EXISTING_WME) {
        // found existing wme, so just update its trace
        WmeImpl wme = p.value.decider_wme;
        if (wme.preference == null) wme.preference = p;
      } else {
        WmeImpl wme = this.workingMemory.make_wme(p.id, p.attr, p.value, true);
        s.addAcceptablePreferenceWme(wme);
        wme.preference = p;
        this.workingMemory.add_wme_to_wm(wme);
        p.value.decider_flag = DeciderFlag.ALREADY_EXISTING_WME;
        p.value.decider_wme = wme;
      }
    }

    for (Preference p = s.getPreferencesByType(PreferenceType.ACCEPTABLE); p != null; p = p.next) {
      if (p.value.decider_flag == DeciderFlag.ALREADY_EXISTING_WME) {
        // found existing wme, so just update its trace
        WmeImpl wme = p.value.decider_wme;
        if (wme.preference == null) wme.preference = p;
      } else {
        WmeImpl wme = this.workingMemory.make_wme(p.id, p.attr, p.value, true);
        s.addAcceptablePreferenceWme(wme);
        wme.preference = p;
        this.workingMemory.add_wme_to_wm(wme);
        p.value.decider_flag = DeciderFlag.ALREADY_EXISTING_WME;
        p.value.decider_wme = wme;
      }
    }
  }

  /**
   * Moved here from consistency since it accesses no other state and is only ever called from
   * decider.
   *
   * <p>consistency.cpp:41:remove_operator_if_necessary
   *
   * @param s
   * @param w
   */
  private void remove_operator_if_necessary(Slot s, WmeImpl w) {
    // #ifndef NO_TIMING_STUFF
    // #ifdef DETAILED_TIMING_STATS
    // start_timer(thisAgent, &thisAgent->start_gds_tv);
    // #endif
    // #endif

    // Note: Deleted about 40 lines of commented printf debugging code here from CSoar

    if (s.getWmes() != null) {
      // If there is something in the context slot
      if (s.getWmes().value == w.value) {
        // The WME in the context slot is WME whose pref changed
        context
            .getTrace()
            .print(
                Category.OPERAND2_REMOVALS,
                "\n        REMOVING: Operator from context slot (proposal no longer matches): %s",
                w);
        this.remove_wmes_for_context_slot(s);
        if (s.id.goalInfo.lower_goal != null) {
          context
              .getTrace()
              .print(
                  EnumSet.of(Category.VERBOSE, Category.WM_CHANGES),
                  "Removing state %s because of an operator removal.\n",
                  s.id.goalInfo.lower_goal);
          this.remove_existing_context_and_descendents(s.id.goalInfo.lower_goal);
        }
      }
    }

    // #ifndef NO_TIMING_STUFF
    // #ifdef DETAILED_TIMING_STATS
    //  stop_timer(thisAgent, &thisAgent->start_gds_tv,
    //             &thisAgent->gds_cpu_time[thisAgent->current_phase]);
    //  #endif
    //  #endif
  }

  /**
   * At the end of the phases, do_buffered_acceptable_preference_wme_changes() is called to update
   * the acceptable preference wmes. This should be called *before* do_buffered_link_changes() and
   * do_buffered_wm_changes().
   *
   * <p>decide.cpp:232:do_buffered_acceptable_preference_wme_changes
   */
  private void do_buffered_acceptable_preference_wme_changes() {
    while (!context_slots_with_changed_acceptable_preferences.isEmpty()) {
      Slot s = context_slots_with_changed_acceptable_preferences.pop();
      do_acceptable_preference_wme_changes_for_slot(s);
      s.acceptable_preference_changed = null;
    }
  }

  /**
   * Post a link addition for later processing.
   *
   * <p>decide.cpp:288:post_link_addition
   *
   * @param from
   * @param to
   */
  public void post_link_addition(IdentifierImpl from, IdentifierImpl to) {
    // don't add links to goals/impasses, except the special one (NIL,goal)
    if ((to.isGoal()) && from != null) return;

    to.link_count++;

    if (DEBUG_LINKS) {
      if (from != null) context.getPrinter().print("\nAdding link from %s to %s", from, to);
      else context.getPrinter().print("\nAdding special link to %s (count=%d)", to, to.link_count);
    }

    if (from == null) return; /* if adding a special link, we're done */

    // if adding link from same level, ignore it
    if (from.promotion_level == to.promotion_level) return;

    // if adding link from lower to higher, mark higher accordingly
    if (from.promotion_level > to.promotion_level) {
      to.could_be_a_link_from_below = true;
      return;
    }

    // otherwise buffer it for later
    to.promotion_level = from.promotion_level;
    this.promoted_ids.add(to); // not push (see decl comment)
  }

  /**
   * decide.cpp:329:promote_if_needed
   *
   * @param sym
   * @param new_level
   */
  private void promote_if_needed(SymbolImpl sym, int new_level) {
    IdentifierImpl id = sym.asIdentifier();
    if (id != null) promote_id_and_tc(id, new_level);
  }

  /**
   * Promote an id and its transitive closure.
   *
   * <p>decide.cpp:333:promote_id_and_tc
   *
   * @param id
   * @param new_level
   */
  private void promote_id_and_tc(IdentifierImpl id, /* goal_stack_level */ int new_level) {
    // if it's already that high, or is going to be soon, don't bother
    if (id.level <= new_level) return;
    if (id.promotion_level < new_level) return;

    // update its level, etc.
    id.level = new_level;
    id.promotion_level = new_level;
    id.could_be_a_link_from_below = true;

    // sanity check
    if (id.isGoal()) {
      throw new IllegalStateException("Internal error: tried to promote a goal or impasse id");
      /*
       * Note--since we can't promote a goal, we don't have to worry about
       * slot->acceptable_preference_wmes below
       */
    }

    // scan through all preferences and wmes for all slots for this id
    for (WmeImpl w = id.getInputWmes(); w != null; w = w.next)
      promote_if_needed(w.value, new_level);

    for (Slot s = id.slots; s != null; s = s.next) {
      for (Preference pref = s.getAllPreferences(); pref != null; pref = pref.nextOfSlot) {
        promote_if_needed(pref.value, new_level);
        if (pref.type.isBinary()) promote_if_needed(pref.referent, new_level);
      }
      for (WmeImpl w = s.getWmes(); w != null; w = w.next) promote_if_needed(w.value, new_level);
    }
  }

  /** decide.cpp:375:do_promotion */
  private void do_promotion() {
    while (!promoted_ids.isEmpty()) {
      IdentifierImpl to =
          promoted_ids.remove(promoted_ids.size() - 1); // pop off end (see decl comment)
      promote_id_and_tc(to, to.promotion_level);
    }
  }

  /**
   * Post a link removal for later processing
   *
   * <p>decide.cpp:424:post_link_removal
   *
   * @param from
   * @param to
   */
  public void post_link_removal(IdentifierImpl from, IdentifierImpl to) {
    // don't remove links to goals/impasses, except the special one
    // (NIL,goal)
    if ((to.isGoal()) && from != null) return;

    to.link_count--;

    if (DEBUG_LINKS) {
      if (from != null) {
        context
            .getPrinter()
            .print("\nRemoving link from %s to %s (%d to %d)", from, to, from.level, to.level);
      } else {
        context.getPrinter().print("\nRemoving special link to %s  (%d)", to, to.level);
      }
      context.getPrinter().print(" (count=%d)", to.link_count);
    }

    // if a gc is in progress, handle differently
    if (link_update_mode == LinkUpdateType.JUST_UPDATE_COUNT) return;

    if ((link_update_mode == LinkUpdateType.UPDATE_DISCONNECTED_IDS_LIST) && (to.link_count == 0)) {
      if (to.unknown_level != null) {
        ListItem<IdentifierImpl> dc = to.unknown_level;
        dc.remove(this.ids_with_unknown_level);
        dc.insertAtHead(this.disconnected_ids);
      } else {
        to.unknown_level = new ListItem<IdentifierImpl>(to);
        to.unknown_level.insertAtHead(this.disconnected_ids);
      }
      return;
    }

    // if removing a link from a different level, there must be some
    // other link at the same level, so we can ignore this change
    if (from != null && (from.level != to.level)) return;

    if (to.unknown_level == null) {
      to.unknown_level = new ListItem<IdentifierImpl>(to);
      to.unknown_level.insertAtHead(this.ids_with_unknown_level);
    }
  }

  /**
   * Garbage collect an identifier. This removes all wmes, input wmes, and preferences for that id
   * from TM.
   *
   * <p>decide.cpp:483:garbage_collect_id
   *
   * @param id
   */
  private void garbage_collect_id(IdentifierImpl id) {
    if (DEBUG_LINKS) {
      context.getPrinter().print("\n*** Garbage collecting id: %s", id);
    }

    id.unknown_level = null; // From CSoar revision r10938

    // Note--for goal/impasse id's, this does not remove the impasse wme's.
    // This is handled by remove_existing_such-and-such...

    // remove any input wmes from the id
    this.workingMemory.remove_wme_list_from_wm(id.getInputWmes(), true);
    id.removeAllInputWmes();

    for (Slot s = id.slots; s != null; s = s.next) {
      // remove all wme's from the slot
      this.workingMemory.remove_wme_list_from_wm(s.getWmes(), false);
      s.removeAllWmes();

      // remove all preferences for the slot
      Preference pref = s.getAllPreferences();
      while (pref != null) {
        final Preference next_pref = pref.nextOfSlot;
        recMemory.remove_preference_from_tm(pref);

        // Note: the call to remove_preference_from_slot handles the
        // removal of acceptable_preference_wmes
        pref = next_pref;
      }

      tempMemory.mark_slot_for_possible_removal(s);
    }
  }

  /**
   * decide.cpp:545:mark_level_unknown_needed
   *
   * @param sym
   */
  private boolean mark_level_unknown_needed(SymbolImpl sym) {
    return sym.asIdentifier() != null;
  }

  /**
   * Mark an id and its transitive closure as having an unknown level. Ids are marked by setting
   * id.tc_num to mark_tc_number. The starting id's goal stack level is recorded in
   * level_at_which_marking_started by the caller. The marked ids are added to
   * ids_with_unknown_level.
   *
   * <p>decide.cpp:550:mark_id_and_tc_as_unknown_level
   *
   * @param root
   */
  private void mark_id_and_tc_as_unknown_level(IdentifierImpl root) {
    final Deque<IdentifierImpl> ids_to_walk = new ArrayDeque<IdentifierImpl>();
    ids_to_walk.push(root);

    while (!ids_to_walk.isEmpty()) {
      IdentifierImpl id = ids_to_walk.pop();

      // if id is already marked, do nothing
      if (id.tc_number == this.mark_tc_number) continue;

      // don't mark anything higher up as disconnected--in order to be higher
      // up, it must have a link to it up there
      if (id.level < this.level_at_which_marking_started) continue;

      // mark id, so we won't do it again later
      id.tc_number = this.mark_tc_number;

      // update range of goal stack levels we'll need to walk
      if (id.level < this.highest_level_anything_could_fall_from)
        this.highest_level_anything_could_fall_from = id.level;
      if (id.level > this.lowest_level_anything_could_fall_to)
        this.lowest_level_anything_could_fall_to = id.level;
      if (id.could_be_a_link_from_below)
        this.lowest_level_anything_could_fall_to = LOWEST_POSSIBLE_GOAL_LEVEL;

      // add id to the set of ids with unknown level
      if (id.unknown_level == null) {
        id.unknown_level = new ListItem<IdentifierImpl>(id);
        id.unknown_level.insertAtHead(ids_with_unknown_level);
      }

      // scan through all preferences and wmes for all slots for this id
      for (WmeImpl w = id.getInputWmes(); w != null; w = w.next) {
        if (mark_level_unknown_needed(w.value)) {
          ids_to_walk.push(w.value.asIdentifier());
        }
      }

      for (Slot s = id.slots; s != null; s = s.next) {
        for (Preference pref = s.getAllPreferences(); pref != null; pref = pref.nextOfSlot) {
          if (mark_level_unknown_needed(pref.value)) {
            ids_to_walk.push(pref.value.asIdentifier());
          }
          if (pref.type.isBinary()) {
            if (mark_level_unknown_needed(pref.referent)) {
              ids_to_walk.push(pref.referent.asIdentifier());
            }
          }
        }
        if (s.impasse_id != null) {
          if (mark_level_unknown_needed(s.impasse_id)) {
            ids_to_walk.push(s.impasse_id.asIdentifier());
          }
        }
        for (WmeImpl w = s.getWmes(); w != null; w = w.next) {
          if (mark_level_unknown_needed(w.value)) {
            ids_to_walk.push(w.value.asIdentifier());
          }
        }
      } /* end of for slots loop */
    }
  }

  /**
   * decide.cpp:647:level_update_needed
   *
   * @param sym
   */
  private boolean level_update_needed(SymbolImpl sym) {
    IdentifierImpl id = sym.asIdentifier();
    return id != null && id.tc_number != this.walk_tc_number;
  }

  /**
   * After marking the ids with unknown level, we walk various levels of the goal stack, higher
   * level to lower level. If, while doing the walk, we encounter an id marked as having an unknown
   * level, we update its level and remove it from ids_with_unknown_level.
   *
   * <p>decide.cpp:652:walk_and_update_levels
   *
   * @param root
   */
  private void walk_and_update_levels(IdentifierImpl root) {
    Deque<IdentifierImpl> ids_to_walk = new ArrayDeque<IdentifierImpl>();
    ids_to_walk.push(root);

    while (!ids_to_walk.isEmpty()) {
      IdentifierImpl id = ids_to_walk.pop();

      // mark id so we don't walk it twice
      id.tc_number = this.walk_tc_number;

      // if we already know its level, and it's higher up, then exit
      if ((id.unknown_level == null) && (id.level < this.walk_level)) continue;

      // if we didn't know its level before, we do now
      if (id.unknown_level != null) {
        id.unknown_level.remove(this.ids_with_unknown_level);
        id.unknown_level = null;
        id.level = this.walk_level;
        id.promotion_level = this.walk_level;
      }

      // scan through all preferences and wmes for all slots for this id
      for (WmeImpl w = id.getInputWmes(); w != null; w = w.next) {
        if (level_update_needed(w.value)) {
          ids_to_walk.push(w.value.asIdentifier());
        }
      }
      for (Slot s = id.slots; s != null; s = s.next) {
        for (Preference pref = s.getAllPreferences(); pref != null; pref = pref.nextOfSlot) {
          if (level_update_needed(pref.value)) {
            ids_to_walk.push(pref.value.asIdentifier());
          }
          if (pref.type.isBinary()) {
            if (level_update_needed(pref.referent)) {
              ids_to_walk.push(pref.referent.asIdentifier());
            }
          }
        }
        if (s.impasse_id != null) {
          if (level_update_needed(s.impasse_id)) {
            ids_to_walk.push(s.impasse_id.asIdentifier());
          }
        }
        for (WmeImpl w = s.getWmes(); w != null; w = w.next) {
          if (level_update_needed(w.value)) {
            ids_to_walk.push(w.value.asIdentifier());
          }
        }
      } /* end of for slots loop */
    }
  }

  /**
   * Do all buffered demotions and gc's.
   *
   * <p>decide.cpp:666:do_demotion
   */
  private void do_demotion() {
    // scan through ids_with_unknown_level, move the ones with link_count==0
    // over to disconnected_ids
    ListItem<IdentifierImpl> dc, next_dc;
    for (dc = ids_with_unknown_level.first; dc != null; dc = next_dc) {
      next_dc = dc.next;
      final IdentifierImpl id = dc.item;
      if (id.link_count == 0) {
        dc.remove(this.ids_with_unknown_level);
        dc.insertAtHead(this.disconnected_ids);
      }
    }

    // keep garbage collecting ids until nothing left to gc
    this.link_update_mode = LinkUpdateType.UPDATE_DISCONNECTED_IDS_LIST;
    while (!this.disconnected_ids.isEmpty()) {
      final IdentifierImpl id = disconnected_ids.pop();
      garbage_collect_id(id);
    }
    this.link_update_mode = LinkUpdateType.UPDATE_LINKS_NORMALLY;

    // if nothing's left with an unknown level, we're done
    if (this.ids_with_unknown_level.isEmpty()) return;

    // do the mark
    this.highest_level_anything_could_fall_from = LOWEST_POSSIBLE_GOAL_LEVEL;
    this.lowest_level_anything_could_fall_to = -1;
    this.mark_tc_number = DefaultMarker.create();
    for (dc = this.ids_with_unknown_level.first; dc != null; dc = dc.next) {
      final IdentifierImpl id = dc.item;
      this.level_at_which_marking_started = id.level;
      mark_id_and_tc_as_unknown_level(id);
    }

    // do the walk
    IdentifierImpl g = this.top_goal;
    while (true) {
      if (g == null) break;
      if (g.level > this.lowest_level_anything_could_fall_to) break;
      if (g.level >= this.highest_level_anything_could_fall_from) {
        this.walk_level = g.level;
        this.walk_tc_number = DefaultMarker.create();
        walk_and_update_levels(g);
      }
      g = g.goalInfo.lower_goal;
    }

    // GC anything left with an unknown level after the walk
    this.link_update_mode = LinkUpdateType.JUST_UPDATE_COUNT;
    while (!ids_with_unknown_level.isEmpty()) {
      final IdentifierImpl id = ids_with_unknown_level.pop();
      garbage_collect_id(id);
    }
    this.link_update_mode = LinkUpdateType.UPDATE_LINKS_NORMALLY;
  }

  /**
   * This routine does all the buffered link (ownership) changes, updating the goal stack level on
   * all identifiers and garbage collecting disconnected wmes.
   *
   * <p>decide.cpp:744:do_buffered_link_changes
   */
  private void do_buffered_link_changes() {
    // #ifndef NO_TIMING_STUFF
    // #ifdef DETAILED_TIMING_STATS
    // struct timeval saved_start_tv;
    // #endif
    // #endif

    // if no promotions or demotions are buffered, do nothing
    if (promoted_ids.isEmpty() && ids_with_unknown_level.isEmpty() && disconnected_ids.isEmpty())
      return;

    // #ifndef NO_TIMING_STUFF
    // #ifdef DETAILED_TIMING_STATS
    // start_timer (thisAgent, &saved_start_tv);
    // #endif
    // #endif
    do_promotion();
    do_demotion();
    // #ifndef NO_TIMING_STUFF
    // #ifdef DETAILED_TIMING_STATS
    //  stop_timer (thisAgent, &saved_start_tv,
    // &thisAgent->ownership_cpu_time[thisAgent->current_phase]);
    // #endif
    // #endif
  }

  /** Perform reinforcement learning update for one valid candidate. */
  void rl_update_for_one_candidate(Slot s, boolean consistency, Preference candidates) {
    if (!consistency && this.rl.rl_enabled()) {
      rl.rl_tabulate_reward_values();
      exploration.exploration_compute_value_of_candidate(candidates, s, 0);
      rl.rl_perform_update(candidates.numeric_value, candidates.rl_contribution, s.id);
    }
  }

  /**
   * Examines the preferences for a given slot, and returns an impasse type for the slot. The
   * argument "result_candidates" is set to a list of candidate values for the slot--if the returned
   * impasse type is NONE_IMPASSE_TYPE, this is the set of winners; otherwise it is the set of tied,
   * conflicted, or constraint-failure values. This list of values is a list of preferences for
   * those values, linked via the "next_candidate" field on each preference structure. If there is
   * more than one preference for a given value, only one is returned in the result_candidates, with
   * (first) require preferences being preferred over acceptable preferences, and (second)
   * preferences from higher match goals being preferred over those from lower match goals.
   *
   * <p>BUGBUG There is a problem here: since the require/acceptable priority takes precedence over
   * the match goal level priority, it's possible that we could return a require preference from
   * lower in the goal stack than some acceptable preference. If the goal stack gets popped soon
   * afterwards (i.e., before the next time the slot is re-decided, I think), we would be left with
   * a WME still in WM (not GC'd, because of the acceptable preference higher up) but with a trace
   * pointing to a deallocated require preference. This case is very obscure and unlikely to come
   * up, but it could easily cause a core dump or worse.
   *
   * <p>decide.cpp:840:run_preference_semantics
   */
  public ImpasseType run_preference_semantics(Slot s, ByRef<Preference> result_candidates) {
    return run_preference_semantics(s, result_candidates, false, false);
  }

  public ImpasseType run_preference_semantics(
      Slot s, ByRef<Preference> result_candidates, boolean consistency /* = false */) {
    return run_preference_semantics(s, result_candidates, consistency, false);
  }

  public ImpasseType run_preference_semantics(
      Slot s,
      ByRef<Preference> result_candidates,
      boolean consistency /* = false */,
      boolean predict /* = false */) {
    /* Set a flag to determine if a context-dependent preference set makes sense in this context.
     * We can ignore the CDPS when:
     * - Run_preference_semantics is called for a consistency check (don't want side effects)
     * - For non-context slots (only makes sense for operators)
     * - For context-slots at the top level (will never be backtraced through)
     * - when the learning system parameter is set off (note, this is independent of whether learning is on) */

    boolean do_CDPS =
        (s.isa_context_slot
            && !consistency
            && (s.id.level > SoarConstants.TOP_GOAL_LEVEL)
            && chunker.chunkThroughEvaluationRules);

    /* Empty the context-dependent preference set in the slot */

    if (do_CDPS && s.hasContextDependentPreferenceSet()) {
      s.clear_CDPS(context);
    }

    // if the slot has no preferences at all, things are trivial
    if (s.getAllPreferences() == null) {
      if (!s.isa_context_slot) tempMemory.mark_slot_for_possible_removal(s);
      result_candidates.value = null;
      return ImpasseType.NONE;
    }

    // If this is the true decision slot and selection has been made, attempt force selection

    if (!s.isa_context_slot && !consistency) {
      if (decisionManip.select_get_operator() != null) {
        final Preference force_result =
            decisionManip.select_force(s.getPreferencesByType(PreferenceType.ACCEPTABLE), !predict);

        if (force_result != null) {
          force_result.next_candidate = null;
          result_candidates.value = force_result;

          if (!predict && rl.rl_enabled()) {
            rl.rl_tabulate_reward_values();
            exploration.exploration_compute_value_of_candidate(force_result, s, 0);
            rl.rl_perform_update(force_result.numeric_value, force_result.rl_contribution, s.id);
          }

          return ImpasseType.NONE;
        } else {
          context.getPrinter().warn("WARNING: Invalid forced selection operator id");
        }
      }
    }

    /* If debugging a context-slot, print all preferences that we're deciding through */

    final Trace trace = context.getTrace();
    final Printer printer = trace.getPrinter();
    final boolean traceBacktracing = trace.isEnabled(Category.BACKTRACING);

    if (traceBacktracing && s.isa_context_slot) {

      printer.print(
          "\n-------------------------------\nRUNNING PREFERENCE SEMANTICS...\n-------------------------------\n");
      printer.print("All Preferences for slot:");

      for (PreferenceType type : PreferenceType.values()) {
        Preference pref = s.getPreferencesByType(type);

        if (pref != null) {
          printer.print("\n %ss:\n", type.getDisplayName());
          for (Preference p = pref; p != null; p = p.next) {
            printer.print(" ");
            printer.print(p.toString());
          }
        }
      }
      printer.print("-------------------------------\n");
    }

    /* === Requires === */

    if (s.getPreferencesByType(PreferenceType.REQUIRE) != null) {

      // Collect set of required items into candidates list

      for (Preference p = s.getPreferencesByType(PreferenceType.REQUIRE); p != null; p = p.next)
        p.value.decider_flag = DeciderFlag.NOTHING;

      Preference candidates = null;
      for (Preference p = s.getPreferencesByType(PreferenceType.REQUIRE); p != null; p = p.next) {
        if (p.value.decider_flag == DeciderFlag.NOTHING) {
          p.next_candidate = candidates;
          candidates = p;
          // unmark it, in order to prevent it from being added twice
          p.value.decider_flag = DeciderFlag.CANDIDATE;
        }
      }
      result_candidates.value = candidates;

      // Check if we have more than one required item. If so, return constraint failure.

      if (candidates.next_candidate != null) return ImpasseType.CONSTRAINT_FAILURE;

      /*
       * Check if we have also have a prohibit preference. If so, return
       * constraint failure. Note that this is the one difference between
       * prohibit and reject preferences.
       */

      SymbolImpl value = candidates.value;
      for (Preference p = s.getPreferencesByType(PreferenceType.PROHIBIT); p != null; p = p.next)
        if (p.value == value) return ImpasseType.CONSTRAINT_FAILURE;

      // --- We have a winner, so update RL ---

      rl_update_for_one_candidate(s, consistency, candidates);

      /* Print a message that we're adding the require preference to the CDPS
       * even though we really aren't. Requires aren't actually handled by
       * the CDPS mechanism since they are already backtraced through. */

      if (traceBacktracing) {
        printer.print("--> Adding preference to CDPS: ");
        printer.print(candidates.toString());
      }

      return ImpasseType.NONE;
    }

    /* === Acceptables, Prohibits, Rejects === */

    // Mark every acceptable preference as a possible candidate

    for (Preference p = s.getPreferencesByType(PreferenceType.ACCEPTABLE); p != null; p = p.next)
      p.value.decider_flag = DeciderFlag.CANDIDATE;

    /* Unmark any preferences that have a prohibit or reject. Note that this may
     * remove the candidate_decider_flag set in the last loop
     */
    for (Preference p = s.getPreferencesByType(PreferenceType.PROHIBIT); p != null; p = p.next)
      p.value.decider_flag = DeciderFlag.NOTHING;
    for (Preference p = s.getPreferencesByType(PreferenceType.REJECT); p != null; p = p.next)
      p.value.decider_flag = DeciderFlag.NOTHING;

    /* Build list of candidates. These are the acceptable prefs that didn't
     * have the CANDIDATE_DECIDER_FLAG reversed by prohibit or reject prefs.
     */
    Preference candidates = null;
    for (Preference p = s.getPreferencesByType(PreferenceType.ACCEPTABLE); p != null; p = p.next) {
      if (p.value.decider_flag == DeciderFlag.CANDIDATE) {
        p.next_candidate = candidates;
        candidates = p;
        // unmark it, in order to prevent it from being added twice
        p.value.decider_flag = DeciderFlag.NOTHING;
      }
    }

    /* If this is not a decidable context slot, then we're done */
    if (!s.isa_context_slot) {
      result_candidates.value = candidates;
      return ImpasseType.NONE;
    }

    /* If there are reject or prohibit preferences, then
     * add all reject and prohibit preferences to CDPS */

    if (do_CDPS) {
      if (s.getPreferencesByType(PreferenceType.PROHIBIT) != null
          || s.getPreferencesByType(PreferenceType.REJECT) != null) {
        for (Preference p = s.getPreferencesByType(PreferenceType.PROHIBIT); p != null; p = p.next)
          s.add_to_CDPS(context, p);
        for (Preference p = s.getPreferencesByType(PreferenceType.REJECT); p != null; p = p.next)
          s.add_to_CDPS(context, p);
      }
    }

    /* Exit point 1: Check if we're done, i.e. 0 or 1 candidates left */
    if ((candidates == null) || (candidates.next_candidate == null)) {
      result_candidates.value = candidates;

      if (candidates != null) {
        // Update RL values for the winning candidate
        rl_update_for_one_candidate(s, consistency, candidates);
      } else {
        if (do_CDPS && s.hasContextDependentPreferenceSet()) {
          s.clear_CDPS(context);
        }
      }

      return ImpasseType.NONE;
    }

    /* === Better/Worse === */

    if (s.getPreferencesByType(PreferenceType.BETTER) != null
        || s.getPreferencesByType(PreferenceType.WORSE) != null) {

      // Initialize decider flags

      for (Preference p = s.getPreferencesByType(PreferenceType.BETTER); p != null; p = p.next) {
        p.value.decider_flag = DeciderFlag.NOTHING;
        p.referent.decider_flag = DeciderFlag.NOTHING;
      }
      for (Preference p = s.getPreferencesByType(PreferenceType.WORSE); p != null; p = p.next) {
        p.value.decider_flag = DeciderFlag.NOTHING;
        p.referent.decider_flag = DeciderFlag.NOTHING;
      }
      for (Preference cand = candidates; cand != null; cand = cand.next_candidate) {
        cand.value.decider_flag = DeciderFlag.CANDIDATE;
      }

      /*
       * Mark any preferences that are worse than another as conflicted.
       * This will either remove it from the candidate list or add it to
       * the conflicted list later. We first do this for both the referent
       * half of better and then the value half of worse preferences.
       */
      for (Preference p = s.getPreferencesByType(PreferenceType.BETTER); p != null; p = p.next) {
        final SymbolImpl j = p.value;
        final SymbolImpl k = p.referent;
        if (j == k) continue;
        if (j.decider_flag.isSomething() && k.decider_flag.isSomething()) {
          if (j.decider_flag == DeciderFlag.CANDIDATE || k.decider_flag == DeciderFlag.CANDIDATE) {
            k.decider_flag = DeciderFlag.CONFLICTED;
          }
        }
      }

      for (Preference p = s.getPreferencesByType(PreferenceType.WORSE); p != null; p = p.next) {
        final SymbolImpl j = p.value;
        final SymbolImpl k = p.referent;
        if (j == k) continue;
        if (j.decider_flag.isSomething() && k.decider_flag.isSomething()) {
          if (j.decider_flag == DeciderFlag.CANDIDATE || k.decider_flag == DeciderFlag.CANDIDATE) {
            j.decider_flag = DeciderFlag.CONFLICTED;
          }
        }
      }

      // Check if a valid candidate still exists.

      Preference cand = null;
      for (cand = candidates; cand != null; cand = cand.next_candidate) {
        if (cand.value.decider_flag == DeciderFlag.CANDIDATE) break;
      }

      /* If no candidates exists, collect conflicted candidates and return as
       * the result candidates with a conflict impasse type. */

      Preference prev_cand = null;
      if (cand == null) {
        // collect conflicted candidates into new candidates list
        prev_cand = null;
        cand = candidates;
        while (cand != null) {
          if (cand.value.decider_flag != DeciderFlag.CONFLICTED) {
            if (prev_cand != null) prev_cand.next_candidate = cand.next_candidate;
            else candidates = cand.next_candidate;
          } else {
            prev_cand = cand;
          }
          cand = cand.next_candidate;
        }
        result_candidates.value = candidates;
        if (do_CDPS && s.hasContextDependentPreferenceSet()) {
          s.clear_CDPS(context);
        }
        return ImpasseType.CONFLICT;
      }

      /*
       * Otherwise, delete conflicted candidates from candidate list. Also
       * add better preferences to CDPS for every item in the candidate
       * list and delete acceptable preferences from the CDPS for those
       * that don't make the candidate list.
       */

      prev_cand = null;
      cand = candidates;
      while (cand != null) {
        if (cand.value.decider_flag == DeciderFlag.CONFLICTED) {
          // Remove this preference from the candidate list
          if (prev_cand != null) prev_cand.next_candidate = cand.next_candidate;
          else candidates = cand.next_candidate;
        } else {
          if (do_CDPS) {
            /* Add better/worse preferences to CDPS */
            for (Preference p = s.getPreferencesByType(PreferenceType.BETTER);
                p != null;
                p = p.next) {
              if (p.value == cand.value) {
                s.add_to_CDPS(context, p);
              }
            }
            for (Preference p = s.getPreferencesByType(PreferenceType.WORSE);
                p != null;
                p = p.next) {
              if (p.referent == cand.value) {
                s.add_to_CDPS(context, p);
              }
            }
          }
          prev_cand = cand;
        }
        cand = cand.next_candidate;
      }
    }

    // Exit point 2: Check if we're done, i.e. 0 or 1 candidates left

    if ((candidates == null) || (candidates.next_candidate == null)) {
      result_candidates.value = candidates;

      if (candidates != null) {
        // Update RL values for the winning candidate
        rl_update_for_one_candidate(s, consistency, candidates);
      } else {
        if (do_CDPS && s.hasContextDependentPreferenceSet()) {
          s.clear_CDPS(context);
        }
      }

      return ImpasseType.NONE;
    }

    /* === Bests === */
    if (s.getPreferencesByType(PreferenceType.BEST) != null) {
      // Initialize decider flags for all candidates
      Preference cand, prev_cand;
      for (cand = candidates; cand != null; cand = cand.next_candidate)
        cand.value.decider_flag = DeciderFlag.NOTHING;

      // Mark flag for those with a best preference
      for (Preference p = s.getPreferencesByType(PreferenceType.BEST); p != null; p = p.next) {
        p.value.decider_flag = DeciderFlag.BEST;
      }

      // Reduce candidates list to only those with best preference flag and add pref to CDPS
      prev_cand = null;
      for (cand = candidates; cand != null; cand = cand.next_candidate)
        if (cand.value.decider_flag == DeciderFlag.BEST) {
          if (do_CDPS) {
            for (Preference p = s.getPreferencesByType(PreferenceType.BEST);
                p != null;
                p = p.next) {
              if (p.value == cand.value) {
                s.add_to_CDPS(context, p);
              }
            }
          }
          if (prev_cand != null) prev_cand.next_candidate = cand;
          else candidates = cand;
          prev_cand = cand;
        }
      if (prev_cand != null) prev_cand.next_candidate = null;
    }

    /* Exit point 3: Check if we're done, i.e. 0 or 1 candidates left */

    if ((candidates == null) || (candidates.next_candidate == null)) {
      result_candidates.value = candidates;

      if (candidates != null) {
        // Update RL values for the winning candidate
        rl_update_for_one_candidate(s, consistency, candidates);
      } else {
        if (do_CDPS && s.hasContextDependentPreferenceSet()) {
          s.clear_CDPS(context);
        }
      }

      return ImpasseType.NONE;
    }

    /* === Worsts === */
    if (s.getPreferencesByType(PreferenceType.WORST) != null) {
      // Initialize decider flags for all candidates
      Preference cand, prev_cand;
      for (cand = candidates; cand != null; cand = cand.next_candidate)
        cand.value.decider_flag = DeciderFlag.NOTHING;

      // Mark flag for those with a worst preference
      for (Preference p = s.getPreferencesByType(PreferenceType.WORST); p != null; p = p.next)
        p.value.decider_flag = DeciderFlag.WORST;

      /*
       * Because we only want to add worst preferences to the CDPS if they
       * actually have an impact on the candidate list, we must first see
       * if there's at least one non-worst candidate.
       */

      boolean some_not_worst = false;
      if (do_CDPS) {
        for (cand = candidates; cand != null; cand = cand.next_candidate) {
          if (cand.value.decider_flag != DeciderFlag.WORST) {
            some_not_worst = true;
          }
        }
      }

      prev_cand = null;
      for (cand = candidates; cand != null; cand = cand.next_candidate) {
        if (cand.value.decider_flag != DeciderFlag.WORST) {
          if (prev_cand != null) prev_cand.next_candidate = cand;
          else candidates = cand;
          prev_cand = cand;
        } else {
          if (do_CDPS && some_not_worst) {
            /* Add this worst preference to CDPS */
            for (Preference p = s.getPreferencesByType(PreferenceType.WORST);
                p != null;
                p = p.next) {
              if (p.value == cand.value) {
                s.add_to_CDPS(context, p);
              }
            }
          }
        }
      }
      if (prev_cand != null) prev_cand.next_candidate = null;
    }

    /* Exit point 4: Check if we're done, i.e. 0 or 1 candidates left */
    if ((candidates == null) || (candidates.next_candidate == null)) {
      result_candidates.value = candidates;

      if (candidates != null) {
        // Update RL values for the winning candidate
        rl_update_for_one_candidate(s, consistency, candidates);
      } else {
        if (do_CDPS && s.hasContextDependentPreferenceSet()) {
          s.clear_CDPS(context);
        }
      }

      return ImpasseType.NONE;
    }

    /* === Indifferents === */

    // Initialize decider flags for all candidates

    for (Preference cand = candidates; cand != null; cand = cand.next_candidate)
      cand.value.decider_flag = DeciderFlag.NOTHING;

    // Mark flag for unary or numeric indifferent preferences

    for (Preference p = s.getPreferencesByType(PreferenceType.UNARY_INDIFFERENT);
        p != null;
        p = p.next) p.value.decider_flag = DeciderFlag.UNARY_INDIFFERENT;

    for (Preference p = s.getPreferencesByType(PreferenceType.NUMERIC_INDIFFERENT);
        p != null;
        p = p.next) p.value.decider_flag = DeciderFlag.UNARY_INDIFFERENT_CONSTANT;

    /*
     * Go through candidate list and check for a tie impasse. All candidates
     * must either be unary indifferent or binary indifferent to every item
     * on the candidate list. This will also catch when a candidate has no
     * indifferent preferences at all.
     */

    boolean not_all_indifferent = false;
    boolean some_numeric = false;

    for (Preference cand = candidates; cand != null; cand = cand.next_candidate) {
      /*
       * If this candidate has a unary indifferent preference, skip.
       * Numeric indifferent prefs are considered to have an implicit
       * unary indifferent pref, which is why they are skipped too.
       */
      if (cand.value.decider_flag == DeciderFlag.UNARY_INDIFFERENT) continue;
      else if (cand.value.decider_flag == DeciderFlag.UNARY_INDIFFERENT_CONSTANT) {
        some_numeric = true;
        continue;
      }

      /*
       * Candidate has either only binary indifferences or no indifference
       * prefs at all, so make sure there is a binary preference between
       * its operator and every other preference's operator in the
       * candidate list
       */

      for (Preference p = candidates; p != null; p = p.next_candidate) {
        if (p == cand) continue;
        boolean match_found = false;
        for (Preference p2 = s.getPreferencesByType(PreferenceType.BINARY_INDIFFERENT);
            p2 != null;
            p2 = p2.next) {
          if (((p2.value == cand.value) && (p2.referent == p.value))
              || ((p2.value == p.value) && (p2.referent == cand.value))) {
            match_found = true;
            break;
          }
        }
        if (!match_found) {
          not_all_indifferent = true;
          break;
        }
      } /* end of for p loop */
      if (not_all_indifferent) break;
    } /* end of for cand loop */

    if (!not_all_indifferent) {
      if (!consistency) {
        result_candidates.value = exploration.exploration_choose_according_to_policy(s, candidates);
        result_candidates.value.next_candidate = null;

        if (do_CDPS) {

          /*
           * Add all indifferent preferences associated with the
           * chosen candidate to the CDPS.
           */

          if (some_numeric) {

            /*
             * Note that numeric indifferent preferences are never
             * considered duplicates, so we pass an extra argument
             * to add_to_cdps so that it does not check for
             * duplicates.
             */

            for (Preference p = s.getPreferencesByType(PreferenceType.NUMERIC_INDIFFERENT);
                p != null;
                p = p.next) {
              if (p.value == result_candidates.value.value) {
                s.add_to_CDPS(context, p, false);
              }
            }

            /*
             * Now add any binary preferences with a candidate that
             * does NOT have a numeric preference.
             */

            for (Preference p = s.getPreferencesByType(PreferenceType.BINARY_INDIFFERENT);
                p != null;
                p = p.next) {
              if ((p.value == result_candidates.value.value)
                  || (p.referent == result_candidates.value.value)) {
                if ((p.referent.decider_flag != DeciderFlag.UNARY_INDIFFERENT_CONSTANT)
                    || (p.value.decider_flag != DeciderFlag.UNARY_INDIFFERENT_CONSTANT)) {
                  s.add_to_CDPS(context, p);
                }
              }
            }
          } else {
            /* This decision was non-numeric, so add all non-numeric preferences associated with the
             * chosen candidate to the CDPS.*/

            for (Preference p = s.getPreferencesByType(PreferenceType.UNARY_INDIFFERENT);
                p != null;
                p = p.next) {
              if (p.value == result_candidates.value.value) {
                s.add_to_CDPS(context, p);
              }
            }
            for (Preference p = s.getPreferencesByType(PreferenceType.BINARY_INDIFFERENT);
                p != null;
                p = p.next) {
              if ((p.value == result_candidates.value.value)
                  || (p.referent == result_candidates.value.value)) {
                s.add_to_CDPS(context, p);
              }
            }
          }
        }
      } else {
        result_candidates.value = candidates;
      }

      return ImpasseType.NONE;
    }

    // Candidates are not all indifferent, so we have a tie.
    result_candidates.value = candidates;
    if (do_CDPS && s.hasContextDependentPreferenceSet()) {
      s.clear_CDPS(context);
    }
    return ImpasseType.TIE;
  }

  /**
   * This creates a new wme and adds it to the given impasse object. "Id" indicates the goal/impasse
   * id; (id ^attr value) is the impasse wme to be added. The "preference" argument indicates the
   * preference (if non-NIL) for backtracing.
   *
   * <p>decide.cpp:1224:add_impasse_wme
   *
   * @param id
   * @param attr
   * @param value
   * @param p
   */
  private void add_impasse_wme(IdentifierImpl id, SymbolImpl attr, SymbolImpl value, Preference p) {
    WmeImpl w = this.workingMemory.make_wme(id, attr, value, false);
    id.goalInfo.addImpasseWme(w);
    w.preference = p;
    this.workingMemory.add_wme_to_wm(w);
  }

  /**
   * This creates a new impasse, returning its identifier. The caller is responsible for filling in
   * either {@code id->isa_impasse} or {@code id->isa_goal}, and all the extra stuff for goal
   * identifiers.
   *
   * <p>decide.cpp:1241:create_new_impasse
   *
   * @param level Goal stack level
   */
  private IdentifierImpl create_new_impasse(
      SymbolImpl object, SymbolImpl attr, ImpasseType impasse_type, int level) {
    final PredefinedSymbols predefined = predefinedSyms; // reduce typing

    final IdentifierImpl id = predefined.getSyms().make_new_identifier('S', level);
    post_link_addition(null, id); // add the special link

    id.goalInfo = new GoalIdentifierInfo(id);

    add_impasse_wme(id, predefined.type_symbol, predefined.state_symbol, null);
    add_impasse_wme(id, predefined.superstate_symbol, object, null);

    if (attr != null) {
      add_impasse_wme(id, predefined.attribute_symbol, attr, null);
    }

    switch (impasse_type) {
      case NONE: // this happens only when creating the top goal
        break;
      case CONSTRAINT_FAILURE:
        add_impasse_wme(id, predefined.impasse_symbol, predefined.constraint_failure_symbol, null);
        add_impasse_wme(id, predefined.choices_symbol, predefined.none_symbol, null);
        break;
      case CONFLICT:
        add_impasse_wme(id, predefined.impasse_symbol, predefined.conflict_symbol, null);
        add_impasse_wme(id, predefined.choices_symbol, predefined.multiple_symbol, null);
        break;
      case TIE:
        add_impasse_wme(id, predefined.impasse_symbol, predefined.tie_symbol, null);
        add_impasse_wme(id, predefined.choices_symbol, predefined.multiple_symbol, null);
        break;
      case NO_CHANGE:
        add_impasse_wme(id, predefined.impasse_symbol, predefined.no_change_symbol, null);
        add_impasse_wme(id, predefined.choices_symbol, predefined.none_symbol, null);
        break;
      default:
        // do nothing
        break;
    }

    id.goalInfo.allow_bottom_up_chunks = true;
    id.goalInfo.operator_slot =
        Slot.make_slot(id, predefinedSyms.operator_symbol, predefinedSyms.operator_symbol);

    // Create RL link
    id.goalInfo.reward_header = predefined.getSyms().make_new_identifier('R', level);
    SoarModule.add_module_wme(
        workingMemory, id, predefined.rl_sym_reward_link, id.goalInfo.reward_header);

    // Create EPMEM stuff
    epmem.initializeNewContext(workingMemory, id);

    // Create SMEM stuff
    smem.initializeNewContext(workingMemory, id);

    return id;
  }

  /**
   * Fake Preferences for Goal ^Item Augmentations
   *
   * <p>When we backtrace through a (goal ^item) augmentation, we want to backtrace to the
   * acceptable preference wme in the supercontext corresponding to that ^item. A slick way to do
   * this automagically is to set the backtracing preference pointer on the (goal ^item) wme to be a
   * "fake" preference for a "fake" instantiation. The instantiation has as its LHS a list of one
   * condition, which matched the acceptable preference wme in the supercontext.
   *
   * <p>Make_fake_preference_for_goal_item() builds such a fake preference and instantiation, given
   * a pointer to the supergoal and the acceptable/require preference for the value, and returns a
   * pointer to the fake preference. *** for Soar 8.3, we changed the fake preference to be
   * ACCEPTABLE instead of REQUIRE. This could potentially break some code, but it avoids the BUGBUG
   * condition that can occur when you have a REQUIRE lower in the stack than an ACCEPTABLE but the
   * goal stack gets popped while the WME backtrace still points to the REQUIRE, instead of the
   * higher ACCEPTABLE. See the section above on Preference Semantics. It also allows the GDS to
   * backtrace through ^items properly.
   *
   * <p>decide.cpp:1350:make_fake_preference_for_goal_item
   */
  private Preference make_fake_preference_for_goal_item(IdentifierImpl goal, Preference cand) {
    // find the acceptable preference wme we want to backtrace to
    final Slot s = cand.slot;
    WmeImpl ap_wme;
    for (ap_wme = s.getAcceptablePreferenceWmes(); ap_wme != null; ap_wme = ap_wme.next)
      if (ap_wme.value == cand.value) break;
    if (ap_wme == null) {
      throw new IllegalStateException("Internal error: couldn't find acceptable pref wme");
    }
    // make the fake preference
    final Preference pref =
        new Preference(
            PreferenceType.ACCEPTABLE, goal, predefinedSyms.item_symbol, cand.value, null);
    goal.goalInfo.addGoalPreference(pref);
    pref.on_goal_list = true;
    pref.preference_add_ref();

    // make the fake instantiation
    final Instantiation inst = new Instantiation(null, null, null);
    pref.setInstantiation(inst);
    inst.match_goal = goal;
    inst.match_goal_level = goal.level;
    inst.reliable = true;
    inst.backtrace_number = 0;
    inst.in_ms = false;

    // make the fake condition
    final PositiveCondition cond = new PositiveCondition();

    cond.id_test = SymbolImpl.makeEqualityTest(ap_wme.id); // make_equality_test
    // (ap_wme->id);
    cond.attr_test = SymbolImpl.makeEqualityTest(ap_wme.attr);
    cond.value_test = SymbolImpl.makeEqualityTest(ap_wme.value);
    cond.test_for_acceptable_preference = true;
    cond.bt().wme_ = ap_wme;
    cond.bt().level = ap_wme.id.level;

    inst.top_of_instantiated_conditions = cond;
    inst.bottom_of_instantiated_conditions = cond;
    inst.nots = null;

    if (SoarConstants.DO_TOP_LEVEL_REF_CTS) {
      // (removed in jsoar) ap_wme.wme_add_ref();
    } else {
      if (inst.match_goal_level > SoarConstants.TOP_GOAL_LEVEL) {
        // (removed in jsoar) ap_wme.wme_add_ref();
      }
    }

    // return the fake preference
    return pref;
  }

  /**
   * Remove_fake_preference_for_goal_item() is called to clean up the fake stuff once the (goal
   * ^item) wme is no longer needed.
   *
   * <p>decide.cpp:1419:remove_fake_preference_for_goal_item
   *
   * @param pref
   */
  private void remove_fake_preference_for_goal_item(Preference pref) {
    pref.preference_remove_ref(recMemory); /* everything else happens automatically */
  }

  /**
   * This routine updates the set of ^item wmes on a goal or attribute impasse. It takes the
   * identifier of the goal/impasse, and a list of preferences (linked via the "next_candidate"
   * field) for the new set of items that should be there.
   *
   * <p>decide.cpp:1432:update_impasse_items
   *
   * @param id
   * @param items
   */
  private void update_impasse_items(IdentifierImpl id, Preference items) {
    /*
    Count up the number of candidates
    REW: 2003-01-06
    I'm assuming that all of the candidates have unary or
    unary+value (binary) indifferent preferences at this point.
    So we loop over the candidates list and count the number of
    elements in the list.
    */
    final int item_count = Preference.countCandidates(items);

    // reset flags on existing items to "NOTHING"
    for (WmeImpl w = id.goalInfo.getImpasseWmes(); w != null; w = w.next)
      if (w.attr == predefinedSyms.item_symbol) w.value.decider_flag = DeciderFlag.NOTHING;

    // mark set of desired items as "CANDIDATEs"
    for (Preference cand = items; cand != null; cand = cand.next_candidate)
      cand.value.decider_flag = DeciderFlag.CANDIDATE;

    // for each existing item: if it's supposed to be there still, then
    // mark it "ALREADY_EXISTING"; otherwise remove it
    WmeImpl w = id.goalInfo.getImpasseWmes();
    while (w != null) {
      final WmeImpl next_w = w.next;
      if (w.attr == predefinedSyms.item_symbol) {
        if (w.value.decider_flag == DeciderFlag.CANDIDATE) {
          w.value.decider_flag = DeciderFlag.ALREADY_EXISTING_WME;
          w.value.decider_wme = w; // so we can update the pref later
        } else {
          id.goalInfo.removeImpasseWme(w);
          remove_fake_preference_for_goal_item(w.preference);
          this.workingMemory.remove_wme_from_wm(w);
        }
      }

      // SBW 5/07
      // remove item-count WME if it exists
      else if (w.attr == predefinedSyms.item_count_symbol) {
        id.goalInfo.removeImpasseWme(w);
        this.workingMemory.remove_wme_from_wm(w);
      }

      w = next_w;
    }

    // for each desired item: if it doesn't ALREADY_EXIST, add it
    for (Preference cand = items; cand != null; cand = cand.next_candidate) {
      Preference bt_pref;
      if (id.isGoal()) bt_pref = make_fake_preference_for_goal_item(id, cand);
      else bt_pref = cand;
      if (cand.value.decider_flag == DeciderFlag.ALREADY_EXISTING_WME) {
        if (id.isGoal()) remove_fake_preference_for_goal_item(cand.value.decider_wme.preference);
        cand.value.decider_wme.preference = bt_pref;
      } else {
        add_impasse_wme(id, predefinedSyms.item_symbol, cand.value, bt_pref);
      }
    }

    // SBW 5/07
    // update the item-count WME
    // detect relevant impasses by having more than one item
    if (item_count > 0) {
      add_impasse_wme(
          id,
          predefinedSyms.item_count_symbol,
          predefinedSyms.getSyms().createInteger(item_count),
          null);
    }
  }

  /**
   * This routine decides a given slot, which must be a non-context slot. It calls
   * run_preference_semantics() on the slot, then updates the wmes and/or impasse for the slot
   * accordingly.
   *
   * <p>decide.cpp:1510:decide_non_context_slot
   *
   * @param s
   */
  private void decide_non_context_slot(Slot s) {
    final ByRef<Preference> candidates = ByRef.create(null);

    final ImpasseType impasse_type = run_preference_semantics(s, candidates);

    if (impasse_type == ImpasseType.NONE) {
      // reset marks on existing wme values to "NOTHING"
      for (WmeImpl w = s.getWmes(); w != null; w = w.next)
        w.value.decider_flag = DeciderFlag.NOTHING;

      // set marks on desired values to "CANDIDATES"
      for (Preference cand = candidates.value; cand != null; cand = cand.next_candidate)
        cand.value.decider_flag = DeciderFlag.CANDIDATE;

      // for each existing wme, if we want it there, mark it as ALREADY_EXISTING; otherwise remove
      // it
      WmeImpl it = s.getWmes();
      while (it != null) {
        final WmeImpl w = it;
        it = w.next;

        if (w.value.decider_flag == DeciderFlag.CANDIDATE) {
          w.value.decider_flag = DeciderFlag.ALREADY_EXISTING_WME;
          w.value.decider_wme = w; /* so we can set the pref later */
        } else {
          s.removeWme(w);
          if (w.gds != null) {
            if (w.gds.getGoal() != null) {
              // If the goal pointer is non-NIL, then goal is in the stack
              gds_invalid_so_remove_goal(w, "While deciding non-context slot");
            }
          }
          this.workingMemory.remove_wme_from_wm(w);
        }
      }

      // for each desired value, if it's not already there, add it
      for (Preference cand = candidates.value; cand != null; cand = cand.next_candidate) {
        if (cand.value.decider_flag == DeciderFlag.ALREADY_EXISTING_WME) {
          /* print(thisAgent, "\n This WME was marked as already existing...."); print_wme(cand->value->common.a.decider_wme); */
          cand.value.decider_wme.preference = cand;
        } else {
          WmeImpl w = this.workingMemory.make_wme(cand.id, cand.attr, cand.value, false);
          s.addWme(w);
          w.preference = cand;

          if ((s.wma_val_references != null) && wma.wma_enabled()) {
            Long numRefs = s.wma_val_references.get(w.getValue());
            if (numRefs != null) {
              // should only activate at this point if WME is o-supported
              wma.wma_activate_wme(w, numRefs, null, true);

              s.wma_val_references.remove(w.getValue());
              if (s.wma_val_references.isEmpty()) {
                s.wma_val_references = null;
              }
            }
          }

          /* Whenever we add a WME to WM, we also want to check and see if
          this new WME is o-supported.  If so, then we want to add the
          supergoal dependencies of the new, o-supported element to the
          goal in which the element was created (as long as the o_supported
          element was not created in the top state -- the top goal has
             no gds).  */

          // #ifndef NO_TIMING_STUFF
          // #ifdef DETAILED_TIMING_STATS
          //              start_timer(thisAgent, &thisAgent->start_gds_tv);
          // #endif
          // #endif

          this.parent_list_head = null;

          /* If the working memory element being added is going to have
          o_supported preferences and the instantion that created it
          is not in the top_level_goal (where there is no GDS), then
          loop over the preferences for this WME and determine which
          WMEs should be added to the goal's GDS (the goal here being the
          goal to which the added memory is attached). */

          if ((w.preference.o_supported == true) && (w.preference.inst.match_goal_level != 1)) {

            if (w.preference.inst.match_goal.goalInfo.gds == null) {
              /* If there is no GDS yet for this goal,
               * then we need to create one */
              if (w.preference.inst.match_goal_level == w.preference.id.level) {

                create_gds_for_goal(w.preference.inst.match_goal);

                /* REW: BUG When chunks and result instantiations both create
                 * preferences for the same WME, then we only want to create
                 * the GDS for the highest goal.  Right now I ensure that we
                 * elaborate the correct GDS with the tests in the loop just
                 * below this code, but the GDS creation above assumes that
                 * the chunk will be first on the GDS list.  This order
                 * appears to be always true, although I am not 100% certain
                 * (I think it occurs this way because the chunk is
                 * necessarily added to the instantiation list after the
                 * original instantiation and lists get built such older items
                 * appear further from the head of the list) . If not true,
                 * then we need to keep track of any GDS's that get created
                 * here to remove them later if we find a higher match goal
                 * for the WME. For now, the program just exits in this
                 * situation; otherwise, we would build a GDS for the wrong
                 * level and never elaborate it (resulting in a memory
                 * leak).
                 */
              } else {
                // If this happens, we better be halted, see chunk.cpp:chunk_instantiation
                // This can happen if a chunk can't be created, because then the match level
                // of the preference instantiation can map back to the original matching
                // production which can be at a different level than the id wme.
                // Normally, there would be a chunk or justification firing at the higher
                // goal with a match level equal to the id level.
                // See more comments in chunk_instantiation.
                if (!this.decisionCycle.isHalted()) {
                  throw new IllegalStateException(
                      "Wanted to create a GDS for a WME level different from the instantiation level.....Big problems....exiting....");
                }
              }
            } /* end if no GDS yet for goal... */

            /* Loop over all the preferences for this WME:
             *   If the instantiation that lead to the preference has not
             *         been already explored; OR
             *   If the instantiation is not an subgoal instantiation
             *          for a chunk instantiation we are already exploring
             *   Then
             *      Add the instantiation to a list of instantiations that
             *          will be explored in elaborate_gds().
             */

            if (!this.decisionCycle.isHalted()) {
              for (Preference pref = w.preference; pref != null; pref = pref.next) {
                if (DEBUG_GDS_HIGH) {
                  context
                      .getPrinter()
                      .print("\n\n   %s   Goal level of preference: %d\n", pref, pref.id.level);
                }

                if (pref.inst.GDS_evaluated_already == false) {
                  if (DEBUG_GDS_HIGH) {
                    context
                        .getPrinter()
                        .print(
                            "   Match goal lev of instantiation %s is %d\n",
                            pref.inst.prod.getName(), pref.inst.match_goal_level);
                  }
                  if (pref.inst.match_goal_level > pref.id.level) {
                    if (DEBUG_GDS_HIGH) {
                      context
                          .getPrinter()
                          .print(
                              "        %s  is simply the instantiation that led to a chunk.\n        Not adding it the current instantiations.\n",
                              pref.inst.prod.getName());
                    }

                  } else {
                    if (DEBUG_GDS_HIGH) {
                      context
                          .getPrinter()
                          .print(
                              "\n   Adding %s to list of parent instantiations\n",
                              pref.inst.prod.getName());
                    }
                    uniquely_add_to_head_of_dll(pref.inst);
                    pref.inst.GDS_evaluated_already = true;
                  }
                } /* end if GDS_evaluated_already is FALSE */ else if (DEBUG_GDS_HIGH) {
                  context
                      .getPrinter()
                      .print(
                          "\n    Instantiation %s was already explored; skipping it\n",
                          pref.inst.prod.getName());
                }
              } /* end of forloop over preferences for this wme */

              if (DEBUG_GDS_HIGH) {
                context.getPrinter().print("\n    CALLING ELABORATE GDS....\n");
              }
              elaborate_gds();

              /* technically, the list should be empty at this point ??? */

              free_parent_list();
              if (DEBUG_GDS_HIGH) {
                context.getPrinter().print("    FINISHED ELABORATING GDS.\n\n");
              }
            } // end if not halted
          } /* end if w->preference->o_supported == TRUE ... */

          /* REW: begin 11.25.96 */
          // #ifndef NO_TIMING_STUFF
          // #ifdef DETAILED_TIMING_STATS
          //              stop_timer(thisAgent, &thisAgent->start_gds_tv,
          //                 &thisAgent->gds_cpu_time[thisAgent->current_phase]);
          // #endif
          // #endif
          /* REW: end   11.25.96 */

          this.workingMemory.add_wme_to_wm(w);
        }
      }

      return;
    } /* end of if impasse type == NONE */

    // impasse type != NONE
    if (s.getWmes() != null) {
      // remove any existing wmes
      this.workingMemory.remove_wme_list_from_wm(s.getWmes(), false);
      s.removeAllWmes();
    }

    // create and/or update impasse structure
    update_impasse_items(s.impasse_id, candidates.value);
  }

  /**
   * This routine iterates through all changed non-context slots, and decides each one.
   *
   * <p>decide.cpp:1766:decide_non_context_slots
   */
  private void decide_non_context_slots() {
    final ListHead<Slot> changed_slots = tempMemory.changed_slots;
    while (!changed_slots.isEmpty()) {
      Slot s = changed_slots.pop();
      decide_non_context_slot(s);
      s.changed = null;
    }
  }

  /**
   * This returns TRUE iff the given slot (which must be a context slot) is decidable. A context
   * slot is decidable if it has no installed value but does have changed preferences
   *
   * <p>decide.cpp:1791:context_slot_is_decidable
   */
  private boolean context_slot_is_decidable(Slot s) {
    if (s.getWmes() == null) return s.changed != null;

    return false;
  }

  /**
   * This removes the wmes (there can only be 0 or 1 of them) for the given context slot.
   *
   * <p>decide.cpp:1816:remove_wmes_for_context_slot
   *
   * @param s
   */
  void remove_wmes_for_context_slot(Slot s) {
    if (s.getWmes() == null) return;
    /*
     * Note that we only need to handle one wme--context slots never have
     * more than one wme in them
     */
    final WmeImpl w = s.getWmes();
    assert w.next == null;
    w.preference.preference_remove_ref(recMemory);
    this.workingMemory.remove_wme_from_wm(w);
    s.removeAllWmes();
  }

  /**
   * This routine truncates the goal stack by removing the given goal and all its subgoals. (If the
   * given goal is the top goal, the entire context stack is removed.)
   *
   * <p>decide.cpp:2032:remove_existing_context_and_descendents
   *
   * @param goal
   */
  void remove_existing_context_and_descendents(IdentifierImpl goal) {
    // remove descendents of this goal
    if (goal.goalInfo.lower_goal != null)
      remove_existing_context_and_descendents(goal.goalInfo.lower_goal);

    // TODO callback POP_CONTEXT_STACK_CALLBACK
    // invoke callback routine
    // soar_invoke_callbacks(thisAgent, POP_CONTEXT_STACK_CALLBACK, (soar_call_data) goal);

    if ((goal != top_goal) && rl.rl_enabled()) {
      rl.rl_tabulate_reward_value_for_goal(goal);
      rl.rl_perform_update(
          0, true, goal, false); // this update only sees reward - there is no next state
    }

    /* --- disconnect this goal from the goal stack --- */
    if (goal == top_goal) {
      top_goal = null;
      bottom_goal = null;
    } else {
      bottom_goal = goal.goalInfo.higher_goal;
      bottom_goal.goalInfo.lower_goal = null;
    }

    /* --- remove any preferences supported by this goal --- */
    if (SoarConstants.DO_TOP_LEVEL_REF_CTS) {
      while (goal.goalInfo.preferences_from_goal != null) {
        final Preference p = goal.goalInfo.popGoalPreference();
        p.on_goal_list = false;

        if (!p.remove_preference_from_clones(recMemory))
          if (p.isInTempMemory()) recMemory.remove_preference_from_tm(p);
      }
    } else {
      /*
       * KJC Aug 05: this seems to cure a potential for exceeding
       * callstack when popping soar's goal stack and not doing
       * DO_TOP_LEVEL_REF_CTS Probably should make this change for all
       * cases, but needs testing.
       */
      // Prefs are added to head of dll, so try removing from tail
      if (goal.goalInfo.preferences_from_goal != null) {
        Preference p = goal.goalInfo.preferences_from_goal;
        while (p.all_of_goal_next != null) p = p.all_of_goal_next;
        while (p != null) {
          // RPM 10/06 we need to save this because p may be freed by the
          // end of the loop
          final Preference p_next = p.all_of_goal_prev;
          goal.goalInfo.removeGoalPreference(p);
          p.on_goal_list = false;
          if (!p.remove_preference_from_clones(recMemory))
            if (p.isInTempMemory()) recMemory.remove_preference_from_tm(p);
          p = p_next;
        }
      }
    }
    // remove wmes for this goal, and garbage collect
    remove_wmes_for_context_slot(goal.goalInfo.operator_slot);
    update_impasse_items(goal, null); // causes items & fake pref's to go away

    epmem.epmem_reset(goal);
    smem.smem_reset(goal);

    this.workingMemory.remove_wme_list_from_wm(goal.goalInfo.getImpasseWmes(), false);
    goal.goalInfo.removeAllImpasseWmes();

    /*
     * If there was a GDS for this goal, we want to set the pointer for the
     * goal to NIL to indicate it no longer exists. BUG: We probably also
     * need to make certain that the GDS doesn't need to be free'd here as
     * well.
     */
    if (goal.goalInfo.gds != null) {
      goal.goalInfo.gds.clearGoal();
    }

    /*
     * If we remove a goal WME, then we have to transfer any already
     * existing retractions to the nil-goal list on the current agent. We
     * should be able to do this more efficiently but the most obvious way
     * (below) still requires scanning over the whole list (to set the goal
     * pointer of each msc to NIL); therefore this solution should be
     * acceptably efficient.
     */

    if (!goal.goalInfo.ms_retractions.isEmpty()) {
        /* There's something on the retraction list */

      final MatchSetChange head = goal.goalInfo.ms_retractions.getFirstItem();
      MatchSetChange tail = head;

      // find the tail of this list
      while (tail.in_level.next != null) {
        tail.goal = null; // force the goal to be NIL
        tail = tail.in_level.getNextItem();
      }
      tail.goal = null;

      final ListHead<MatchSetChange> nil_goal_retractions =
          this.soarReteListener.nil_goal_retractions;
      if (!nil_goal_retractions.isEmpty()) {
        /* There are already retractions on the list */

        /* Append this list to front of NIL goal list */
        // TODO replace this with a splice operation
        nil_goal_retractions.first.previous = tail.in_level;
        tail.in_level.next = nil_goal_retractions.first;
        nil_goal_retractions.first = head.in_level;

      } else {
          /* If no retractions, make this list the NIL goal list */
        nil_goal_retractions.first = head.in_level;
      }
    }

    // decide.cpp:remove_existing_context_and_descendents_rl
    goal.goalInfo.rl_info = null;

    /* REW: BUG
     * Tentative assertions can exist for removed goals.  However, it looks
     * like the removal forces a tentative retraction, which then leads to
     * the deletion of the tentative assertion.  However, I have not tested
     * such cases exhaustively -- I would guess that some processing may be
     * necessary for the assertions here at some point?
     */

    /* We have to remove this state from the list of states to learn in (NLD: and free cons)
     * jzxu April 24, 2009 */
    // TODO: The fact that we have to access chunker here sucks. Decouple?
    this.chunker.removeGoalFromChunkyProblemSpaces(goal);
    this.chunker.removeGoalFromChunkFreeProblemSpaces(goal);

    post_link_removal(null, goal); // remove the special link
  }

  /**
   * decide.cpp:2215:create_new_context_rl (9.3.0)
   *
   * @param id
   */
  private void create_new_context_rl(IdentifierImpl id) {
    id.goalInfo.rl_info = new ReinforcementLearningInfo();
    // everything else set by ReinforcementLearningInfo constructor
  }

  /**
   * This routine creates a new goal context (becoming the new bottom goal) below the current bottom
   * goal. If there is no current bottom goal, this routine creates a new goal and makes it both the
   * top and bottom goal.
   *
   * <p>decide.cpp:1969:create_new_context
   *
   * @param attr_of_impasse
   * @param impasse_type
   */
  private void create_new_context(SymbolImpl attr_of_impasse, ImpasseType impasse_type) {
    IdentifierImpl id;

    if (bottom_goal != null) {
      // Creating a sub-goal (or substate)
      id = create_new_impasse(bottom_goal, attr_of_impasse, impasse_type, bottom_goal.level + 1);

      // Insert into goal stack
      id.goalInfo.higher_goal = bottom_goal;
      bottom_goal.goalInfo.lower_goal = id;
      bottom_goal = id;

      add_impasse_wme(id, predefinedSyms.quiescence_symbol, predefinedSyms.t_symbol, null);
      if ((ImpasseType.NO_CHANGE == impasse_type) && (MAX_GOAL_DEPTH < bottom_goal.level)) {
        // appear to be SNC'ing deep in goalstack, so interrupt and warn user
        // KJC note: we actually halt, because there is no interrupt function in SoarKernel
        // in the gSKI Agent code, if system_halted, MAX_GOAL_DEPTH is checked and if exceeded
        // then the interrupt is generated and system_halted is set to FALSE so the user can
        // recover.

        context
            .getPrinter()
            .warn(
                "\nGoal stack depth exceeded %d on a no-change impasse.\n"
                    + "Soar appears to be in an infinite loop.  \n"
                    + "Continuing to subgoal may cause Soar to \n"
                    + "exceed the program stack of your system.\n",
                MAX_GOAL_DEPTH);

        this.decisionCycle.halt("Max Goal Depth (" + MAX_GOAL_DEPTH + ") exceeded");
      }
    } else {
      // Creating the top state
      id =
          create_new_impasse(
              predefinedSyms.nil_symbol, null, ImpasseType.NONE, SoarConstants.TOP_GOAL_LEVEL);

      // Insert into goal stack
      top_goal = id;
      bottom_goal = id;
      top_state = top_goal;
    }

    create_new_context_rl(id);

    /* --- invoke callback routine --- */
    // TODO callback CREATE_NEW_CONTEXT_CALLBACK
    //  soar_invoke_callbacks(thisAgent, CREATE_NEW_CONTEXT_CALLBACK, (soar_call_data) id);
  }

  /**
   * Given a goal, these routines return the type and attribute, respectively, of the impasse just
   * below that goal context. It does so by looking at the impasse wmes for the next lower goal in
   * the goal stack.
   *
   * <p>decide.cpp:2042:type_of_existing_impasse
   *
   * @param goal
   * @return the type of the impasse
   */
  public ImpasseType type_of_existing_impasse(IdentifierImpl goal) {
    if (goal.goalInfo.lower_goal == null) return ImpasseType.NONE;

    for (WmeImpl w = goal.goalInfo.lower_goal.goalInfo.getImpasseWmes(); w != null; w = w.next) {
      if (w.attr == predefinedSyms.impasse_symbol) {
        if (w.value == predefinedSyms.no_change_symbol) return ImpasseType.NO_CHANGE;
        if (w.value == predefinedSyms.tie_symbol) return ImpasseType.TIE;
        if (w.value == predefinedSyms.constraint_failure_symbol)
          return ImpasseType.CONSTRAINT_FAILURE;
        if (w.value == predefinedSyms.conflict_symbol) return ImpasseType.CONFLICT;
        if (w.value == predefinedSyms.none_symbol) return ImpasseType.NONE;

        throw new IllegalStateException("Internal error: bad type of existing impasse.");
      }
    }
    throw new IllegalStateException("Internal error: couldn't find type of existing impasse.");
  }

  /**
   * decide.cpp:2069:attribute_of_existing_impasse
   *
   * @param goal
   * @return the attribute of the existing impasse
   */
  public SymbolImpl attribute_of_existing_impasse(IdentifierImpl goal) {
    if (goal.goalInfo.lower_goal == null) return null;

    for (WmeImpl w = goal.goalInfo.lower_goal.goalInfo.getImpasseWmes(); w != null; w = w.next)
      if (w.attr == predefinedSyms.attribute_symbol) return w.value;

    throw new IllegalStateException("Internal error: couldn't find attribute of existing impasse.");
  }

  /**
   * This decides the given context slot. It normally returns TRUE, but returns FALSE if the ONLY
   * change as a result of the decision procedure was a change in the set of ^item's on the impasse
   * below the given slot.
   *
   * <p>decide.cpp:2092:decide_context_slot
   *
   * @param predict (defaulted to false in CSoar)
   */
  private boolean decide_context_slot(IdentifierImpl goal, Slot s, boolean predict /*= false*/) {
    ImpasseType impasse_type;
    SymbolImpl attribute_of_impasse;
    final ByRef<Preference> candidates = ByRef.create(null);

    if (!context_slot_is_decidable(s)) {
      // the only time we decide a slot that's not "decidable" is when it's
      // the last slot in the entire context stack, in which case we have a
      // no-change impasse there
      impasse_type = ImpasseType.NO_CHANGE;
      candidates.value = null; // we don't want any impasse ^item's later

      if (predict) {
        decisionManip.predict_set("none");
        return true;
      }
    } else {
      // the slot is decidable, so run preference semantics on it
      impasse_type = run_preference_semantics(s, candidates);

      if (predict) {
        // TODO make this a method on DecisionManip
        switch (impasse_type) {
          case CONSTRAINT_FAILURE:
            decisionManip.predict_set("constraint");
            break;

          case CONFLICT:
            decisionManip.predict_set("conflict");
            break;

          case TIE:
            decisionManip.predict_set("tie");
            break;

          case NO_CHANGE:
            decisionManip.predict_set("none");
            break;

          default:
            if (candidates.value == null || (candidates.value.value.asIdentifier() == null))
              decisionManip.predict_set("none");
            else {
              final IdentifierImpl tempId = candidates.value.value.asIdentifier();
              // TODO can this be null?
              final String temp = String.format("%s", tempId);
              decisionManip.predict_set(temp);
            }
            break;
        }

        return true;
      }

      remove_wmes_for_context_slot(s); // must remove old wme before adding the new one (if any)

      if (impasse_type == ImpasseType.NONE) {
        if (candidates.value == null) {
          // no winner ==> no-change impasse on the previous slot
          impasse_type = ImpasseType.NO_CHANGE;
        } else if (candidates.value.next_candidate != null) {
          // more than one winner ==> internal error
          throw new IllegalStateException("Internal error: more than one winner for context slot");
        }
      }
    } // end if !context_slot_is_decidable

    // mark the slot as not changed
    s.changed = null;

    // determine the attribute of the impasse (if there is no impasse, this
    // doesn't matter)
    if (impasse_type == ImpasseType.NO_CHANGE) {
      if (s.getWmes() != null) {
        attribute_of_impasse = s.attr;
      } else {
        attribute_of_impasse = predefinedSyms.state_symbol;
      }
    } else {
      // for all other kinds of impasses
      attribute_of_impasse = s.attr;
    }

    // remove wme's for lower slots of this context
    if (attribute_of_impasse == predefinedSyms.state_symbol) {
      remove_wmes_for_context_slot(goal.goalInfo.operator_slot);
    }

    // if we have a winner, remove any existing impasse and install the
    // new value for the current slot
    if (impasse_type == ImpasseType.NONE) {
      for (Preference temp = candidates.value; temp != null; temp = temp.next_candidate)
        temp.preference_add_ref();

      if (goal.goalInfo.lower_goal != null) {
        context
            .getTrace()
            .print(
                EnumSet.of(Category.VERBOSE, Category.WM_CHANGES),
                "Removing state %s because of a decision.\n",
                goal.goalInfo.lower_goal);
        remove_existing_context_and_descendents(goal.goalInfo.lower_goal);
      }

      WmeImpl w = this.workingMemory.make_wme(s.id, s.attr, candidates.value.value, false);
      s.addWme(w);
      w.preference = candidates.value;
      w.preference.preference_add_ref();

      /* JC Adding an operator to working memory in the current state */
      this.workingMemory.add_wme_to_wm(w);

      for (Preference temp = candidates.value; temp != null; temp = temp.next_candidate)
        temp.preference_remove_ref(recMemory);

      if (rl.rl_enabled()) rl.rl_store_data(goal, candidates.value);

      return true;
    }

    // no winner; if an impasse of the right type already existed, just
    // update the ^item set on it
    if ((impasse_type == type_of_existing_impasse(goal))
        && (attribute_of_impasse == attribute_of_existing_impasse(goal))) {
      update_impasse_items(goal.goalInfo.lower_goal, candidates.value);
      return false;
    }

    // no impasse already existed, or an impasse of the wrong type
    // already existed
    for (Preference temp = candidates.value; temp != null; temp = temp.next_candidate)
      temp.preference_add_ref();

    if (goal.goalInfo.lower_goal != null) {
      context
          .getTrace()
          .print(
              EnumSet.of(Category.VERBOSE, Category.WM_CHANGES),
              "Removing state %s because it's the wrong type of impasse.\n",
              goal.goalInfo.lower_goal);
      remove_existing_context_and_descendents(goal.goalInfo.lower_goal);
    }

    if (this.waitsnc.value.get()
        && (impasse_type == ImpasseType.NO_CHANGE)
        && (attribute_of_impasse == predefinedSyms.state_symbol)) {
      // Note: In csoar, waitsnc_detect was set to true here, but it was
      // never used in any actual code, so I (DR) removed it.
    } else {
      create_new_context(attribute_of_impasse, impasse_type);
      update_impasse_items(goal.goalInfo.lower_goal, candidates.value);
    }

    for (Preference temp = candidates.value; temp != null; temp = temp.next_candidate)
      temp.preference_remove_ref(recMemory);

    return true;
  }

  /**
   * This scans down the goal stack and runs the decision procedure on the appropriate context
   * slots.
   *
   * <p>decide.cpp:2289:decide_context_slots
   *
   * @param predict (defaulted to false in CSoar)
   */
  private void decide_context_slots(boolean predict /* = false */) {
    IdentifierImpl goal;

    if (tempMemory.highest_goal_whose_context_changed != null) {
      goal = tempMemory.highest_goal_whose_context_changed;
    } else
      /* no context changed, so jump right to the bottom */
      goal = bottom_goal;

    Slot s = goal.goalInfo.operator_slot;

    // loop down context stack
    while (true) {
      // find next slot to decide
      while (true) {
        if (context_slot_is_decidable(s)) break;

        if ((s == goal.goalInfo.operator_slot) || (s.getWmes() == null)) {
          // no more slots to look at for this goal; have we reached
          // the last slot in whole stack?
          if (goal.goalInfo.lower_goal == null) break;

          // no, go down one level
          goal = goal.goalInfo.lower_goal;
          s = goal.goalInfo.operator_slot;
        }
      } /* end of while (TRUE) find next slot to decide */

      // now go and decide that slot
      if (decide_context_slot(goal, s, predict)) break;
    } /* end of while (TRUE) loop down context stack */

    if (!predict) tempMemory.highest_goal_whose_context_changed = null;
  }

  /**
   * does the end-of-phases processing of WM changes, ownership calculations, garbage collection,
   * etc.
   *
   * <p>decide.cpp::do_buffered_wm_and_ownership_changes
   */
  public void do_buffered_wm_and_ownership_changes() {
    do_buffered_acceptable_preference_wme_changes();
    do_buffered_link_changes();
    this.workingMemory.do_buffered_wm_changes(io);
    tempMemory.remove_garbage_slots(context);
  }

  /** decide.cpp:2373:do_working_memory_phase */
  public void do_working_memory_phase() {
    final Trace trace = context.getTrace();
    if (trace.isEnabled() && trace.isEnabled(Category.PHASES)) {
      if (this.decisionCycle.current_phase.get() == Phase.APPLY) { // it's always IE for PROPOSE
        // TODO xml
        // xml_begin_tag(thisAgent, kTagSubphase);
        // xml_att_val(thisAgent, kPhase_Name, kSubphaseName_ChangingWorkingMemory);
        switch (recMemory.FIRING_TYPE) {
          case PE_PRODS:
            context.getPrinter().startNewLine().print("--- Change Working Memory (PE) ---\n");
            // TODO xml_att_val(thisAgent, kPhase_FiringType, kPhaseFiringType_PE);
            break;
          case IE_PRODS:
            context.getPrinter().startNewLine().print("--- Change Working Memory (IE) ---\n");
            // TODO xml_att_val(thisAgent, kPhase_FiringType, kPhaseFiringType_IE);
            break;
          default:
            // do nothing
            break;
        }
        // TODO xml_end_tag(thisAgent, kTagSubphase);
      }
    }

    decide_non_context_slots();
    do_buffered_wm_and_ownership_changes();
  }

  /**
   * decide.cpp:2409:do_decision_phase
   *
   * @param predict (defaulted to false in CSoar)
   */
  public void do_decision_phase(boolean predict /*=false*/) {
    decisionManip.predict_srand_restore_snapshot(!predict);

    /* phases printing moved to init_soar: do_one_top_level_phase */

    decide_context_slots(predict);

    if (!predict) {
      do_buffered_wm_and_ownership_changes();

      /*
       * Bob provided a solution to fix WME's hanging around unsupported
       * for an elaboration cycle.
       */
      decide_non_context_slots();
      do_buffered_wm_and_ownership_changes();

      exploration.exploration_update_parameters();
    }
  }

  /** decide.cpp:2435:create_top_goal */
  public void create_top_goal() {
    create_new_context(null, ImpasseType.NONE);
    tempMemory.highest_goal_whose_context_changed = null; // nothing changed yet
    do_buffered_wm_and_ownership_changes();
  }

  /** decide.cpp:2442:clear_goal_stack */
  public void clear_goal_stack() {
    if (top_goal == null) return;

    remove_existing_context_and_descendents(top_goal);
    tempMemory.highest_goal_whose_context_changed = null; // nothing changed yet
    do_buffered_wm_and_ownership_changes();
    top_state = null;
    active_goal = null;

    // jsoar: Moved do_input_cycle() and do_output_cycle() calls to reinitialize
  }

  /**
   * decide.cpp:2522:uniquely_add_to_head_of_dll
   *
   * @param inst
   */
  private void uniquely_add_to_head_of_dll(Instantiation inst) {
    /* print(thisAgent, "UNIQUE DLL:         scanning parent list...\n"); */

    for (ParentInstantiation curr_pi = parent_list_head; curr_pi != null; curr_pi = curr_pi.next) {
      if (curr_pi.inst == inst) {
        if (DEBUG_GDS) {
          context
              .getPrinter()
              .print(
                  "UNIQUE DLL: %s (%d) is already in parent list\n",
                  curr_pi.inst.prod.getName(), System.identityHashCode(curr_pi.inst));
        }
        return;
      }
      if (DEBUG_GDS) {
        context
            .getPrinter()
            .print(
                "UNIQUE DLL: %s (%d)\n",
                curr_pi.inst.prod.getName(), System.identityHashCode(curr_pi.inst));
      }
    } /* end for loop */

    ParentInstantiation new_pi = new ParentInstantiation();
    new_pi.next = null;
    new_pi.prev = null;
    new_pi.inst = inst;

    new_pi.next = parent_list_head;

    if (parent_list_head != null) parent_list_head.prev = new_pi;

    parent_list_head = new_pi;
    if (DEBUG_GDS) {
      context
          .getPrinter()
          .print("UNIQUE DLL: added: %s %d\n", inst.prod.getName(), System.identityHashCode(inst));
    }
  }

  /**
   * Added this function to make one place for wme's being added to the GDS. Callback for wme added
   * to GDS is made here.
   *
   * <p>decide.cpp:2562:add_wme_to_gds
   *
   * @param gds
   * @param wme_to_add
   */
  private void add_wme_to_gds(GoalDependencySetImpl gds, WmeImpl wme_to_add) {
    gds.addWme(wme_to_add);

    if (context
        .getTrace()
        .isEnabled(EnumSet.of(Category.GDS, Category.WM_CHANGES, Category.VERBOSE))) {
      context
          .getTrace()
          .startNewLine()
          .print("Adding to GDS for %s: %s", wme_to_add.gds.getGoal(), wme_to_add);
    }
  }

  /** decide.cpp:2587:elaborate_gds */
  private void elaborate_gds() {
    ParentInstantiation temp_pi = null;
    for (ParentInstantiation curr_pi = parent_list_head; curr_pi != null; curr_pi = temp_pi) {

      Instantiation inst = curr_pi.inst;
      if (DEBUG_GDS) {
        context.getTrace().print("\n      EXPLORING INSTANTIATION: %s\n", inst);
      }

      for (Condition cond = inst.top_of_instantiated_conditions; cond != null; cond = cond.next) {
        PositiveCondition pc = cond.asPositiveCondition();
        if (pc == null) continue;

        // We'll deal with negative instantiations after we get the
        // positive ones figured out

        WmeImpl wme_matching_this_cond = pc.bt().wme_;
        int wme_goal_level = pc.bt().level;
        Preference pref_for_this_wme = wme_matching_this_cond.preference;

        if (DEBUG_GDS) {
          context
              .getPrinter()
              .print(
                  "\n wme_matching_this_cond at goal_level = %d : %s",
                  wme_goal_level, wme_matching_this_cond);
          if (pref_for_this_wme != null) {
            context
                .getPrinter()
                .print("       pref_for_this_wme                        : %s", pref_for_this_wme);
          }
        }

        // WME is in a supergoal or is arch-supported WME (except for fake instantiations,
        // which do have prefs, so they get handled under "wme is local and i-supported")
        if ((pref_for_this_wme == null) || (wme_goal_level < inst.match_goal_level)) {

          if (DEBUG_GDS) {
            if (pref_for_this_wme == null) {
              context
                  .getPrinter()
                  .print(" this wme has no preferences (it's an arch-created wme)\n");
            } else if (wme_goal_level < inst.match_goal_level) {
              context.getPrinter().print(" this wme is in the supergoal\n");
            }
            context.getPrinter().print("inst->match_goal [%s]\n", inst.match_goal);
          }

          if (wme_matching_this_cond.gds != null) {
            // Then we want to check and see if the old GDS value
            // should be changed
            if (wme_matching_this_cond.gds.getGoal() == null) {
              // The goal is NIL: meaning that the goal for the GDS
              // is no longer around
              wme_matching_this_cond.gds.removeWme(wme_matching_this_cond);

              /* JC ADDED: Separate adding wme to GDS as a function */
              add_wme_to_gds(inst.match_goal.goalInfo.gds, wme_matching_this_cond);

              if (DEBUG_GDS) {
                context
                    .getPrinter()
                    .print(
                        "\n       .....GDS' goal is NIL so switching from old to new GDS list....\n");
              }

            } else if (wme_matching_this_cond.gds.getGoal().level > inst.match_goal_level) {
              // if the WME currently belongs to the GDS of a goal below the current one
              // 1. Take WME off old (current) GDS list
              // 2. Check to see if old GDS WME list is empty.  If so, remove(free) it.
              // 3. Add WME to new GDS list
              // 4. Update WME pointer to new GDS list

              wme_matching_this_cond.gds.removeWme(wme_matching_this_cond);

              add_wme_to_gds(inst.match_goal.goalInfo.gds, wme_matching_this_cond);

              if (DEBUG_GDS) {
                context.getPrinter().print("\n       ....switching from old to new GDS list....\n");
              }

              wme_matching_this_cond.gds = inst.match_goal.goalInfo.gds;
            }
          } else {
            // We know that the WME should be in the GDS of the current
            // goal if the WME's GDS does not already exist. (i.e., if NIL GDS)

            add_wme_to_gds(inst.match_goal.goalInfo.gds, wme_matching_this_cond);

            if (DEBUG_GDS) {
              context
                  .getPrinter()
                  .print(
                      "\n       ......WME did not have defined GDS.  Now adding to goal [%s].\n",
                      wme_matching_this_cond.gds.getGoal());
            }
          } /* end else clause for "if wme_matching_this_cond->gds != NIL" */

          if (DEBUG_GDS) {
            context
                .getPrinter()
                .print(
                    "            Added WME to GDS for goal = %d [%s]\n",
                    wme_matching_this_cond.gds.getGoal().level,
                    wme_matching_this_cond.gds.getGoal());
          }
        } /* end "wme in supergoal or arch-supported" */ else {
          // wme must be local

          // if wme's pref is o-supported, then just ignore it and
          // move to next condition
          if (pref_for_this_wme.o_supported == true) {
            if (DEBUG_GDS) {
              context.getPrinter().print("         this wme is local and o-supported\n");
            }
            continue;
          } else {
            // wme's pref is i-supported, so remember it's instantiation
            // for later examination

            // this test avoids "backtracing" through the top state
            if (inst.match_goal_level == 1) {
              if (DEBUG_GDS) {
                context.getPrinter().print("         don't back up through top state\n");
                if (inst.prod != null)
                  if (inst.prod.getName() != null)
                    context
                        .getPrinter()
                        .print(
                            "         don't back up through top state for instantiation %s\n",
                            inst.prod.getName());
              }
              continue;
            } else {
                /* (inst->match_goal_level != 1) */
              if (DEBUG_GDS) {
                context.getPrinter().print("         this wme is local and i-supported\n");
              }
              Slot s = Slot.find_slot(pref_for_this_wme.id, pref_for_this_wme.attr);
              if (s == null) {
                // this must be an arch-wme from a fake instantiation

                if (DEBUG_GDS) {
                  context
                      .getPrinter()
                      .print(
                          "here's the wme with no slot:\t %s",
                          pref_for_this_wme
                              .inst
                              .top_of_instantiated_conditions
                              .asPositiveCondition()
                              .bt()
                              .wme_);
                }

                // this is the same code as above, just using the
                // differently-named pointer.  it probably should
                // be a subroutine
                {
                  WmeImpl fake_inst_wme_cond =
                      pref_for_this_wme
                          .inst
                          .top_of_instantiated_conditions
                          .asPositiveCondition()
                          .bt()
                          .wme_;
                  if (fake_inst_wme_cond.gds != null) {
                    /* Then we want to check and see if the old GDS
                     * value should be changed */
                    if (fake_inst_wme_cond.gds.getGoal() == null) {
                      /* The goal is NIL: meaning that the goal for
                       * the GDS is no longer around */
                      fake_inst_wme_cond.gds.removeWme(fake_inst_wme_cond);

                      add_wme_to_gds(inst.match_goal.goalInfo.gds, fake_inst_wme_cond);

                      if (DEBUG_GDS) {
                        context
                            .getPrinter()
                            .print(
                                "\n       .....GDS' goal is NIL so switching from old to new GDS list....\n");
                      }
                    } else if (fake_inst_wme_cond.gds.getGoal().level > inst.match_goal_level) {
                      // if the WME currently belongs to the GDS of a goal below the current one
                      // 1. Take WME off old (current) GDS list
                      // 2. Check to see if old GDS WME list is empty. If so, remove(free) it.
                      // 3. Add WME to new GDS list
                      // 4. Update WME pointer to new GDS list

                      fake_inst_wme_cond.gds.removeWme(fake_inst_wme_cond);

                      add_wme_to_gds(inst.match_goal.goalInfo.gds, fake_inst_wme_cond);

                      if (DEBUG_GDS) {
                        context
                            .getPrinter()
                            .print("\n       .....switching from old to new GDS list....\n");
                      }
                      fake_inst_wme_cond.gds = inst.match_goal.goalInfo.gds;
                    }
                  } else {
                    // We know that the WME should be in the GDS of
                    // the current goal if the WME's GDS does not
                    // already exist. (i.e., if NIL GDS)

                    add_wme_to_gds(inst.match_goal.goalInfo.gds, fake_inst_wme_cond);

                    if (DEBUG_GDS) {
                      context
                          .getPrinter()
                          .print(
                              "\n       ......WME did not have defined GDS.  Now adding to goal [%s].\n",
                              fake_inst_wme_cond.gds.getGoal());
                    }
                  }
                  if (DEBUG_GDS) {
                    context
                        .getPrinter()
                        .print(
                            "            Added WME to GDS for goal = %d [%s]\n",
                            fake_inst_wme_cond.gds.getGoal().level,
                            fake_inst_wme_cond.gds.getGoal());
                  }
                } /* matches { wme *fake_inst_wme_cond  */
              } else {
                // this was the original "local & i-supported" action
                for (Preference pref = s.getPreferencesByType(PreferenceType.ACCEPTABLE);
                    pref != null;
                    pref = pref.next) {
                  if (DEBUG_GDS) {
                    context.getPrinter().print("           looking at pref for the wme: %s", pref);
                  }

                  /* REW: 2004-05-27: Bug fix
                  We must check that the value with acceptable pref for the slot
                  is the same as the value for the wme in the condition, since
                  operators can have acceptable preferences for values other than
                  the WME value.  We dont want to backtrack thru acceptable prefs
                  for other operators */

                  if (pref.value == wme_matching_this_cond.value) {

                    /* REW BUG: may have to go over all insts regardless
                     * of this visited_already flag... */

                    if (pref.inst.GDS_evaluated_already == false) {

                      if (DEBUG_GDS) {
                        context
                            .getPrinter()
                            .print(
                                "\n           adding inst that produced the pref to GDS: %s\n",
                                pref.inst.prod.getName());
                      }

                      // If the preference comes from a lower level inst, then ignore it.
                      // Preferences from lower levels must come from result instantiations;
                      // we just want to use the justification/chunk
                      // instantiations at the match goal level
                      if (pref.inst.match_goal_level <= inst.match_goal_level) {
                        uniquely_add_to_head_of_dll(pref.inst);
                        pref.inst.GDS_evaluated_already = true;
                      } else if (DEBUG_GDS) {
                        context
                            .getPrinter()
                            .print(
                                "\n           ignoring inst %s because it is at a lower level than the GDS\n",
                                pref.inst.prod.getName());
                        pref.inst.GDS_evaluated_already = true;
                      }
                    } else if (DEBUG_GDS) {
                      context
                          .getPrinter()
                          .print(
                              "           the inst producing this pref was already explored; skipping it\n");
                    }
                  } else if (DEBUG_GDS) {
                    context
                        .getPrinter()
                        .print(
                            "        this inst is for a pref with a differnt value than the condition WME; skippint it\n");
                  }
                } /* for pref = s->pref[ACCEPTABLE_PREF ...*/
              }
            }
          }
        }
      } /* for (cond = inst->top_of_instantiated_cond ...  *;*/

      // remove just used instantiation from list

      if (DEBUG_GDS) {
        context
            .getPrinter()
            .print("\n      removing instantiation: %s\n", curr_pi.inst.prod.getName());
      }

      if (curr_pi.next != null) curr_pi.next.prev = curr_pi.prev;

      if (curr_pi.prev != null) curr_pi.prev.next = curr_pi.next;

      if (parent_list_head == curr_pi) parent_list_head = curr_pi.next;

      temp_pi = curr_pi.next;
    } /* end of "for (curr_pi = thisAgent->parent_list_head ... */

    if (parent_list_head != null) {

      if (DEBUG_GDS) {
        context.getPrinter().print("\n    RECURSING using these parents:\n");
        for (ParentInstantiation curr_pi = parent_list_head;
            curr_pi != null;
            curr_pi = curr_pi.next) {
          context.getPrinter().print("      %s\n", curr_pi.inst.prod.getName());
        }
      }

      // recursively explore the parents of all the instantiations
      elaborate_gds();

      // free the parent instantiation list.  technically, the list
      //  should be empty at this point ???
      free_parent_list();
    }
  }

  /**
   * REW BUG: this needs to be smarter to deal with wmes that get support from multiple
   * instantiations. for example ^enemy-out-there could be made by 50 instantiations. if one of
   * those instantiations goes, should the goal be killed???? This routine says "yes" -- anytime a
   * dependent item gets changed, we're gonna yank out the goal -- even when that i-supported
   * element itself may not be removed (due to multiple preferences). So, we'll say that this is a
   * "twitchy" version of OPERAND2, and leave open the possibility that other approaches may be
   * better
   *
   * <p>decide.cpp:3040:gds_invalid_so_remove_goal
   *
   * @param w the WME whose removal invalidated the GDS
   * @param traceContext a string, possibly {@code null} indicating the context in which the WME was
   *     removed.
   */
  public void gds_invalid_so_remove_goal(WmeImpl w, String traceContext) {
    // This trace was original copied in al the places where this method
    // was called.
    context
        .getTrace()
        .print(
            EnumSet.of(Category.GDS, Category.VERBOSE),
            "%n%sRemoving state %s because element in GDS changed. WME: %s",
            traceContext != null ? traceContext + ": " : "",
            w.gds.getGoal(),
            w);

    // #ifndef NO_TIMING_STUFF
    // #ifdef DETAILED_TIMING_STATS
    // start_timer(thisAgent, &thisAgent->start_gds_tv);
    // #endif
    // #endif

    /* REW: BUG.  I have no idea right now if this is a terrible hack or
     * actually what we want to do.  The idea here is that the context of
     * the immediately higher goal above a retraction should be marked as
     * having its context changed in order that the architecture doesn't
     * look below this level for context changes.  I think it's a hack b/c
     * it seems like there should aready be mechanisms for doing this in
     * the architecture but I couldn't find any.
     */
    /* Note: the inner 'if' is correct -- we only want to change
     * highest_goal_whose_context_changed if the pointer is currently at
     * or below (greater than) the goal which we are going to retract.
     * However, I'm not so sure about the outer 'else.'  If we don't set
     * this to the goal above the retraction, even if the current value
     * is NIL, we still seg fault in certain cases.  But setting it as we do
     * in the inner 'if' seems to clear up the difficulty.
     */

    if (tempMemory.highest_goal_whose_context_changed != null) {
      if (tempMemory.highest_goal_whose_context_changed.level >= w.gds.getGoal().level) {
        tempMemory.highest_goal_whose_context_changed = w.gds.getGoal().goalInfo.higher_goal;
      }
    } else {
      // If nothing has yet changed (highest_ ... = NIL) then set the goal automatically
      tempMemory.highest_goal_whose_context_changed = w.gds.getGoal().goalInfo.higher_goal;

      // Tell those slots they are changed so that the impasses can be regenerated
      // bug 1011
      for (Slot s = tempMemory.highest_goal_whose_context_changed.slots; s != null; s = s.next) {
        if (s.isa_context_slot && s.changed == null) {
          s.changed = s; // use non-zero value to indicate change, see definition of slot::changed
        }
      }
    }

    final Goal goal = Adaptables.adapt(w.gds.getGoal(), Goal.class);
    remove_existing_context_and_descendents(w.gds.getGoal());
    /* BUG: Need to reset highest_goal here ???*/

    /* usually, we'd call do_buffered_wm_and_ownership_changes() here, but
     * we don't need to because it will be done at the end of the working
     * memory phases; cf. the end of do_working_memory_phase().
     */

    /* REW: begin 11.25.96 */
    //  #ifndef NO_TIMING_STUFF
    //  #ifdef DETAILED_TIMING_STATS
    //  stop_timer(thisAgent, &thisAgent->start_gds_tv,
    //             &thisAgent->gds_cpu_time[thisAgent->current_phase]);
    //  #endif
    //  #endif
    /* REW: end   11.25.96 */

    context.getEvents().fireEvent(new GdsGoalRemovedEvent(context, goal, w));
  }

  /** decide.cpp:3107:free_parent_list */
  private void free_parent_list() {
    // parent_inst *curr_pi;
    //
    // for (curr_pi = thisAgent->parent_list_head;
    // curr_pi;
    // curr_pi = curr_pi->next)
    // free(curr_pi);

    this.parent_list_head = null;
  }

  /**
   * decide.cpp:3119:create_gds_for_goal
   *
   * <p>TODO Make this a GoalDependencySet constructor?
   *
   * @param goal
   */
  private void create_gds_for_goal(IdentifierImpl goal) {
    goal.goalInfo.gds = new GoalDependencySetImpl(goal);
    if (DEBUG_GDS) {
      context.getPrinter().print("\nCreated GDS for goal [%s].\n", goal);
    }
  }
}

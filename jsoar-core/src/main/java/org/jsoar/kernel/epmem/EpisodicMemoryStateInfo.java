/*
 * Copyright (c) 2012 Soar Technology Inc.
 *
 * Created on January 17, 2013
 */
package org.jsoar.kernel.epmem;

import java.util.ArrayDeque;
import java.util.Deque;
import org.jsoar.kernel.memory.Preference;
import org.jsoar.kernel.memory.WmeImpl;
import org.jsoar.kernel.memory.WorkingMemory;
import org.jsoar.kernel.modules.SoarModule;
import org.jsoar.kernel.symbols.IdentifierImpl;
import org.jsoar.kernel.symbols.SymbolImpl;

/**
 * Epmem info associated with a state. This structure isn't accessed in a tight loop so we can just
 * maintain a map in {@link EpisodicMemory}
 *
 * <p>episodic_memory.h:427:epmem_data_struct decide.cpp:2464:create_new_context (initialization)
 *
 * @author skrawczyk
 */
public class EpisodicMemoryStateInfo {
  public long last_ol_time = 0; // last update to output-link
  public long last_ol_count = 0; // last count of output-link

  public long last_cmd_time = 0; // last update to epmem.command
  public long last_cmd_count = 0; // last update to epmem.command

  public long last_memory = DefaultEpisodicMemory.EPMEM_MEMID_NONE; // last retrieved memory

  public final Deque<Preference> epmem_wmes = new ArrayDeque<Preference>(); // wmes in last epmem

  public final IdentifierImpl epmem_header;
  public final IdentifierImpl epmem_cmd_header;
  public final IdentifierImpl epmem_result_header;

  public WmeImpl epmem_time_wme;

  public EpisodicMemoryStateInfo(DefaultEpisodicMemory epmem, WorkingMemory wm, IdentifierImpl id) {
    // id->id.epmem_header = make_new_identifier( thisAgent, 'E', level );
    epmem_header = epmem.symbols.createIdentifier('E', id.getLevel());
    // soar_module::add_module_wme( thisAgent, id, thisAgent->epmem_sym, id->id.epmem_header );
    SoarModule.add_module_wme(wm, id, epmem.predefinedSyms.epmem_sym, epmem_header);
    // id->id.epmem_cmd_header = make_new_identifier( thisAgent, 'C', level );
    epmem_cmd_header = epmem.symbols.createIdentifier('C', id.getLevel());
    // soar_module::add_module_wme( thisAgent, id->id.epmem_header, thisAgent->epmem_sym_cmd,
    // id->id.epmem_cmd_header );
    SoarModule.add_module_wme(
        wm, epmem_header, epmem.predefinedSyms.epmem_sym_cmd, epmem_cmd_header);
    // id->id.epmem_result_header = make_new_identifier( thisAgent, 'R', level );
    epmem_result_header = epmem.symbols.createIdentifier('R', id.getLevel());
    // soar_module::add_module_wme( thisAgent, id->id.epmem_header, thisAgent->epmem_sym_result,
    // id->id.epmem_result_header );
    SoarModule.add_module_wme(
        wm, epmem_header, epmem.predefinedSyms.epmem_sym_result, epmem_result_header);

    // epmem_time_wme = SoarModule.add_module_wme(wm, id, epmem.predefinedSyms.epmem_sym_present_id,
    // epmem_result_header);

    {
      // int64_t my_time = static_cast<int64_t>( thisAgent->epmem_stats->time->get_value() );
      long my_time = epmem.stats.time.get();
      if (my_time == 0) {
        // special case: pre-initialization
        my_time = 1;
      }
      SymbolImpl my_time_sym = epmem.symbols.createInteger(my_time);
      epmem_time_wme =
          SoarModule.add_module_wme(
              wm, epmem_header, epmem.predefinedSyms.epmem_sym_present_id, my_time_sym);
      // symbol_remove_ref( thisAgent, my_time_sym );
    }
  }
}

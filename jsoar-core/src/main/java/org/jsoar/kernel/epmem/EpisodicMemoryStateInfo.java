package org.jsoar.kernel.epmem;

/**
 * Epmem info associated with a state.  This structure isn't accessed in a tight loop
 * so we can just maintain a map in {@link EpisodicMemory}
 * 
 * episodic_memory.h:427:epmem_data_struct
 * decide.cpp:2464:create_new_context (initialization)
 * 
 * @author skrawczyk
 *
 */

public class EpisodicMemoryStateInfo 
{   
    public long last_ol_time;       // last update to output-link
    public long last_ol_count;      // last count of output-link
    
    public long last_cmd_time;      // last update to epmem.command
    public long last_cmd_count;     // last update to epmem.command
    
    public long last_memory = DefaultEpisodicMemory.EPMEM_MEMID_NONE;        // last retrieved memory
	
	public EpisodicMemoryStateInfo()
	{
		//TODO stub
//    	id->id.epmem_header = make_new_identifier( thisAgent, 'E', level );		
//    	soar_module::add_module_wme( thisAgent, id, thisAgent->epmem_sym, id->id.epmem_header );
//    	id->id.epmem_cmd_header = make_new_identifier( thisAgent, 'C', level );
//    	soar_module::add_module_wme( thisAgent, id->id.epmem_header, thisAgent->epmem_sym_cmd, id->id.epmem_cmd_header );	
//    	id->id.epmem_result_header = make_new_identifier( thisAgent, 'R', level );
//    	soar_module::add_module_wme( thisAgent, id->id.epmem_header, thisAgent->epmem_sym_result, id->id.epmem_result_header );

// CK: not implementing timers
//    	{
//    	  int64_t my_time = static_cast<int64_t>( thisAgent->epmem_stats->time->get_value() );
//    	  if ( my_time == 0 )
//    	  {
//    		  // special case: pre-initialization
//    		  my_time = 1;
//    	  }
//    	  
//    	  Symbol* my_time_sym = make_int_constant( thisAgent, my_time );
//    	  id->id.epmem_time_wme = soar_module::add_module_wme( thisAgent, id->id.epmem_header, thisAgent->epmem_sym_present_id, my_time_sym );
//    	  symbol_remove_ref( thisAgent, my_time_sym );
//    	}
	}
}

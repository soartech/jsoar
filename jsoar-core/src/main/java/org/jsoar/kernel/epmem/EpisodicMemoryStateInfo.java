package org.jsoar.kernel.epmem;

public class EpisodicMemoryStateInfo 
{
	
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

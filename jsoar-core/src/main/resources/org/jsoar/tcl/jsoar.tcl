# Default Tcl code loaded by org.jsoar.tcl.SoarTclInterface

# http://winter.eecs.umich.edu/soarwiki/Alias
proc alias { { newcmd {} } args } {
    if {[string match {} $newcmd]} {
		set res {}
		foreach a [interp aliases] {
	    	lappend res [list $a -> [interp alias {} $a]]
		}
		return [join $res \n]
    } elseif {![llength $args]} {
		interp alias {} $newcmd
    } else {
		eval interp alias [list {} $newcmd {}] $args
	}
}

proc unalias { cmd } {
   alias $cmd {}
}

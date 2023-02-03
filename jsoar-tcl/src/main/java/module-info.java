module org.jsoar.tcl
{
    requires com.google.common;
    requires jtcl;
    requires org.jsoar.core;
    requires org.slf4j;
    
    provides org.jsoar.util.commands.SoarCommandInterpreterFactory with org.jsoar.tcl.SoarTclInterfaceFactory;
    
    exports org.jsoar.tcl;
}

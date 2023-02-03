module org.jsoar.debugger
{
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.google.common;
    requires docking.frames.common;
    requires docking.frames.core;
    requires info.picocli;
    requires java.datatransfer;
    requires java.desktop;
    requires java.prefs;
    requires java.xml;
    requires org.jsoar.core;
    requires org.jsoar.tcl;
    requires org.slf4j;
    requires re2j;
    requires swingx.all;
    
    exports org.jsoar.debugger;
    exports org.jsoar.debugger.actions;
    exports org.jsoar.debugger.plugins;
    exports org.jsoar.debugger.selection;
    exports org.jsoar.debugger.stopcommand;
    exports org.jsoar.debugger.syntax;
    exports org.jsoar.debugger.syntax.ui;
    exports org.jsoar.debugger.util;
    exports org.jsoar.debugger.wm;
    
    opens org.jsoar.debugger to info.picocli;
}

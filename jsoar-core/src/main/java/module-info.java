module org.jsoar.core
{
    requires com.google.common;
    requires commons.beanutils;
    requires commons.math3;
    requires transitive info.picocli;
    requires java.desktop;
    requires transitive java.scripting;
    requires transitive java.sql;
    requires java.xml;
    requires transitive json.simple;
    
    requires transitive org.slf4j;
    
    requires transitive spring.core;
    
    // optional dependencies: required to build, but need not be present at runtime
    // this is so projects using jsoar can exclude these dependencies if they don't need them
    // e.g., if they aren't using javascript, smem, or epmem
    requires static org.graalvm.js.scriptengine;
    requires static org.graalvm.sdk;
    requires static org.xerial.sqlitejdbc;
    
    uses org.jsoar.util.timing.ExecutionTimeSource;
    uses org.jsoar.util.commands.SoarCommandInterpreterFactory;
    uses org.jsoar.util.commands.SoarCommandProvider;
    
    provides org.jsoar.util.timing.ExecutionTimeSource with org.jsoar.util.timing.WallclockExecutionTimeSource;
    provides org.jsoar.util.commands.SoarCommandInterpreterFactory with org.jsoar.util.commands.DefaultInterpreterFactory;
    provides org.jsoar.util.commands.SoarCommandProvider with org.jsoar.kernel.smem.SmemCommand.Provider,
            org.jsoar.kernel.epmem.EpmemCommand.Provider,
            org.jsoar.script.ScriptCommand.Provider,
            org.jsoar.kernel.wma.WMActivationCommand.Provider,
            org.jsoar.kernel.learning.rl.RLCommand.Provider;
    
    exports org.jsoar.kernel;
    exports org.jsoar.kernel.commands;
    exports org.jsoar.kernel.epmem;
    exports org.jsoar.kernel.events;
    exports org.jsoar.kernel.exceptions;
    exports org.jsoar.kernel.exploration;
    exports org.jsoar.kernel.io;
    exports org.jsoar.kernel.io.beans;
    exports org.jsoar.kernel.io.commands;
    exports org.jsoar.kernel.io.json;
    exports org.jsoar.kernel.io.quick;
    exports org.jsoar.kernel.io.xml;
    exports org.jsoar.kernel.learning;
    exports org.jsoar.kernel.learning.rl;
    exports org.jsoar.kernel.lhs;
    exports org.jsoar.kernel.memory;
    exports org.jsoar.kernel.modules;
    exports org.jsoar.kernel.parser;
    exports org.jsoar.kernel.parser.original;
    exports org.jsoar.kernel.rete;
    exports org.jsoar.kernel.rhs;
    exports org.jsoar.kernel.rhs.functions;
    exports org.jsoar.kernel.smem;
    exports org.jsoar.kernel.smem.math;
    exports org.jsoar.kernel.symbols;
    exports org.jsoar.kernel.tracing;
    exports org.jsoar.kernel.wma;
    exports org.jsoar.runtime;
    exports org.jsoar.script;
    exports org.jsoar.util;
    exports org.jsoar.util.adaptables;
    exports org.jsoar.util.commands;
    exports org.jsoar.util.db;
    exports org.jsoar.util.events;
    exports org.jsoar.util.markers;
    exports org.jsoar.util.properties;
    exports org.jsoar.util.timing;
    
    opens org.jsoar.kernel.commands to info.picocli;
    opens org.jsoar.util.commands to info.picocli;
    opens org.jsoar.kernel.wma to info.picocli;
    opens org.jsoar.kernel.smem to info.picocli;
    opens org.jsoar.kernel.epmem to info.picocli;
    opens org.jsoar.script to info.picocli;
    opens org.jsoar.kernel.learning.rl to info.picocli;
}

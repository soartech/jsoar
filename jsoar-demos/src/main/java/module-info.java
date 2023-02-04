module org.jsoar.demos
{
    requires java.datatransfer;
    requires transitive java.desktop;
    requires transitive org.jsoar.core;
    requires transitive org.jsoar.debugger;
    requires org.slf4j;
    
    exports org.jsoar.demos.robot;
    exports org.jsoar.demos.robot.events;
    exports org.jsoar.demos.toh;
}

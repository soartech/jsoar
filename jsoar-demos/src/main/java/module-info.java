module org.jsoar.demos
{
    requires java.datatransfer;
    requires java.desktop;
    requires org.jsoar.core;
    requires org.jsoar.debugger;
    requires org.slf4j;
    
    exports org.jsoar.demos.robot;
    exports org.jsoar.demos.robot.events;
    exports org.jsoar.demos.toh;
}

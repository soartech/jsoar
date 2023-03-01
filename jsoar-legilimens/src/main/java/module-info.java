module org.jsoar.legilimens
{
    requires transitive freemarker;
    requires transitive org.jsoar.core;
    requires transitive org.restlet;
    requires org.restlet.ext.freemarker;
    requires org.slf4j;
    requires jdk.httpserver;
    
    exports org.jsoar.legilimens;
    exports org.jsoar.legilimens.resources;
    exports org.jsoar.legilimens.templates;
    exports org.jsoar.legilimens.trace;
    
    opens org.jsoar.legilimens.templates;
}

module org.jsoar.performancetesting
{
    exports org.jsoar.performancetesting;
    exports org.jsoar.performancetesting.csoar;
    exports org.jsoar.performancetesting.jsoar;
    exports org.jsoar.performancetesting.simplecli;
    exports org.jsoar.performancetesting.yaml;
    
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.google.common;
    requires com.opencsv;
    requires info.picocli;
    requires io.github.classgraph;
    requires org.jsoar.core;
}

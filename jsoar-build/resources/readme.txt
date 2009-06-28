jsoar @JSOAR_VERSION@ is a pure Java implementation of the Soar kernel. See 
http://jsoar.googlecode.com for more information.

###############################################################################
Getting Started

Double-click @JSOAR_DEMOS_JAR@, or run from the command-line:

    > java -jar @JSOAR_DEMOS_JAR@ 

This will run the jsoar debugger. Now, if you ran @JSOAR_DEMOS_JAR@, you can load 
some Soar code from the demos directory:

* Click File->Source File...
* Select demos/towers-of-hanoi.soar

A Towers of Hanoi game visualization will be displayed. Now step the agent
with the run controls at the top of the debugger. Each decision cycle a
disk will be moved. 

jsoar's performance is pretty good, but it also loves memory, so it's probably
a good idea to bump up the size of your Java heap:

    > java -Xmx1024m -jar @JSOAR_DEMOS_JAR@
    
Additionally, JSoar performs better over time when using the -server version
of the JVM. This is only available in the JDK version of the java executable:

    > java -server -Xmx1024m -jar @JSOAR_DEMOS_JAR@

###############################################################################
Contents of this distribution:

   lib/jsoar-core-x.x.x.jar - Core jsoar core including Soar kernel
   lib/jsoar-debugger-x.x.x.jar - jsoar debugger
   lib/jsoar-sml-x.x.x.jar - Unfinished jsoar implementation of SML API.
   lib/jsoar-demos-x.x.x.jar - jsoar demos
      
   license.txt - the jsoar BSD license
   
   perftimer.bat - a script used to do performance testing on jsoar
   
   licenses/ - licenses for 3rd party libraries used by jsoar
   
   doc/ - jsoar API documenation
      
   demos/ - Demonstration agents

   ruby/ - Ruby API for jsoar


jsoar @JSOAR_VERSION@ is a pure Java implementation of the Soar kernel. See 
http://jsoar.googlecode.com for more information.

###############################################################################
Getting Started

Double-click @JSOAR_COMPLETE_JAR@, or run from the command-line:

    > java -jar @JSOAR_COMPLETE_JAR@ 

This will run the jsoar debugger. Now you can load some Soar code:

* Click File->Source File...
* Select demos/towers-of-hanoi.soar

A Towers of Hanoi game visualization will be displayed. Now step the agent
with the run controls at the top of the debugger. Each decision cycle a
disk will be moved. 

jsoar's performance is prett good, but it also loves memory, so it's probably
a good idea to bump up the size of your Java heap:

    > java -Xmx1024m -jar @JSOAR_COMPLETE_JAR@

The working memory graph is a purely experimental visualization which will
probably be removed at some point. Don't be surprised if it crashes the debugger,
especially on large working memories.

###############################################################################
Contents of this distribution:

   @JSOAR_COMPLETE_JAR@ - the jsoar jar including all third-party libraries.
      Download and build from SVN (http://jsoar.googlecode.com/svn/trunk/) to
      build a minimal jar with separate dependencies.
      
   license.txt - the jsoar BSD license
   
   perftimer.bat - a script used to do performance testing on jsoar
   
   licenses/ - licenses for 3rd party libraries used by jsoar
   
   doc/ - jsoar API documenation
      
   demos/ - Demonstration agents

jsoar @JSOAR_VERSION@ is a pure Java implementation of the Soar kernel. See 
http://jsoar.googlecode.com for more information.

###############################################################################
Getting Started

Double-click bin/jsoar.bat (or bin/jsoar), or run from the command-line:

    > \path\to\jsoar\bin\jsoar    (Windows)
    $ /path/to/jsoar/bin/jsoar    (Unix)

This will run the JSoar debugger. Note, on Unix systems, it may be necessary
to add the executable permission to the startup script:

    $ chmod +x /path/to/jsoar/bin/jsoar

Now you can load some Soar code from the demos directory:

* Click File->Source File...
* Select demos/towers-of-hanoi.soar

A Towers of Hanoi game visualization will be displayed. Now step the agent
with the run controls at the top of the debugger. Each decision cycle a
disk will be moved. 

This also starts Legilimens, JSoar's web interface. Try opening
http://localhost:12122/jsoar/agents in your browser and poke around.

JSoar's performance is pretty good, but it also loves memory, so it's probably
a good idea to bump up the size of your Java heap with the -Xmx option.
    
Additionally, JSoar performs better over time when using the -server version
of the JVM. This is only available in the JDK version of the java executable.

###############################################################################
Contents of this distribution:

   lib/jsoar-core-x.x.x.jar - Core jsoar core including Soar kernel
   lib/jsoar-debugger-x.x.x.jar - jsoar debugger
   lib/jsoar-sml-x.x.x.jar - Unfinished jsoar implementation of SML API.
   lib/jsoar-demos-x.x.x.jar - jsoar demos
      
   license.txt - the jsoar BSD license
   
   bin/jsoar.bat - runs JSoar debugger for simple experiments in Windows
   bin/jsoar     - runs JSoar debugger for simple experiments in Unix
   
   perftimer.bat - a script used to do performance testing on jsoar
   
   licenses/ - licenses for 3rd party libraries used by jsoar
   
   doc/ - jsoar API documenation
      
   demos/ - Demonstration agents

   ruby/ - Ruby API for jsoar


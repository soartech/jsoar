@echo off
rem This is a startup script for the JSoar debugger. It's mostly "for fun"
rem because JSoar is typically used as a library. The debugger that is
rem started can be used for running simple agents with no I/O.
rem
rem Usage:
rem
rem > jsoar [soar files or URLs]

setlocal

set HERE=%~sp0

rem To enable Legilimens web interface add this to JSOAR_OPTS
rem -Djsoar.legilimens.autoStart=true

if not defined JSOAR_MAIN (
   set JSOAR_MAIN=org.jsoar.debugger.JSoarDebugger
)
if not defined JSOAR_OPTS (
   set JSOAR_OPTS=-Xmx1024m
)
if not defined JSOAR_CLASSPATH (
   set JSOAR_CLASSPATH=%HERE%..\lib\*
)

java %JSOAR_OPTS% -cp "%JSOAR_CLASSPATH%" "-Djsoar.home=%HERE%.." %JSOAR_MAIN% %*

@echo off
rem This is a startup script for SoarUnit. 
rem
rem Usage:
rem
rem > soarunit --help 

setlocal

set HERE=%~dsp0
set JSOAR_LIB=%HERE%..\lib
set JSOAR_MAIN=org.jsoar.soarunit.SoarUnit
set JSOAR_CLASSPATH=%JSOAR_LIB%\*

rem SOAR_HOME has to be set to use CSoar
if defined SOAR_HOME (
  rem Setup all the native lib junk
  set JSOAR_OPTS="-Dsoar.home=%SOAR_HOME%" "-Djava.library.path=%SOAR_HOME%\bin"
  
  rem Stick the right sml.jar at the front of the classpath
  set JSOAR_CLASSPATH=%SOAR_HOME%\share\java\sml.jar;%JSOAR_CLASSPATH%

  rem For CSoar debugger to work, bin has to be on the system path
  rem See http://code.google.com/p/soar/issues/detail?id=81
  set PATH=%SOAR_HOME%\bin;%PATH%
)

call %HERE%jsoar.bat %*

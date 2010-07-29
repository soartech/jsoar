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
set JSOAR_CLASSPATH=%JSOAR_LIB%\jsoar-soarunit-@JSOAR_VERSION@.jar;%JSOAR_LIB%\jsoar-core-@JSOAR_VERSION@.jar;%JSOAR_LIB%\jsoar-debugger-@JSOAR_VERSION@.jar;%JSOAR_LIB%\sml-9.3.0.jar

call %HERE%jsoar.bat %*

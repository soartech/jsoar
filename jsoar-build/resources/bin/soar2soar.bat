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
set JSOAR_MAIN=org.jsoar.soar2soar.Soar2Soar
call %HERE%jsoar.bat %*

@echo off
rem This is a startup script for SoarUnit. 
rem
rem Usage:
rem
rem > soarunit --help 

setlocal

set HERE=%~dsp0
set JSOAR_MAIN=org.jsoar.soarunit.SoarUnit
call %HERE%jsoar.bat %*

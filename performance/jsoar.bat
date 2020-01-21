@echo off
setlocal
if not defined JSOAR_HOME (
   set JSOAR_HOME=..\jsoar-build\dist
)
set JSOAR_MAIN=org.jsoar.kernel.PerformanceTimer
set JSOAR_OPTS=-server -Xmx512M

call "%JSOAR_HOME%\bin\jsoar.bat" %*


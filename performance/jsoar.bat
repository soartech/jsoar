echo off
if not defined JSOAR_HOME (
   set JSOAR_HOME=..\jsoar-build\dist
)
call "%JSOAR_HOME%\perftimer.bat" %*


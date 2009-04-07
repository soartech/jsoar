@echo off
rem Sources a Soar file and runs it until it halt. Then prints timing stats and
rem exits.
rem Usage: perftimer.bat soarfile

set HERE=%~sp0
set ARGS=-Xmx1000M -cp "%HERE%lib\*" org.jsoar.kernel.PerformanceTimer

if defined JAVA_HOME (
   if exist "%JAVA_HOME%\bin\javac.exe" (
      echo **** Running -client java **************************************
      "%JAVA_HOME%\bin\java" %ARGS% %*
      echo **** Running -server java **************************************
      "%JAVA_HOME%\bin\java" -server %ARGS% %*
      goto alldone
   )
)
echo JAVA_HOME not set. Just running with whatever Java's on the path
java.exe %ARGS% %*

:alldone

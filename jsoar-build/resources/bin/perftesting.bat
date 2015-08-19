@echo off
rem Runs performance testing using the specified options
rem Usage: perftesting.bat -C example_configuration.yaml

set HERE=%~sp0
set JSOAR_HOME=%HERE%..
set ARGS= -cp "%JSOAR_HOME%\lib\*" org.jsoar.performancetesting.PerformanceTesting

cd %JSOAR_HOME%\performance-testing

if defined JAVA_HOME (
   if exist "%JAVA_HOME%\bin\javac.exe" (
      "%JAVA_HOME%\bin\java" %ARGS% %*
      goto alldone
   )
)
echo JAVA_HOME not set. Just running with whatever Java's on the path
java.exe %ARGS% %*

:alldone
cd %HERE%

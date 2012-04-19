echo off
setlocal
if not defined SOAR_HOME (
   set SOAR_HOME=D:\SoarSuite_9.3.2-Windows_64bit
)
copy %1 csoar.temp.soar
echo watch 0 >> csoar.temp.soar
echo stats >> csoar.temp.soar
echo run >> csoar.temp.soar
echo stats >> csoar.temp.soar
echo exit >> csoar.temp.soar

"%SOAR_HOME%\bin\TestCLI.exe" csoar.temp.soar

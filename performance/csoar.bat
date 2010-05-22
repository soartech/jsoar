echo off
setlocal
if not defined CSOAR_HOME (
   rem set CSOAR_HOME=C:\Program Files\Soar-Suite-9.0.0\SoarSuite\SoarLibrary
   set CSOAR_HOME=C:\Program Files\Soar\Soar-Suite-9.3.0-win-x86
)
copy %1 csoar.temp.soar
echo watch 0 >> csoar.temp.soar
echo stats >> csoar.temp.soar
echo run >> csoar.temp.soar
echo stats >> csoar.temp.soar
echo exit >> csoar.temp.soar

"%CSOAR_HOME%\bin\TestCLI.exe" csoar.temp.soar

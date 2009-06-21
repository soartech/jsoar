echo off
if not defined CSOAR_HOME (
   set CSOAR_HOME=C:\Program Files\Soar-Suite-9.0.0\SoarSuite
)
copy %1 csoar.temp.soar
echo watch 0 >> csoar.temp.soar
echo run >> csoar.temp.soar
echo stats >> csoar.temp.soar
echo exit >> csoar.temp.soar

"%CSOAR_HOME%\SoarLibrary\bin\TestCLI.exe" csoar.temp.soar

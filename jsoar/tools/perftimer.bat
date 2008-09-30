@echo off
rem Sources a Soar file and runs it until it halt. Then prints timing stats and
rem exits.
rem Usage: perftimer.bat soarfile

set HERE=%~sp0
java -cp %HERE%jsoar-complete.jar org.jsoar.kernel.PerformanceTimer %*
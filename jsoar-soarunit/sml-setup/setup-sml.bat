rem this file sets up the pulse pysio engine stuff in the project
rem it should only need to be run when a new version of pulse is released
rem this is windows-specific; this would need to be ported to other platforms (e.g., to copy .so's instead .dll's)

set SML_VERSION=9.6.0
set SML_BIN=%HOMEDRIVE%%HOMEPATH%\SoarTutorial_9.6.0-Multiplatform_64bit\bin\java
echo %SML_BIN%
set LOCAL_REPO=repo

rem install jar in project-specific repo, with provided pom
rem have to use cmd /C here because otherwise the bat file terminates as soon as this completes
cmd /C mvn source:jar install:install-file -U -DlocalRepositoryPath=%LOCAL_REPO% -Dfile="%SML_BIN%/sml.jar" -DpomFile=pom.xml

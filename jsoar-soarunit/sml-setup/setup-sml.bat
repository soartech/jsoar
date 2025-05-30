rem this file sets up the sml dependency in the project
rem it should only need to be run when needing to update to a new version of sml
rem this is windows-specific; this would need to be ported to other platforms to run elsewhere

set SML_VERSION=9.6.0
set SML_BIN=%HOMEDRIVE%%HOMEPATH%\SoarTutorial_9.6.0-Multiplatform_64bit\bin\java
echo %SML_BIN%
set LOCAL_REPO=repo

rem install jar in project-specific repo, with provided pom
rem have to use cmd /C here because otherwise the bat file terminates as soon as this completes
cmd /C mvn source:jar install:install-file -U -DlocalRepositoryPath=%LOCAL_REPO% -Dfile="%SML_BIN%/sml.jar" -DpomFile=pom.xml

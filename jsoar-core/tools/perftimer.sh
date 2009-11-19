#!/bin/bash
# Sources a Soar file and runs it until it halt. Then prints timing stats and
# exits.
# Usage: perftimer.bat soarfile

HERE=`pwd`
JSOAR_HOME=$HERE/..
ARGS="-Xmx1000M -cp $JSOAR_HOME/../jsoar-build/dist/lib/jsoar-core-snapshot-20091119.jar org.jsoar.kernel.PerformanceTimer"
SERVER_ARGS="-server -XX:+DoEscapeAnalysis"
if [ $JAVA_HOME ]; then
   if [ -e "${JAVA_HOME}/bin/java" ]; then
      echo "**** Running -client java **************************************"
      ${JAVA_HOME}/bin/java ${ARGS} $@
      echo "**** Running -server java **************************************"
      ${JAVA_HOME}/bin/java ${SERVER_ARGS} ${ARGS} $@
      exit
   fi
fi

echo "JAVA_HOME not set. Just running with whatever Java's on the path"
java ${ARGS} $@


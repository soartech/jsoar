#!/bin/sh
JSOAR_HOME="../jsoar-build/dist"
export JSOAR_MAIN="org.jsoar.kernel.PerformanceTimer"
export JSOAR_OPTS="-server -Xmx512M"
$JSOAR_HOME/bin/jsoar $*

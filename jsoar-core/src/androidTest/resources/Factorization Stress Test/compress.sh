#! /bin/bash

DIR=`pwd`

rm -f SingleFileFactorizationStressTest.soar

cat FactorizationStressTest.soar >> SingleFileFactorizationStressTest.soar

find Factorization\ Stress\ Test -name "*.soar" -exec cat {} >> "$DIR/SingleFileFactorizationStressTest.soar" \;

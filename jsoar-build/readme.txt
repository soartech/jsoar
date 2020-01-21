THIS FILE IS OBSOLETE

This folder is the JSoar "build" project. It includes common ANT
scripts along with additional resources that go into a release
distribution. Once you have ANT installed and JAVA_HOME set, just
run ant to create a new build:

$ ant

Note that this will always perform a full rebuild of JSoar. JSoar is
developed in Eclipse which compiles code on the fly. Thus, the ANT
build scripts are only used to build releases.  In fact, this folder
is an Eclipse project, so the ANT build can be run from inside Eclipse.

The resulting distribution will be in the dist folder. A zip file will
also be created.

To specify the version for a release:

$ ant -Dversion=X.Y.Z

To skip Javadoc generation:

$ ant -Dnodoc=true

To skip tests(?!):

$ ant -Dnotest=true

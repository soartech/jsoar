This folder is the jsoar "build" project. It includes common ANT
scripts along with additional resources that go into a release
distribution. Once you have ANT installed and JAVA_HOME set, just
run ant to create a new build:

$ ant

The resulting distribution will be in the dist folder.

Note that this will always perform a full rebuild of jsoar. jsoar is
developed in Eclipse which compiles code on the fly. Thus, the ANT
build scripts are only used to build releases.  In fact, this folder
is an Eclipse project, so the ANT build can be run from inside Eclipse.

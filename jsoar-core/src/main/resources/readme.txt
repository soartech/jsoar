jsoar-core-android can be developed in Android Studio as it uses gradle.

You will need a gradle.properties file in your .gradle folder with nexusUser and nexusPassword defined.

You may need to install an updated version of the build tools to match the buildToolsVersion in the
build.gradle file. This can be done in Android Studio by clicking Tools -> Android -> SDK Manager -> SDK Tools Tab.
Click the Show Package Details checkbox, then look under Android SDK Build Tools to see which version you have installed.

To build and upload an archive to nexus, run the following gradle command: gradlew clean build uploadArchives

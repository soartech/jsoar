[![license](https://img.shields.io/badge/license-BSD--3-green)](https://github.com/soartech/jsoar/blob/maven/LICENSE.txt)
![version](https://img.shields.io/badge/jsoar-5.0.0-blue)

[![javadoc-jsoar-core](https://img.shields.io/badge/javadoc-jsoar--core-brightgreen)](https://javadoc.io/doc/com.soartech/jsoar-core) 
[![javadoc-jsoar-tcl](https://img.shields.io/badge/javadoc-jsoar--tcl-brightgreen)](https://javadoc.io/doc/com.soartech/jsoar-tcl) 
[![javadoc-jsoar-debugger](https://img.shields.io/badge/javadoc-jsoar--debugger-brightgreen)](https://javadoc.io/doc/com.soartech/jsoar-debugger) 
[![javadoc-jsoar-soarunit](https://img.shields.io/badge/javadoc-jsoar--soarunit-brightgreen)](https://javadoc.io/doc/com.soartech/jsoar-soarunit) 
[![javadoc-jsoar-soar2soar](https://img.shields.io/badge/javadoc-jsoar--soar2soar-brightgreen)](https://javadoc.io/doc/com.soartech/jsoar-soar2soar) 
[![javadoc-jsoar-demos](https://img.shields.io/badge/javadoc-jsoar--demos-brightgreen)](https://javadoc.io/doc/com.soartech/jsoar-demos) 

![Maven Build](https://github.com/soartech/jsoar/workflows/Maven%20Build/badge.svg)

JSoar is a pure Java implementation of the Soar kernel. See the [JSoar Wiki](https://github.com/soartech/jsoar/wiki) for more information. The [User's Guide](https://github.com/soartech/jsoar/wiki/JSoarUsersGuide) is a good place to start.

## Obtaining JSoar ##

To add a dependency on JSoar using Maven or gradle, include the following dependencies as needed. A typical project may include jsoar-core, jsoar-debugger, jsoar-tcl, and jsoar-soarunit.

We no longer provide pre-built binaries on github, but they can be downloaded from maven central if desired. Building from source is also straightforward (see below).

JSoar Core (required):
* Maven
```
<dependency>
    <groupId>com.soartech</groupId>
    <artifactId>jsoar-core</artifactId>
    <version>${jsoar.version}</version>
</dependency>
```
* Gradle
```
dependencies {
    compile 'com.soartech:jsoar-core:$jsoarVersion'
}
```

[Support for Tcl in Soar code](https://github.com/soartech/jsoar/wiki/JSoarTclSupport):
* Maven
```
<dependency>
    <groupId>com.soartech</groupId>
    <artifactId>jsoar-tcl</artifactId>
    <version>${jsoar.version}</version>
</dependency>
```
* Gradle
```
dependencies {
    compile 'com.soartech:jsoar-tcl:$jsoarVersion'
}
```

[JSoar debugger](https://github.com/soartech/jsoar/wiki/JSoarDebugger):
* Maven
```
<dependency>
    <groupId>com.soartech</groupId>
    <artifactId>jsoar-debugger</artifactId>
    <version>${jsoar.version}</version>
</dependency>
```
* Gradle
```
dependencies {
    compile 'com.soartech:jsoar-debugger:$jsoarVersion'
}
```

[SoarUnit](https://github.com/soartech/jsoar/wiki/SoarUnit):
* Maven
```
<dependency>
    <groupId>com.soartech</groupId>
    <artifactId>jsoar-soarunit</artifactId>
    <version>${jsoar.version}</version>
</dependency>
```
* Gradle
```
dependencies {
    compile 'com.soartech:jsoar-soarunit:$jsoarVersion'
}
```

[Remote Web-Based Debugging Support](https://github.com/soartech/jsoar/wiki/JSoarLegilimens):
* Maven
```
<dependency>
    <groupId>com.soartech</groupId>
    <artifactId>jsoar-legilimens</artifactId>
    <version>${jsoar.version}</version>
</dependency>
```
* Gradle
```
dependencies {
    compile 'com.soartech:jsoar-legilimens:$jsoarVersion'
}
```

[Soar2Soar](https://derbinsky.info/public/_custom/research/misc/talks/soar2soar_soarworkshop_2010.pdf):
* Maven
```
<dependency>
    <groupId>com.soartech</groupId>
    <artifactId>jsoar-soar2soar</artifactId>
    <version>${jsoar.version}</version>
</dependency>
```
* Gradle
```
dependencies {
    compile 'com.soartech:jsoar-soar2soar:$jsoarVersion'
}
```

JSoar Demos has a number of examples of using JSoar that you can look at; it probably doesn't make sense to depend on this:
* Maven
```
<dependency>
    <groupId>com.soartech</groupId>
    <artifactId>jsoar-demos</artifactId>
    <version>${jsoar.version}</version>
</dependency>
```
* Gradle
```
dependencies {
    compile 'com.soartech:jsoar-demos:$jsoarVersion'
}
```

## Developer info ##

Note that the maven branch is now the main branch. The master branch is no longer maintained.

There is also an Android branch, which maintains an Android-compatible version.

### Coding Conventions ###

The coding conventions for the JSoar codebase are stored as Eclipse formatter rules in `eclipse-formatter.xml`. To import:

    Window -> Preferences -> Java -> Code Style -> Formatter -> Import ...

The basic rules are:
* NO TABS
* 4 spaces of indentation
* Opening braces on their own line

### Maven Build ###

Builds are performed from the root directory.

* To just do a build, run `mvn package`
* To do a build and install, which includes generating javadocs, run `mvn install`
* To just build javadocs, run `mvn javadoc:aggregate`

Jars will end up in the target directories of the various projects (and in your local .m2 cache, if you install). Javadocs will end up in the top-level target/site directory.

### Maven Releases ###

To create a release using the Maven Release plugin:

* Make sure you have a github account that has permission to commit to the jsoar repo.
* Make sure you have ossrh server setup in your settings.xml with the soartech-releases account.
* To build using the poms as-is, you should use a Java 8 JDK. More recent JDKs will work, but without additional changes to the poms, they may not produce jar files that can be linked with Java 8 applications.

* Make sure everything is fully merged and all commits are pushed.
* `mvn -Dusername=<yourGithubUsername> release:prepare -DdryRun=true`
	* This runs through all the questions (if you're not sure, accept all the default answers), make sure everything builds, and creates temp versions of the pom files showing what changes will be made.
    * If a prompt doesn't pop up for your signing password, you may have to force gpg/kleopatra to wake up. Try executing this from the command line: `echo "test" | gpg --clearsign`. If that works, you should be good to go. If not, then something is probably wrong with your gpg install.
* If everything looks good in the dry run:
	* `mvn -Dusername=<yourGithubUsername> release:clean`
		* This removes all the temp files from the dry run
	* `mvn -Dusername=<yourGithubUsername> release:prepare`
		* You will answer all the questions again	
		* This will change the current poms to the release version, commit and tag that, and then change the poms to the next snapshot version and commit that. Then it will push the commits to github.
	* `mvn -Dusername=<yourGithubUsername> release:perform`
		* This will build everything and upload the release jars to Maven Central's staging repo (make sure you have credentials for `soartech-releases` setup in your maven settings.xml). They should be accessible in Maven Central proper some time after that (if automated, which it currently is not, nominally 20 mins, but it could take hours. Otherwise the release must be done manually). There's no need to wait for things to show up on Maven Central before continuing.
			* Manual promotion from the staging repo: log into `https://oss.sonatype.org/#stagingRepositories`, check the Content tab and make sure it looks good, then click the Close button at the top. Once closing has completed all of its checks it will be closed (you may have to refresh to see this). Then click the Release button at the top put it onto Maven Central. It may take a little while for it to actually appear in search results, and longer for the online javadocs to update.
	* `mvn deploy`
		* This will put the new snapshot on SoarTech's nexus (assuming you have the proper setup to access that, which all SoarTech employees should have).

If something goes wrong when running any of the `release` commands, you can try `mvn -Dusername=<yourGithubUsername> release:rollback`, which will attempt to undo the changes. This should work as long as you haven't done a `release:clean`. For more info, see the [documentation](https://maven.apache.org/maven-release/maven-release-plugin/examples/rollback-release.html).

If everything is good, you can do a `mvn release:clean` to remove all the intermediate files that the release plugin created. These should definitely not be committed.

Don't forget to update the version badge at the top of the readme.

# Acknowledgments / History
JSoar was originally envisioned and implemented by Dave Ray (and indeed, the vast majority of the code is still Dave's). JSoar started out on Google Code in SVN, was converted to Mercurial and then Git, and then moved to [Dave's github site](https://github.com/daveray/). Today JSoar is primarily maintained by Soar Technology, Inc.

JSoar is a pure Java implementation of the Soar kernel. See [https://github.com/soartech/jsoar/](https://github.com/soartech/jsoar/wiki) for more information.

Note that the maven branch is now the main branch. The master branch is no longer maintained.

## Coding Conventions ##

The coding conventions for the JSoar codebase are stored as Eclipse formatter rules in `eclipse-formatter.xml`. To import:

    Window -> Preferences -> Java -> Code Style -> Formatter -> Import ...

The basic rules are:
* NO TABS
* 4 spaces of indentation
* Opening braces on their own line

# Maven Build #

Builds are performed from the root directory.

* To just do a build, run `mvn package`
* To do a build and install, which includes generating javadocs, run `mvn install`
* To just build javadocs, run `mvn javadoc:aggregate`

Jars will end up in the target directories of the various projects (and in your local .m2 cache, if you install). Javadocs will end up in the top-level target/site directory.

# Maven Releases #

TODO: Consolidate build instructions.

To create a release using the Maven Release plugin:

* Make sure `git` can authenticate to the SoarTech server without interaction
	* Does `git pull` prompt you for a username or password? If so, you need to change that.
	* This is not technically required but if you type your password wrong, below, it will cancel the entire process.
* Make sure everything is fully merged and all commits are pushed.
* Check out `maven`
* Run the release plugin: 
    * `mvn clean release:clean release:prepare release:perform deploy`

Some notes:

* The first `clean` is probably unnecessary
	* Same with `release:clean`, but they don't hurt.
* `release:prepare` pauses to interactively ask some questions. If you don't know what you're doing, the default behavior (hitting enter in response to all prompts) does the following:
	* Removes `-SNAPSHOT` from all of the JSoar components, and updates all component dependencies (scoped to JSoar components only)
	* Commits the changed poms, tags this with `jsoar-VERSION` which is whatever the version was minus `-SNAPSHOT`, pushes this to the repository
	* Increments the micro and adds `-SNAPSHOT` back to everything it just removed it from, changing the poms again, committing them, and pushing the change to the repository
* `release:perform` creates and deploys the artifacts
* `deploy` deploys the new snapshot, which is pretty much identical to the artifact just built

# Acknowledgements / History
JSoar was originally envisioned and implemented by Dave Ray (and indeed, the 
vast majority of the code is still Dave's). JSoar started out on Google Code in
SVN, was converted to Mercurial and then Git, and then moved to [Dave's github site](https://github.com/daveray/). Today JSoar is primarily maintained by Soar Technology, Inc.

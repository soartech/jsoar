# JSoar Quick Help #
This document is intended as a quick reference, most useful to new Soar users.

## Common Debugging Commands ##

Many of the commands follow a parent command / sub command structure, with options and arguments following. In many cases, there are aliases for common combinations. Most commands abd subcommands include a help subcommand that describes the command. Here is a summary of some of the most useful commands; most of what's here also applies to CSoar:

* `help` : With no args, lists all the commands. Optionally takes the name of a command and prints its help. Note that most commands have a help subcommand, and most subcommands have help sub-subcommands.
	* Examples `help`, `help soar`, `soar help`, `soar init help`
* `print` : Given the name of a rule, prints the rule. Given an identifier, prints the working memory structure rooted in that identifier. Can also take patterns.
	* Examples:
		* `print my*rule`
		* `print s1` : note that identifiers are not case sensitive
		* `print s1 --depth 3 --tree` : print to a specified depth in a tree format
		* `print (* ^input-link *)` : print identifiers that have an input-link attribute. Any pattern may be specified, with `*` for wildcards. Note this will print all the children of the identifier(s), not just the ones that match the pattern. To get only the ones that match the pattern, add the `--exact` option. Also see the WME Search panel below for a graphical way to execute this command.
	* Alias: `p`
* `run` : Runs Soar for the specified number of steps in the specified units
	* Examples:
		* `run -d` : run one decision
		* `run 12 -e` : run 12 smallest steps
	* Aliases: `step` = `run -d`
* `soar stop` : Stop the Soar agent at the next stop point
	* Alias: `stop`
* `soar init` : Reinitialize the Soar agent (i.e., clear working memory, reset stats, but do not change production memory)
	* Alias: `init`
* `trace` : Set the verbosity of the trace output when Soar is running. Default levels 0-5 correspond to common combinations of settings. Default is level 1.
	* Examples:
		* `trace 0` : Minimal output
		* `trace 5` : Very verbose output
		* `trace --wmes` : Print when WMEs are added to or removed from working memory
	* Alias: `watch`
* `production matches` : Print whether a rule matches, and how many times. If a rule doesn't match, shows the first failing condition. Also shows number of partial matches, which is useful for performance tuning.
	* Examples:
		* `matches my*rule`
	* Alias: `matches`
* `production firing-counts` : Prints the number of times each rule has fired, or a specific rule (if specified), or the top n rules, if given a numeric argument.
	* Examples:
		* `production firing-counts` : Shows the number of times every rule has fired.
		* `production firing-counts my*rule` : Shows the number of times my*rule has fired.
		* `production firing-counts 10` : Shows the number of times the 10 most-fired rules have fired.
	* Aliases: `firing-counts`, `fc`
* `preferences` : Shows whether a WME has i-support or o-support and, if an operator, preferences and selection probabilities. Can also show which rule(s) created a WME.
	* Examples:
		* `preferences` : Shows operator preferences for bottom-most state
		* `preferences s1` : Shows operator preferences for state S1
		* `preferences s1 ^foo --names` : Shows which rules created the WMEs rooted in s1 ^foo
	* Alias: `pref`
* `alias` : Add a command alias, or list existing aliases. An alias is executed as if it were the command it represents. Note that while there are a number of default aliases, any new aliases added are not automatically saved anywhere. It is recommended that new aliases be added to your Soar code directly so they are automatically defined when your agent is loaded. Note that aliases cannot currently be removed once defined, but they may be replaced.
	* Examples:
		* `alias` : Lists existing command aliases.
		* `alias foo print s1` : Creates (or replaces) an alias, `foo`, for the command `print s1`.

## Debugger Tips ##

### Changing the Zoom ###

* The default scale for JDK8 can be very tiny on high resolution displays.
* The default scale for JDK9+ is much better on high resolution displays.
* The default can be changed at startup time by setting this property on the command line: `-DfontScale=<scale>` where 1.0 is the default, and higher numbers are larger.
* The scale can be dynamically changed at runtime via `Ctrl+scrollwheel`. Note that this is imperfect, but may be good enough in many cases.

### Change the Panels ###
* There are many panels available for displaying various information, and not all are shown by default.
* Additional panels can be found under the `View` menu.
* The Working Memory panel gives a graphical tree view of working memory. Scroll by dragging the empty space in the panel.
* The WME Search panel allows you to specify a pattern to look for, and lists matching WMEs. Note that this can also be achieved via the `print --exact` command (see the example under the `print` command above).
* The Stop Command panel allows you to specify a command to execute when Soar stops (e.g., to print some location in working memory). You can create multiple of these.
* Note that the panels can be re-arranged, and layouts may be saved/loaded (see the `View` -> `Layout` menu).

## Other Resources ##

* JSoar wiki: https://github.com/soartech/jsoar/wiki
* Soar Manual
	* Included with the CSoar release
	* Can be downloaded from this page: https://soar.eecs.umich.edu/articles/articles
* Soar help: soar-help@lists.sourceforge.net


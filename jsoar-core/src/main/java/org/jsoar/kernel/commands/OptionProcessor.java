package org.jsoar.kernel.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

/**
 * Option processor utility for Soar command line interface. Very similar to
 * csoar option processor. Intended usage: create, register options, loop:
 * process lines, has, getArgument. Each call to process lines resets the
 * internal state but does not unregister options.
 * 
 * Long options should be all lower-case letters and dashes. Short options
 * should be a single upper- or lower- case letter.
 * 
 * TODO: Other exception types instead of RuntimeException
 * 
 * TODO: tests!!!!!!!!!!
 * 
 * TODO: upper/lower-case enforcement
 * 
 * TODO: partial matches on long options
 * 
 * TODO: potentially rewrite using buffers or something to avoid so many string
 * objects
 * 
 * TODO: potentially allow --option=arg and -o=arg syntax (right now the equal
 * is not dropped)
 * 
 * @author Jonathan Voigt <voigtjr@gmail.com>
 * 
 * @param <E>
 *            user-defined option reference object
 */
public class OptionProcessor<E>
{
    /**
     * Factory method.
     * 
     * @param <E>
     *            user-defined option reference object
     * @return New option processor instance.
     */
    public static <E> OptionProcessor<E> create()
    {
        return new OptionProcessor<E>();
    }

    /**
     * Argument type, denotes whether or not the option takes an argument or
     * not, or if it is optional.
     * 
     * @author voigtjr
     * 
     */
    public enum ArgType
    {
        NONE, REQUIRED, OPTIONAL,
    }

    private class Option
    {
        Option(E option, ArgType type)
        {
            this.option = option;
            this.type = type;
        }

        E option;

        ArgType type;

        @Override
        public String toString()
        {
            return option.toString() + "(" + type.toString() + ")";
        }
    }

    private final Map<String, Option> shortOptions = new HashMap<String, Option>();

    private final Map<String, Option> longOptions = new HashMap<String, Option>();

    private final Map<E, String> arguments = new HashMap<E, String>();

    /**
     * Registers an option that does not have an argument, and uses the first
     * character of the long option as its short option.
     * 
     * @param option
     *            User key to refer to this option later.
     * @param longOption
     *            Long option, lower-case, omit initial dashes.
     * @throws IllegalArgumentException
     *             If there is an option name collision.
     */
    public void registerOption(E option, String longOption)
            throws IllegalArgumentException
    {
        registerOption(option, longOption, longOption.substring(0, 1));
    }

    /**
     * Registers an option that does not have an argument.
     * 
     * @param option
     *            User key to refer to this option later.
     * @param longOption
     *            Long option, lower-case, omit initial dashes.
     * @param shortOption
     *            Short option (one letter), no dash.
     * @throws IllegalArgumentException
     *             If there is an option name collision.
     */
    public void registerOption(E option, String longOption, String shortOption)
            throws IllegalArgumentException
    {
        registerOption(option, longOption, shortOption, ArgType.NONE);
    }

    /**
     * Register an option. TODO: case and dash enforcement
     * 
     * @param option
     *            User key to refer to this option later.
     * @param longOption
     *            Long option, lower-case, omit initial dashes.
     * @param shortOption
     *            Short option (one letter), no dash.
     * @param type
     *            Specify if the option takes an argument or not.
     * @throws IllegalArgumentException
     *             If there is an option name collision.
     */
    public void registerOption(E option, String longOption, String shortOption,
            ArgType type) throws IllegalArgumentException
    {
        Option prev = shortOptions.put(shortOption, new Option(option, type));
        if (prev != null)
            throw new IllegalArgumentException(
                    "Already have a short option using -" + shortOption + ": "
                            + prev.option);
        prev = longOptions.put(longOption, new Option(option, type));
        if (prev != null)
        {
            shortOptions.remove(shortOption);
            throw new IllegalArgumentException(
                    "Already have a long option using --" + longOption + ": "
                            + prev.option);
        }
    }

    /**
     * Evaluate a command line. Assumes first arg is command name (ignored).
     * 
     * Short options are preceded by one dash and may be combined together.
     * "-fo" is equivalent to "-f -o". If an option takes an argument, the rest
     * of the token is checked and used before checking for and using the next
     * argument. For example, if "-f" takes an argument, "-fo" means "o" is the
     * argument for "-f" and is equivalent to "-f o"
     * 
     * Long options are preceded by two dashes and must be by themselves. The
     * next argument is used for the option argument if the option takes an
     * argument.
     * 
     * Optional arguments are tricky and could be implemented a few ways. This
     * implementation assumes that if there is any argument following an
     * argument with an optional argument, that it is that option's argument.
     * For example, if --foo takes an optional argument, then "--foo --bar" will
     * mean that "--bar" is interpreted as an argument to the "--foo" option,
     * and not its own option.
     * 
     * Non-option arguments are collected and returned in a new list in the same
     * order they are encountered.
     * 
     * TODO: more documentation, examples
     * 
     * @param args
     *            Command line, first arg is command name (ignored).
     * @return Only non-option arguments in same order as on the original
     *         command line.
     */
    public List<String> process(List<String> args)
    {
        arguments.clear();

        List<String> nonOptionArgs = new ArrayList<String>();

        if (args.isEmpty())
            return nonOptionArgs;

        Iterator<String> iter = args.iterator();
        // assume first argument is command name
        iter.next();
        boolean done = false;
        while (iter.hasNext())
        {
            String arg = iter.next();

            if (!done && arg.length() > 1 && arg.charAt(0) == '-')
            {
                if (arg.charAt(1) == '-')
                {
                    if (arg.length() == 2)
                    {
                        // stop processing args
                        done = true;
                    }
                    else
                    {
                        Option option = resolveLongOption(arg.substring(2));
                        if (option == null)
                            throw new RuntimeException("Unknown option: " + arg);

                        processOption(option, arg, iter);
                    }
                    continue;
                }
                else
                {
                    if (!isNumber(arg))
                    {
                        for (int i = 1; i < arg.length(); ++i)
                        {
                            Option option = resolveShortOption(arg.substring(i));
                            if (option == null)
                                throw new RuntimeException("Unknown option: "
                                        + arg);

                            boolean consumedArg = processOption(option, arg,
                                    iter, i);
                            if (consumedArg)
                                break;
                        }
                        continue;
                    }
                }
            }
            nonOptionArgs.add(arg);
        }

        return nonOptionArgs;
    }

    private boolean processOption(Option option, String arg,
            Iterator<String> iter)
    {
        return processOption(option, arg, iter, -1);
    }

    private boolean processOption(Option option, String arg,
            Iterator<String> iter, int at)
    {
        boolean consumedArg = false;
        if (option.type == ArgType.NONE)
            arguments.put(option.option, null);
        else
        {
            consumedArg = true;

            // This block of code here allows arguments appended directly to
            // options
            String optArg = null;
            if (at >= 0)
            {
                if (at + 1 < arg.length())
                    optArg = arg.substring(at + 1);
            }
            if (optArg == null)
            {
                optArg = iter.hasNext() ? iter.next() : null;
            }

            if (option.type == ArgType.REQUIRED)
                if (optArg == null)
                    throw new RuntimeException("Option requires argument: "
                            + arg);

            arguments.put(option.option, optArg);
        }
        return consumedArg;
    }

    private boolean isNumber(String arg)
    {
        try
        {
            Integer.parseInt(arg);
            return true;
        }
        catch (NumberFormatException e)
        {
        }
        try
        {
            Double.parseDouble(arg);
            return true;
        }
        catch (NumberFormatException e)
        {
        }
        return false;
    }

    private Option resolveLongOption(String arg)
    {
        return longOptions.get(arg);
    }

    private Option resolveShortOption(String arg)
    {
        if (!Character.isLetter(arg.charAt(0)))
            throw new RuntimeException("Short option is not a letter: "
                    + arg.charAt(0));
        return shortOptions.get(arg.substring(0, 1));
    }

    /**
     * Test to see if the most recent invocation of process uncovered the given option.
     * 
     * @param option Option to test for.
     * @return True if it was successfully encountered.
     */
    public boolean has(E option)
    {
        return arguments.containsKey(option);
    }

    /**
     * Get an option's argument.
     * 
     * @param option Option who's argument needs retrieval
     * @return The option's argument as a string.
     */
    public String getArgument(E option)
    {
        return arguments.get(option);
    }

    public static void main(String[] args)
    {
        // quick sanity test
        OptionProcessor<String> op = OptionProcessor.create();
        op.registerOption("no", "no");
        op.registerOption("required", "required", "r", ArgType.REQUIRED);
        op.registerOption("optional", "optional", "o", ArgType.OPTIONAL);

        // watch op.arguments in debugger as you step through these lines
        op.process(Lists.newArrayList("awesome"));
        op.process(Lists.newArrayList("awesome", "-n"));
        op.process(Lists.newArrayList("awesome", "-n", "-o"));
        op.process(Lists.newArrayList("awesome", "-no"));
        op.process(Lists.newArrayList("awesome", "-nor"));
        op.process(Lists.newArrayList("awesome", "-o"));
        op.process(Lists.newArrayList("awesome", "-o", "1"));
        op.process(Lists.newArrayList("awesome", "-r", "2"));
        op.process(Lists.newArrayList("awesome", "-2"));
        op.process(Lists.newArrayList("awesome", "-2.56"));
    }

}

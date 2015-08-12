package org.jsoar.performancetesting.jsoar;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;

import org.jsoar.kernel.Agent;
import org.jsoar.performancetesting.helpers.FileFinder;

/**
 * So this is a very unique class. What this does is it tries to find
 * jsoar-core-*.jar and then load Agent from it. If it can it'll fall back on
 * the internal jsoar version.
 * 
 * @author ALT
 */
public class JSoarAgentFactory
{
    private static Class<?> agent;

    private static String label;

    private static boolean initialized = false;

    JSoarAgentFactory(String newLabel, String JSoarDirectory)
    {
        if (initialized)
            return;

        label = newLabel;

        List<Path> jsoarCorePath;

        try
        {
            jsoarCorePath = getPathToJSoarCore(JSoarDirectory);
        }
        catch (IOException e1)
        {
            System.out
                    .println("Failed to find jsoar-core.jar.  Using assert version of it!  Error: "
                            + e1);
            return;
        }

        if (jsoarCorePath.size() == 0)
        {
            System.out
                    .println("WARNING: Using internal version of JSoar since we could not find jsoar-core-*.jar");
            return;
        }

        // Load the new jsoar-core-*.jar into memory and then resolve all it's classes that we
        // use.
        URLClassLoader child;

        List<URL> urls = new ArrayList<URL>();
        for (Path p : jsoarCorePath)
        {
            try
            {
                urls.add(p.toFile().toURI().toURL());
            }
            catch (MalformedURLException e)
            {
                System.out.println("Malformed URL! " + e.getMessage());
                return;
            }
        }

        // URL[] url = { smlPath.toFile().toURI().toURL() };
        URL[] dummy = {};
        child = new URLClassLoader(urls.toArray(dummy), this.getClass()
                .getClassLoader());

        // Resolve the org.jsoar.kernel.Agent class
        try
        {
            agent = Class.forName("org.jsoar.kernel.Agent", true, child); // Resolves
                                                                          // the
                                                                          // agent
                                                                          // class
        }
        catch (ClassNotFoundException e)
        {
            System.out.println("Could not find Agent class!");
            return;
        }

        initialized = true;
    }

    /**
     * 
     * @return the label for a test. For JSoar this will be 'JSoar' and for
     *         JSoar this will be 'JSoar X' where X is the label in the
     *         configuration file.
     */
    String getLabel()
    {
        return label;
    }

    Object newAgent(String agentLabel)
    {
        if (agent != null)
        {
            try
            {
                return agent.getDeclaredConstructor(String.class).newInstance(
                        agentLabel);
            }
            catch (InstantiationException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException
                    | NoSuchMethodException | SecurityException e)
            {
                System.err
                        .println("Failed to create new JSoar agent instance: "
                                + e + ".  Returning default agent.");
                return new Agent(agentLabel);
            }
        }
        else
            return new Agent(agentLabel);
    }

    private List<Path> getPathToJSoarCore(String jsoarDirectory)
            throws IOException
    {
        // depending on the version of JSoar, jsoar-core-*.jar may be in
        // different places

        List<Path> jsoarPaths = new LinkedList<Path>();

        if (jsoarDirectory != null && jsoarDirectory.length() > 0)
        {
            String[] paths = { "/lib/", "/", "/jsoar-core/" };
            String[] files = { "jsoar-core-*.jar", "/bin/" };

            for (String path : paths)
            {
                for (String file : files)
                {
                    FileFinder finder = new FileFinder(file);

                    Files.walkFileTree(Paths.get(jsoarDirectory, path),
                            EnumSet.noneOf(FileVisitOption.class), 1, finder);

                    for (Path matchedPath : finder.getMatchedPaths())
                    {
                        if (matchedPath.getFileName().toString()
                                .contains(".src.jar") != true)
                        {
                            jsoarPaths.add(Paths.get(jsoarDirectory, path,
                                    matchedPath.getFileName().toString()));
                        }
                    }
                }
            }
        }

        return jsoarPaths;
    }
}

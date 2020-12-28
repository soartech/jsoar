package org.jsoar.performancetesting.jsoar;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.jsoar.kernel.Agent;

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

    JSoarAgentFactory(String newLabel, Path JSoarDirectory)
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

    private List<Path> getPathToJSoarCore(Path jsoarDirectory)
            throws IOException
    {
        // depending on the version of JSoar, jsoar-core-*.jar may be in
        // different places

        List<Path> jsoarPaths = new LinkedList<Path>();

        if (jsoarDirectory != null && jsoarDirectory.getNameCount() > 0)
        {
            Path[] paths = { Paths.get("/lib/"), Paths.get("/"), Paths.get("/jsoar-core/") };
            String[] filePatterns = { "jsoar-core-*.jar", "/bin/" };

            for (Path path : paths)
            {
                for (String filePattern : filePatterns)
                {
                    File dir = jsoarDirectory.resolve(path).toFile();
                    FileFilter fileFilter = new WildcardFileFilter(filePattern);
                    File[] files = dir.listFiles(fileFilter);

                    for (File matchedFile : files)
                    {
                        Path matchedPath = matchedFile.toPath();
                        if (matchedPath.getFileName().endsWith(".src.jar") != true)
                        {
                            jsoarPaths.add(matchedPath.toAbsolutePath());
                        }
                    }
                }
            }
        }

        return jsoarPaths;
    }
}

package org.jsoar.performancetesting.jsoar;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

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

    JSoarAgentFactory(String newLabel, Path jsoarCorePath) throws MalformedURLException, ClassNotFoundException
    {
        if (initialized)
            return;

        label = newLabel;


        if(!Files.exists(jsoarCorePath))
        {
            System.out.println("Failed to find jsoar-core.jar.  Using assert version of it!  Error: ");
            return;
        }

        // Load the new jsoar-core-*.jar into memory and then resolve all it's classes that we
        // use.
        URLClassLoader child;

        URL url = jsoarCorePath.toFile().toURI().toURL();

        
        child = new URLClassLoader(new URL[] {url}, this.getClass()
                .getClassLoader());

        agent = Class.forName("org.jsoar.kernel.Agent", true, child); // Resolves the agent class
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
}

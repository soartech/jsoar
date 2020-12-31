package org.jsoar.performancetesting.csoar;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * So this is a very unique class. What this does is it tries to find sml.jar
 * and then load sml.kernel from it. If it can it'll create CSoarKernelWrappers
 * with the appropriate kernel constructors otherwise it'll return a default
 * version which is entire asserts.
 * 
 * @author ALT
 */
public class CSoarKernelFactory
{
    private static Class<?> kernel;

    private static Class<?> agent;

    private static Path csoarDirectory;

    private static boolean initialized = false;

    CSoarKernelFactory(Path csoarDirectory) throws IllegalAccessException, NoSuchFieldException, ClassNotFoundException
    {
        if (initialized)
            return;

        CSoarKernelFactory.csoarDirectory = csoarDirectory;

        List<Path> smlPath = getPathToSml(csoarDirectory);
        if (smlPath.size() == 0)
        {
            System.out
                    .println("Failed to find SML.jar.  Using assert version of it!");
            return;
        }

        // Load sml.jar into memory and then resolve all it's classes that we
        // use.
        URLClassLoader child;

        List<URL> urls = new ArrayList<URL>();
        for (Path p : smlPath)
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

        URL[] dummy = {};
        child = new URLClassLoader(urls.toArray(dummy), this.getClass()
                .getClassLoader());

        kernel = Class.forName("sml.Kernel", true, child); // Resolves the kernel class

        // Resolve the sml.Agent class
        agent = Class.forName("sml.Agent", true, child); // Resolves the agent class

        // this appears to work on OpenJDK 15. Not sure about Oracle JDKs.
        String libraries = System.getProperty( "java.library.path" );

        if( libraries != null && libraries.length() != 0 )
          libraries += File.pathSeparator + csoarDirectory;
        else
          libraries = csoarDirectory.toString();

        System.setProperty("java.library.path", libraries);


        // Now load the library including all the correct paths to the native
        // libraries.
        loadSoarLibrary();

        initialized = true;
    }

    /**
     * 
     * @return A new kernel wrapper around a CSoar sml.Kernel object.
     */
    CSoarKernelWrapper CreateKernelInNewThread()
    {
        if (initialized)
        {
            try
            {
                Method method = kernel
                        .getDeclaredMethod("CreateKernelInNewThread");
                Object kernelImpl = method.invoke(null);
                return new ImplCSoarKernelWrapper(kernelImpl, kernel, agent);
            }
            catch (NoSuchMethodException | SecurityException
                    | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e)
            {
                System.out.println("Unable to load kernel!");
                return new DefaultCSoarKernelWrapper();
            }
        }
        else
            return new DefaultCSoarKernelWrapper();
    }

    /**
     * 
     * @return A new kernel wrapper around a CSoar sml.Kernel object.
     */
    CSoarKernelWrapper CreateKernelInCurrentThread()
    {
        if (initialized)
        {
            try
            {
                Method method = kernel
                        .getDeclaredMethod("CreateKernelInCurrentThread");
                Object kernelImpl = method.invoke(null);
                return new ImplCSoarKernelWrapper(kernelImpl, kernel, agent);
            }
            catch (NoSuchMethodException | SecurityException
                    | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e)
            {
                System.out.println("Unable to load kernel!");
                return new DefaultCSoarKernelWrapper();
            }
        }
        else
            return new DefaultCSoarKernelWrapper();
    }

    /**
     * 
     * @return A new kernel wrapper around a CSoar sml.Kernel object.
     */
    CSoarKernelWrapper CreateKernelInCurrentThread(boolean optimized)
    {
        if (initialized)
        {
            // 9.3.2 and later
            try
            {
                Method method = kernel.getDeclaredMethod(
                        "CreateKernelInCurrentThread", boolean.class);
                Object kernelImpl = method.invoke(null, optimized);
                return new ImplCSoarKernelWrapper(kernelImpl, kernel, agent);
            }
            catch (NoSuchMethodException | SecurityException
                    | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e)
            {
            }

            // 9.3.1 and earlier
            try
            {
                Method method = kernel.getDeclaredMethod(
                        "CreateKernelInCurrentThread", String.class,
                        boolean.class);
                Object kernelImpl = method.invoke(null, "SoarKernelSML",
                        optimized);
                return new ImplCSoarKernelWrapper(kernelImpl, kernel, agent);
            }
            catch (NoSuchMethodException | SecurityException
                    | IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e)
            {
            }

            System.out.println("Unable to load kernel!");
            return new DefaultCSoarKernelWrapper();
        }
        else
            return new DefaultCSoarKernelWrapper();
    }

    /**
     * 
     * @return the label for a test. For JSoar this will be 'JSoar X' and for
     *         CSoar this will be 'CSoar X' where X is the label in the
     *         configuration file.
     */
    Path getSoarPath()
    {
        return csoarDirectory;
    }

    private List<Path> getPathToSml(Path csoarDirectory)
    {
        // depending on the version of csoar, sml.jar may be in different places
        Path[] paths = { Paths.get("java"), Paths.get("../share/java"), Paths.get("../lib") };
        Path[] files = { Paths.get("sml.jar"), Paths.get("soar-smljava-9.3.1.jar") };

        List<Path> smlPaths = new LinkedList<Path>();

        for (Path path : paths)
        {
            for (Path file : files)
            {
                Path smlpath = csoarDirectory.resolve(path).resolve(file);
                if (Files.exists(smlpath))
                {
                    smlPaths.add(smlpath);
                }
            }
        }

        return smlPaths;
    }

    private void loadSoarLibrary()
    {
        // depending on the version of csoar, the name of the Soar library is
        // different, so try them all

        boolean foundSoar = false;
        String[] libNames = { "Soar", "ElementXML", "SoarKernelSML" };

        for (String libName : libNames)
        {
            try
            {
                System.loadLibrary(libName);
                foundSoar = true;
            }
            catch (UnsatisfiedLinkError e)
            {
            }
        }
        if (!foundSoar)
        {
            throw new UnsatisfiedLinkError("Failed to find Soar library");
        }
    }
}

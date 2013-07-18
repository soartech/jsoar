package org.jsoar.performancetesting.csoar;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * So this is a very unique class. What this does is it tries to find sml.jar
 * and then load sml.kernel from it.  If it can it'll create CSoarKernelWrappers
 * with the appropriate kernel constructors otherwise it'll return a default
 * version which is entire asserts.
 * 
 * @author ALT
 */
public class CSoarKernelFactory
{    
    private static Class<?> kernel;
    private static Class<?> agent;
    
    private static String label;
    
    private static boolean initialized = false;
        
    CSoarKernelFactory(String newLabel, String csoarDirectory)
    {        
        if (initialized)
            return;
        
        label = newLabel;
        
        // Create a new path to the CSoar root + /java/
        // This will be used to find the CSoar sml.jar file (and set java.library.path)
        Path directoryPath = FileSystems.getDefault().getPath(csoarDirectory + "/java/");
        DirectoryStream<Path> stream;
        
        try
        {
            stream = Files.newDirectoryStream(directoryPath);
        }
        catch (IOException e)
        {
            System.out.println("Failed to create new directory stream: " + e.getMessage());
            return;
        }
        
        Path smlPath = null;
        
        //Search for sml.jar in the directory
        for (Path path : stream)
        {
            String testName = path.getFileName().toString();
                        
            if (!testName.equals("sml.jar"))
                continue;
            
            smlPath = path;
            break;
        }
        
        try
        {
            stream.close();
        }
        catch (IOException e)
        {
            System.out.println("Failed to close directory stream: " + e.getMessage());
            return;
        }
        
        if (smlPath == null)
        {
            System.out.println("Failed to find SML.jar.  Using assert version of it!");
            return;
        }
        
        //Load sml.jar into memory and then resolve all it's classes that we use.
        URLClassLoader child;
        
        try
        {
            URL[] url = { smlPath.toFile().toURI().toURL() };
            child = new URLClassLoader(url, this.getClass().getClassLoader());
        }
        catch (MalformedURLException e)
        {
            System.out.println("Malformed URL! " + e.getMessage());
            return;
        }
        
        //Resolve the sml.Kernel class
        try
        {
            kernel = Class.forName("sml.Kernel", true, child); //Resolves the kernel class
        }
        catch (ClassNotFoundException e)
        {
            System.out.println("Could not find Kernel class!");
            return;
        }
        
        //Resolve the sml.Agent class
        try
        {
            agent = Class.forName("sml.Agent", true, child); //Resolves the agent class
        }
        catch (ClassNotFoundException e)
        {
            System.out.println("Could not find Agent class!");
            return;
        }
        
        //Now a hack to add the native library path
        
        // So this code has been the same through Java SE 5 and 7 however
        // because I'm reflecting into Java classes this is VERY BAD but
        // it works.  However if this breaks in the future, you can probably
        // find a solution quite easily since this is only getting one field,
        // the usr_path which has to exist.
        //
        // Method is from: http://stackoverflow.com/questions/5419039/is-djava-library-path-equivalent-to-system-setpropertyjava-library-path
        // And also from:  http://stackoverflow.com/questions/15409223/adding-new-paths-for-native-libraries-at-runtime-in-java
        // 
        // Basically Sun never wanted to fix it and yet people like me needed this feature
        // so a Sun engineer eventually posted a solution which is a bit of a hack but it
        // has worked from Java SE 5 through 7 so it is pretty safe to assume it will work
        // At least through the rest of Java SE 7 and probably thourhg 8 and 9 and however
        // many versions after 7 there are.
        //
        // - ALT
        
        //Get the field from the class loader
        Field usrPathsField = null;
        try
        {
            usrPathsField = ClassLoader.class.getDeclaredField("usr_paths");
        }
        catch (NoSuchFieldException | SecurityException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        //Set it to public
        usrPathsField.setAccessible(true);

        //get the array of paths
        String[] paths = null;
        try
        {
            paths = (String[])usrPathsField.get(null);
        }
        catch (IllegalArgumentException | IllegalAccessException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //check if the path to add is already present
        for(String path : paths) {
            if(path.equals(csoarDirectory)) {
                return;
            }
        }

        //add the new path
        final String[] newPaths = Arrays.copyOf(paths, paths.length);
        
        List<String> finalUserPaths = new ArrayList<String>();
        
        for (int i = 0;i < newPaths.length;i++)
        {
            finalUserPaths.add(newPaths[i]);
        }
                
        //Add the path to our new list
        finalUserPaths.add(csoarDirectory);
        
        //Set the path
        try
        {
            final String[] temp = finalUserPaths.toArray(new String[0]);
            usrPathsField.set(null, temp);
        }
        catch (IllegalArgumentException | IllegalAccessException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        //Now load the library including all the correct paths to the native libraries.
        System.loadLibrary("Soar");
        
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
                Method method = kernel.getDeclaredMethod("CreateKernelInNewThread");
                Object kernelImpl = method.invoke(null);
                return new ImplCSoarKernelWrapper(kernelImpl, kernel, agent);
            }
            catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
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
                Method method = kernel.getDeclaredMethod("CreateKernelInCurrentThread");
                Object kernelImpl = method.invoke(null);
                return new ImplCSoarKernelWrapper(kernelImpl, kernel, agent);
            }
            catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
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
            try
            {
                Method method = kernel.getDeclaredMethod("CreateKernelInCurrentThread", boolean.class);
                Object kernelImpl = method.invoke(null, optimized);
                return new ImplCSoarKernelWrapper(kernelImpl, kernel, agent);
            }
            catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
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
     * @return the label for a test.  For JSoar this will be 'JSoar' and for CSoar this will be 'CSoar X' where X
     * is the label in the configuration file.
     */
    String getLabel()
    {
        return label;
    }
}

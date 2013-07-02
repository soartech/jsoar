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
 * 
 * @author ALT
 *
 * So this is a very unique class. What this does is it tries to find sml.jar
 * and then load sml.kernel from it.  If it can it'll create CSoarKernelWrappers
 * with the appropriate kernel constructors otherwise it'll return a default
 * version which is entire asserts.
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
        
        try
        {
            kernel = Class.forName("sml.Kernel", true, child);
        }
        catch (ClassNotFoundException e)
        {
            System.out.println("Could not find Kernel class!");
            return;
        }
        
        try
        {
            agent = Class.forName("sml.Agent", true, child);
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
        // - ALT
        
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
        
        usrPathsField.setAccessible(true);

        //get array of paths
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
                
        finalUserPaths.add(csoarDirectory);
        
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
        
        System.loadLibrary("Soar");
        
        initialized = true;
    }
    
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
    
    String getLabel()
    {
        return label;
    }
}

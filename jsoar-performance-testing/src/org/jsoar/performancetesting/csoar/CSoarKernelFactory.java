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
import java.util.Arrays;
import java.util.HashMap;

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
    private static final Class[] parameters = new Class[] {URL.class};
    
    private static HashMap<String, Class> kernelMap = new HashMap<String, Class>();
    private static HashMap<String, Class> agentMap = new HashMap<String, Class>();
    
    private boolean initialized = false;
    
    private String label;
    
    CSoarKernelFactory(String label, String csoarDirectory)
    {
        this.label = label;
        
        if (initialized)
        {
            return;
        }
        
        if (kernelMap.containsKey(label) || agentMap.containsKey(label))
        {
            return;
        }
        
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
            kernelMap.put(label, Class.forName("sml.Kernel", true, child));
        }
        catch (ClassNotFoundException e)
        {
            System.out.println("Could not find Kernel class!");
            return;
        }
        
        try
        {
            agentMap.put(label, Class.forName("sml.Agent", true, child));
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
        final String[] newPaths = Arrays.copyOf(paths, paths.length + 1);
        newPaths[newPaths.length-1] = csoarDirectory;
        try
        {
            usrPathsField.set(null, newPaths);
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
                Method method = kernelMap.get(label).getDeclaredMethod("CreateKernelInNewThread");
                Object kernelImpl = method.invoke(null);
                return new ImplCSoarKernelWrapper(kernelImpl, kernelMap, agentMap, label);
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
                Method method = kernelMap.get(label).getDeclaredMethod("CreateKernelInCurrentThread");
                Object kernelImpl = method.invoke(null);
                return new ImplCSoarKernelWrapper(kernelImpl, kernelMap, agentMap, label);
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
                Method method = kernelMap.get(label).getDeclaredMethod("CreateKernelInCurrentThread", boolean.class);
                Object kernelImpl = method.invoke(null, optimized);
                return new ImplCSoarKernelWrapper(kernelImpl, kernelMap, agentMap, label);
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

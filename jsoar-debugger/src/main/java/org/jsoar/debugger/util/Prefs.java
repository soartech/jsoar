package org.jsoar.debugger.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

import org.jsoar.debugger.syntax.SyntaxSettings;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class Prefs extends AbstractPreferences {

    private static final String PREFS_FILENAME = "jsoar.properties";
    private static final String SYNTAX_FILENAME = "syntax.json";
    private static final String LAYOUT_FILENAME = "layout.xml";
    private static final String PREFS_PATH = System.getProperty("user.home") + "/.jsoar";

    private final TreeMap<String, String> root;
    private final TreeMap<String, Prefs> children;
    private boolean isRemoved = false;

    public Prefs(AbstractPreferences parent, String name){
        super(parent,name);

        root = new TreeMap<>();
        children = new TreeMap<>();

        try {
            sync();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void putSpi(String key, String value) {
        root.put(key,value);
        try {
            flush();
        } catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String getSpi(String key) {
        return root.get(key);
    }

    @Override
    protected void removeSpi(String key) {
        root.remove(key);
    }

    @Override
    protected void removeNodeSpi() throws BackingStoreException {
        isRemoved = true;
        flush();
    }

    @Override
    protected String[] keysSpi() throws BackingStoreException {
        return root.keySet().toArray(new String[root.keySet().size()]);
    }

    @Override
    protected String[] childrenNamesSpi() throws BackingStoreException {
        return children.keySet().toArray(new String[children.keySet().size()]);
    }

    @Override
    protected AbstractPreferences childSpi(String name) {
        Prefs prefs = children.get(name);
        if (prefs == null){
            prefs = new Prefs(this,name);
            children.put(name,prefs);
        }
        return prefs;
    }

    @Override
    protected void syncSpi() throws BackingStoreException
    {
        if (isRemoved()) return;

        final File file = getPreferencesFile();

        if (file == null || !file.exists()) return;

        synchronized (file)
        {
            Properties p = new Properties();
            try(FileInputStream fs = new FileInputStream(file))
            {
                p.load(fs);
            }
            catch (IOException e)
            {
                throw new BackingStoreException(e);
            }

            StringBuilder sb = new StringBuilder();
            getPath(sb);
            String path = sb.toString();

            final Enumeration<?> pnen = p.propertyNames();
            while (pnen.hasMoreElements()) {
                String propKey = (String) pnen.nextElement();
                if (propKey.startsWith(path)) {
                    String subKey = propKey.substring(path.length());
                    // Only load immediate descendants
                    if (subKey.indexOf('.') == -1) {
                        root.put(subKey, p.getProperty(propKey));
                    }
                }
            }
        }
    }

    private void getPath(StringBuilder sb)
    {
        final Prefs parent = (Prefs) parent();
        if (parent == null) return;

        parent.getPath(sb);
        sb.append(name()).append('.');
    }

    @Override
    protected void flushSpi() throws BackingStoreException
    {
        final File file = getPreferencesFile();
        if(file == null) {
            return;
        }
        
        synchronized (file)
        {
            Properties p = new Properties();
            
            StringBuilder sb = new StringBuilder();
            getPath(sb);
            String path = sb.toString();
            
            try(FileInputStream fis = new FileInputStream(file))
            {
                if (file.exists()) {
                    p.load(fis);
                }
            }
            catch (IOException e)
            {
                throw new BackingStoreException(e);
            }

            List<String> toRemove = new ArrayList<>();

            // Make a list of all direct children of this node to be removed
            final Enumeration<?> pnen = p.propertyNames();
            while (pnen.hasMoreElements()) {
                String propKey = (String) pnen.nextElement();
                if (propKey.startsWith(path)) {
                    String subKey = propKey.substring(path.length());
                    // Only do immediate descendants
                    if (subKey.indexOf('.') == -1) {
                        toRemove.add(propKey);
                    }
                }
            }

            // Remove them now that the enumeration is done with
            for (String propKey : toRemove) {
                p.remove(propKey);
            }

            // If this node hasn't been removed, add back in any values
            if (!isRemoved) {
                for (String s : root.keySet()) {
                    p.setProperty(path + s, root.get(s));
                }
            }

            try(FileOutputStream fos = new FileOutputStream(file)) {
                p.store(fos, "JSoar Debugger Preferences");
            }
            catch (IOException e)
            {
                throw new BackingStoreException(e);
            }
        }
    }

    private static File getPreferencesFile() {
        try {
            boolean success = true;
            File file = new File(PREFS_PATH);
            if (!file.exists()) {
                success = file.mkdir();
            }
            file = new File(file, PREFS_FILENAME);
            if (!file.exists()) {
                success &= file.createNewFile();
            }
            if (success)
                return file;
            return null;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean storeSyntax(SyntaxSettings syntax) {
        try {
            boolean success = true;
            File file = new File(PREFS_PATH);
            if (!file.exists()) {
                success = file.mkdir();
            }
            file = new File(file, SYNTAX_FILENAME);
            if (file.exists()) {
                success &= file.delete();
            }
            success &= file.createNewFile();
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(file, syntax);
            return success;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static SyntaxSettings loadSyntax() {
        try {
            File file = new File(PREFS_PATH);
            if (!file.exists())
                return null;

            file = new File(file, SYNTAX_FILENAME);
            if (!file.exists())
                return null;

            ObjectMapper mapper = new ObjectMapper();
            TypeReference<SyntaxSettings> highlightsType = new TypeReference<SyntaxSettings>() {
            };
            return mapper.readValue(file, highlightsType);
        } catch (IOException ignored) {
            System.out.println("Error loading syntax, reverting to defaults");
        }
        return null;
    }

    public static SyntaxSettings loadDefaultSyntax() {
        try {
            InputStream resource = Prefs.class.getResourceAsStream("/org/jsoar/debugger/defaultsyntax.json");
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<SyntaxSettings> highlightsType = new TypeReference<SyntaxSettings>() {
            };
            return mapper.readValue(resource, highlightsType);

        } catch (IOException ignored) {

        }
        return new SyntaxSettings();
    }

    public static File getLayoutFile() throws IOException
    {
        @SuppressWarnings("unused")
        boolean success = true;
        File file = new File(PREFS_PATH);
        if (!file.exists()) {
            success = file.mkdir();
        }
        file = new File(file, LAYOUT_FILENAME);
        if (!file.exists()) {
            success &= file.createNewFile();
        }
        return file;
    }
}

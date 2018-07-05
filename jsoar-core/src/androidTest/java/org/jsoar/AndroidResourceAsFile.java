package org.jsoar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AndroidResourceAsFile {

    /*
    This is borrowed from https://stackoverflow.com/questions/34709210/creating-a-file-object-from-a-resource
    This is being used to test file based methods by loading included resources as files.  Note that it is in
    the test package.  Do NOT use this for production code.  Even production code you think is temporary.  Even
    not-supposed-to-be-production code that you're "just testing" with.  Trust me, it isn't worth it.  Look up
    android resource loading and how it's actually supposed to work.  It will take less time than getting this
    hack to work and having to fix it later.
     */

    private static final AndroidResourceAsFile self = new AndroidResourceAsFile();

    public static File resourceToTempFile(String resource){
        return self.resourceToTempFileInternal(resource);
    }
    public static File resourceToTempFile(InputStream inputStream, String resource){
        return self.resourceToTempFileInternal(inputStream, resource);
    }

    private File resourceToTempFileInternal(String resource) {
        InputStream inputStream = this.getClass().getResourceAsStream(resource);
        return resourceToTempFileInternal(inputStream, resource);
    }

    private File resourceToTempFileInternal(InputStream inputStream, String resource) {
        try {
            File tempFile = File.createTempFile(resource, "tmp");
            copyFile(inputStream, new FileOutputStream(tempFile));

            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException("Can't create temp file ", e);
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }
}

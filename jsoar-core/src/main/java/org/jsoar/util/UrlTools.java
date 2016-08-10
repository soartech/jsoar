package org.jsoar.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class UrlTools
{
    private static final Logger logger = LoggerFactory.getLogger(UrlTools.class);
    
    /**
     * This normalizes URLs by converting them to URIs and using the URI normalization method
     * Unlike the standard URI normalization method, it also handles paths inside jars properly
     * 
     * TODO: Should catch and rethrow different exceptions, like {@link XmlTools}?
     * 
     * @param url
     * @return
     * @throws URISyntaxException
     * @throws MalformedURLException
     */
    public static URL normalize(URL url) throws URISyntaxException, MalformedURLException
    {
        // if loading resources from within a jar file, need to normalize the path
        // unfortunately, the URI normalization method doesn't work on jar paths (because they are opaque)
        // so we have to extract the part we want, normalize that, and reinsert it
        URI uri = url.toURI();

        if(uri.getScheme().equals("jar"))
        {
            logger.debug("uri: " + uri);
            // Suppose you have a URI: jar:file:test.jar!/./test2.soar
            URI ssp1 = new URI(uri.getSchemeSpecificPart());
            // ssp1: file:test2.jar!/./test2.soar
            logger.debug("ssp1: " + ssp1);
            URI ssp2 = new URI(ssp1.getSchemeSpecificPart());
            // ssp2: test2.jar!/./test2.soar
            logger.debug("ssp2: " + ssp2);
            String sspScheme = ssp1.getScheme();
            // sspScheme: file
            logger.debug("scheme: " + sspScheme);
            URI normalizedSsp2 = new URI(ssp2.getSchemeSpecificPart()).normalize();
            // normalizedSsp2: test2.jar!/test2.soar
            logger.debug("normalzied ssp2: " + normalizedSsp2);
            URI normalized = new URI("jar:" + sspScheme + ":" + normalizedSsp2);
            logger.debug("normalized: " + normalized);
            // normalized: jar:file:test2.jar!/test2.soar
            url = normalized.toURL();
        }
        else
        {
            url = uri.normalize().toURL();
        }
        
        return url;
    }

    /**
     * Get the parent of a url.
     *
     * @param url the url
     * @return the parent of the url, never {@code null}
     */
    public static URL getParent(URL url) throws Exception
    {
        URI uri = url.toURI().getPath().endsWith("/") ? url.toURI().resolve("..") : url.toURI().resolve(".");
        return uri.toURL();
    }

    /**
     * If url is a classpath url, see {@link UrlTools#isClassPath(String)}, return a URL to its location.
     * @param url the string of the classpath url
     * @return the url to the classpath resource.
     */
    public static URL lookupClassPathURL(String url) throws IOException
    {
        final PathMatchingResourcePatternResolver resolverSoarUnit = new PathMatchingResourcePatternResolver();
        if (isClassPath(url))
        {
            List<Resource> resources = Arrays.asList(resolverSoarUnit.getResources(url));
            if (!resources.isEmpty())
            {
                return resources.get(0).getURL();
            }
        }
        throw new IOException("Invalid classpath resource: " + url);
    }

    /**
     * Determines if this is a classpath URL or not.
     */
    public static boolean isClassPath(String url)
    {
        return url.startsWith("classpath:");
    }

    /**
     * Converts a file: URL to a file object, if possible.
     * @param url url to convert
     * @return {@link java.io.File} representing the URL
     * @throws Exception on failure
     */
    public static File toFile(URL url) throws Exception
    {
        URI uri = url.toURI();
        // Handle UNC paths.
        if (uri.toString().startsWith("file:") && uri.getAuthority() != null && uri.getAuthority().length() > 0)
        {
            uri = new URL("file://" + url.toString().substring("file:".length())).toURI();
        }
        return new File(uri);
    }

    /**
     * Converts a file: URL to a file object, if possible.
     * @param url url to convert
     * @return {@link java.io.File} representing the URL, or null if it can't be converted.
     */
    public static File toFile2(URL url)
    {
        try
        {
            return toFile(url);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param url URL to check
     * @return true if the url is url to a file (even if the file doesn't exist), false otherwise.
     */
    public static boolean isFileURL(URL url)
    {
        try
        {
            toFile(url);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

}

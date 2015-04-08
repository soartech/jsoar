package org.jsoar.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

}

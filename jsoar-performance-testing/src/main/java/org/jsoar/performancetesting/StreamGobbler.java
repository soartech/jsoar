package org.jsoar.performancetesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * This is a class to "gobble" up the output from a stream and then output it to
 * another stream. It is used for process IO instead of redirects for instance.
 * It is necessary because Windows has some issues on CLI with regards to IO.
 * 
 * Adapted from http://www.javaworld.com/jw-12-2000/jw-1229-traps.html?page=4
 * 
 * @author ALT
 */
public class StreamGobbler extends Thread
{
    InputStream is;
    
    PrintWriter output;
    
    StreamGobbler(InputStream is, PrintWriter output)
    {
        this.is = is;
        this.output = output;
    }
    
    /**
     * Run the thread for input but output each character as we get it.
     */
    @Override
    public void run()
    {
        try
        {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            int value;
            while((value = br.read()) != -1)
            {
                char c = (char) value;
                
                output.print(c);
                output.flush();
            }
        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
    }
}

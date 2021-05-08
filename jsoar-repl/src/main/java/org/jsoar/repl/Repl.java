package org.jsoar.repl;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Scanner;

import org.jsoar.kernel.tracing.Printer;
import org.jsoar.runtime.ThreadedAgent;

public class Repl
{

    private ThreadedAgent agent;
    private InputStream in;
    private PrintStream out;
    
    public static void main(String[] args)
    {
        //System.setProperty("jsoar.agent.interpreter", "tcl");
        
        Repl repl = new Repl(System.in, System.out);
        try {
            repl.start();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        
        repl.shutdown();
    }
    
    public Repl(InputStream in, PrintStream out)
    {
        this(ThreadedAgent.create(), in, out);
    }
    
    public Repl(ThreadedAgent agent, InputStream in, PrintStream out)
    {
        this.agent = agent;
        this.in = in;
        this.out = out;
    }
    
    public void start() throws IOException
    {
        initialize();
        
        // read and execute inputs
        try(Scanner scanner = new Scanner(this.in)) {
            
            while(true) {
                String input;
                synchronized(this) {
                    input = scanner.nextLine();
                }
                if(input == null) return; // it seems this can happen if the process is forcibly terminated
                String trimmedInput = input.trim();
                if("quit".equals(trimmedInput)) return;
                
                this.agent.execute(() -> {
                    String result = this.agent.getInterpreter().eval(trimmedInput);
                    Printer printer = this.agent.getPrinter();
                    if(result != null && result.length() != 0)
                    {
                        printer.startNewLine().print(result);
                    }
                    printer.flush();
                    printPrompt();
                    return null;
                }, null);
            }
        }
    }
    

    public void shutdown()
    {
        this.agent.dispose();
    }
    
    private void initialize()
    {
     // ensure we shutdown cleanly
        Runtime.getRuntime().addShutdownHook(new Thread(() -> this.shutdown()));
        
        // echo agent output to the print stream
        this.agent.execute(() -> {
            this.agent.getPrinter().pushWriter(new PrintWriter(this.out));
            printPrompt();
            return null;
        }, null);
    }

    private void printPrompt()
    {
        this.agent.execute(() -> {
            this.agent.getPrinter().startNewLine().print("soar> ").flush();
            return null;
        }, null);
    }

}

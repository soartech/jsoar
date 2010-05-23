package org.jsoar.soar2soar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.jsoar.kernel.SoarException;
import org.jsoar.runtime.ThreadedAgent;

public class Soar2Soar
{

    public Soar2Soar(String[] args) throws SoarException, IOException
    {
        if (args.length < 1)
        {
            usage();
            System.exit(1);
        }

        int numberOfAgents = Integer.parseInt(args[0]);
        EnvironmentAgent ea = new EnvironmentAgent(args[1]);

        for (int i = 0; i < numberOfAgents; ++i)
        {
            ea.createClient(args.length > 3 ? args[2 + i] : args[2]);
        }

        ThreadedAgent currentAgent = null;
        while (true)
        {
            String prompt = currentAgent == null ? "root" : currentAgent
                    .getName();
            System.out.print(prompt + "> ");
            System.out.flush();

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    System.in));

            String line = br.readLine();
            line = line.trim();
            if (line.equals("quit") || line.equals("exit"))
                break;

            // switch agent
            ThreadedAgent temp = ea.getThreadedAgent(line);
            if (temp != null)
            {
                currentAgent = temp;
            }
            else
            {
                if (line.equals("root"))
                    currentAgent = null;

                if (currentAgent != null)
                {
                    currentAgent.getInterpreter().eval(line);
                }
                else
                {
                    // root-level commands
                }
            }

        }

        // cleanup
        System.exit(0);
    }

    private void usage()
    {
        System.out
                .println("usage: "
                        + Soar2Soar.class.toString()
                        + " <# agents> [environment source] [a1 source] [a2 source] ...");
    }

    public static void main(String[] args) throws SoarException, IOException
    {
        new Soar2Soar(args);
    }
}

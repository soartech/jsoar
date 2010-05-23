package org.jsoar.soar2soar;

import java.io.OutputStreamWriter;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.commands.SoarCommands;

public class ClientAgent
{
    private final ThreadedAgent agent;

    private final int id;

    private Identifier envInCommands;

    private Identifier envOutFeedback;

    private Identifier envOutInput;

    public ClientAgent(String name, String source, int id) throws SoarException
    {
        agent = ThreadedAgent.create(name);
        agent
                .getInterpreter()
                .eval(
                        "sp {soar2soar*init (state <s> ^superstate nil) --> (<s> ^soar2soar ready)}");
        agent.getPrinter().addPersistentWriter(
                new OutputStreamWriter(System.out));
        SoarCommands.source(agent.getInterpreter(), source);
        this.id = id;
    }

    public String getName()
    {
        return agent.getName();
    }

    public ThreadedAgent getThreadedAgent()
    {
        return agent;
    }

    public Identifier getEnvInCommands()
    {
        return envInCommands;
    }

    public void setEnvInCommands(Identifier envInCommands)
    {
        this.envInCommands = envInCommands;
    }

    public Identifier getEnvOutFeedback()
    {
        return envOutFeedback;
    }

    public void setEnvOutFeedback(Identifier envOutFeedback)
    {
        this.envOutFeedback = envOutFeedback;
    }

    public Identifier getEnvOutInput()
    {
        return envOutInput;
    }

    public void setEnvOutInput(Identifier envOutInput)
    {
        this.envOutInput = envOutInput;
    }

    public int getId()
    {
        return id;
    }

}

package org.jsoar.soar2soar;

import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.events.InputEvent;
import org.jsoar.kernel.io.InputBuilder;
import org.jsoar.kernel.io.InputWme;
import org.jsoar.kernel.io.InputWmes;
import org.jsoar.kernel.io.TimeInput;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.commands.SoarCommands;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.jsoar.util.events.SoarEvents;

public class EnvironmentAgent
{
    private final Map<String, ClientAgent> agentMap = new HashMap<String, ClientAgent>();

    private final ThreadedAgent env;

    private Identifier agentsId;

    private InputWme timeWme;

    private Identifier agentsOutId;

    public EnvironmentAgent(String source) throws SoarException
    {
        env = ThreadedAgent.create("env");
        env.getInterpreter().eval("sp {soar2soar*init (state <s> ^superstate nil) --> (<s> ^soar2soar ready)}");
        env.getPrinter().addPersistentWriter(new OutputStreamWriter(System.out));
        SoarCommands.source(env.getInterpreter(), source);

        new TimeInput(env.getInputOutput());

        SoarEvents.listenForSingleEvent(env.getEvents(), InputEvent.class,
                new SoarEventListener()
                {
                    @Override
                    public void onEvent(SoarEvent event)
                    {
                        InputBuilder ilBuilder = InputBuilder.create(env.getInputOutput());
                        InputBuilder olBuilder = InputBuilder.create(env.getInputOutput(), env.getInputOutput().getOutputLink());

                        ilBuilder.push("console")
                                       .add("time", 0).markWme("time").pop()
                                 .push("agents").markId("agents");

                        olBuilder.push("agents").markId("agents");

                        agentsId = ilBuilder.getId("agents");
                        timeWme = ilBuilder.getWme("time");
                        agentsOutId = olBuilder.getId("agents");

                        // create structures for each agent
                        for (ClientAgent ca : agentMap.values())
                        {
                            ilBuilder.jump("agents").push("agent")
                                           .add("id",ca.getId()).add("name", ca.getName())
                                           .push("commands").markId("commands");

                            olBuilder.jump("agents")
                                           .push("agent")
                                                 .add("id", ca.getId())
                                                 .add("name", ca.getName())
                                                 .push("feedback").markId("feedback").pop()
                                                 .push("input").markId("input");

                            ca.setEnvInCommands(ilBuilder.getId("commands"));
                            ca.setEnvOutFeedback(olBuilder.getId("feedback"));
                            ca.setEnvOutInput(olBuilder.getId("input"));
                        }
        
			}
		});

		env.openDebugger();
	}

    public void createClient(String source) throws SoarException
    {
        int agentId = agentMap.size() + 1;
        ClientAgent agent = new ClientAgent("a" + agentId, source, agentId);
        agentMap.put(agent.getName(), agent);
    }

    // TODO: create common interface for ClientAgent and EnvironmentAgent
    // so can have common eval method cli can use
    public ThreadedAgent getThreadedAgent(String name)
    {
        if (name.equalsIgnoreCase("env"))
            return env;

        ClientAgent clientAgent = agentMap.get(name);
        return clientAgent != null ? clientAgent.getThreadedAgent() : null;
    }
}

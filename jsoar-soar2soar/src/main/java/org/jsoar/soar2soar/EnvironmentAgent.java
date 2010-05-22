package org.jsoar.soar2soar;

import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.events.InputEvent;
import org.jsoar.kernel.io.InputWme;
import org.jsoar.kernel.io.InputWmes;
import org.jsoar.kernel.io.TimeInput;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.commands.SoarCommands;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.jsoar.util.events.SoarEvents;

public class EnvironmentAgent {
	private final Map<String, ClientAgent> agentMap = new HashMap<String, ClientAgent>();
	private final ThreadedAgent env;
	private InputWme agentsWme;
	private InputWme timeWme;

	public EnvironmentAgent(String source) throws SoarException {
		env = ThreadedAgent.create();
		env.setName("env");
		env.getInterpreter().eval("sp {soar2soar*init (state <s> ^superstate nil) --> (<s> ^soar2soar ready)}");
		env.getPrinter().addPersistentWriter(new OutputStreamWriter(System.out));
		SoarCommands.source(env.getInterpreter(), source);

		new TimeInput(env.getInputOutput());
		
		SoarEvents.listenForSingleEvent(env.getEvents(), InputEvent.class, new SoarEventListener() {
			@Override
			public void onEvent(SoarEvent event) {
				agentsWme = InputWmes.add(env.getInputOutput(), "agents", Symbols.NEW_ID);
				InputWme consoleWme = InputWmes.add(env.getInputOutput(), "console", Symbols.NEW_ID);
				timeWme = InputWmes.add(env.getInputOutput(), consoleWme.getIdentifier(), "time", 0);
			}
		});
		
		env.openDebugger();
	}

	public void createClient(String source) throws SoarException {
		ClientAgent agent = new ClientAgent("a" + (agentMap.size() + 1), source);
		agentMap.put(agent.getName(), agent);
	}
	
	public ThreadedAgent getThreadedAgent(String name) {
		if (name.equalsIgnoreCase("env"))
			return env;
		
		return agentMap.get(name).getThreadedAgent();
	}
}
package org.jsoar.soar2soar;

import java.io.OutputStreamWriter;

import org.jsoar.kernel.SoarException;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.commands.SoarCommands;

public class ClientAgent {
	private final ThreadedAgent agent;

	public ClientAgent(String name, String source) throws SoarException {
		agent = ThreadedAgent.create();
		agent.setName(name); // starts at 1
		agent.getInterpreter().eval("sp {soar2soar*init (state <s> ^superstate nil) --> (<s> ^soar2soar ready)}");
		agent.getPrinter().addPersistentWriter(new OutputStreamWriter(System.out));
		SoarCommands.source(agent.getInterpreter(), source);
	}

	public String getName() {
		return agent.getName();
	}

	public ThreadedAgent getThreadedAgent() {
		return agent;
	}
}
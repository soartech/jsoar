package org.jsoar.soar2soar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.io.TimeInput;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.commands.SoarCommands;

public class Soar2Soar {
	Map<String, ThreadedAgent> agentMap = new HashMap<String, ThreadedAgent>();

	public Soar2Soar(String[] args) throws SoarException, IOException {
		if (args.length < 1) {
			usage();
			System.exit(1);
		}

		int numberOfAgents = Integer.parseInt(args[0]);

		ThreadedAgent env = ThreadedAgent.create();
		env.setName("env");
		env.getPrinter().addPersistentWriter(new OutputStreamWriter(System.out));
		SoarCommands.source(env.getInterpreter(), args[1]);

		new TimeInput(env.getInputOutput());
		env.openDebugger();

		for (int i = 0; i < numberOfAgents; ++i) {
			ThreadedAgent agent = ThreadedAgent.create();
			agent.setName("a" + (i + 1)); // a 1
			agent.getPrinter().addPersistentWriter(new OutputStreamWriter(System.out));
			SoarCommands.source(env.getInterpreter(),
					args.length > 3 ? args[2 + i] : args[2]);
			agentMap.put(agent.getName(), agent);
		}

		ThreadedAgent currentAgent = null;
		while (true) {
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

			if (line.equals("env"))
				currentAgent = env;
			else if (agentMap.containsKey(line))
				currentAgent = agentMap.get(line);
			else if (line.equals("root"))
				currentAgent = null;
			else {
				if (currentAgent != null) {
					currentAgent.getInterpreter().eval(line);
				}
			}
		}

		// cleanup
		System.exit(0);
	}

	private void usage() {
		System.out
				.println("usage: "
						+ Soar2Soar.class.toString()
						+ " <# agents> [environment source] [a1 source] [a2 source] ...");
	}

	public static void main(String[] args) throws SoarException, IOException {
		new Soar2Soar(args);
	}
}
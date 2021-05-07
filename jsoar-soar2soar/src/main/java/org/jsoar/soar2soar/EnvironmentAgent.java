package org.jsoar.soar2soar;

import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.events.InputEvent;
import org.jsoar.kernel.events.OutputEvent;
import org.jsoar.kernel.io.InputBuilder;
import org.jsoar.kernel.io.InputWme;
import org.jsoar.kernel.io.InputWmes;
import org.jsoar.kernel.io.OutputChange;
import org.jsoar.kernel.memory.DummyWme;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.memory.Wmes.MatcherBuilder;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.commands.SoarCommands;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.jsoar.util.events.SoarEvents;

public class EnvironmentAgent {
  private final Map<String, ClientAgent> agentMap = new HashMap<String, ClientAgent>();
  private final ThreadedAgent env;
  private InputWme timeWme;
  private long timeAtStart;

  public EnvironmentAgent(final String source)
      throws SoarException, InterruptedException, ExecutionException, TimeoutException {
    env = ThreadedAgent.create("env");
    env.getPrinter().addPersistentWriter(new OutputStreamWriter(System.out));

    // It's best to source files and initialize the agent in the agent thread!
    env.executeAndWait(
        new Callable<Void>() {

          @Override
          public Void call() throws Exception {
            SoarCommands.source(env.getInterpreter(), getClass().getResource("env.defaults.soar"));
            SoarCommands.source(env.getInterpreter(), source);
            return null;
          }
        },
        20,
        TimeUnit.SECONDS);

    SoarEvents.listenForSingleEvent(
        env.getEvents(),
        InputEvent.class,
        new SoarEventListener() {
          @Override
          public void onEvent(SoarEvent event) {
            doFirstInput();
          }
        });

    env.getEvents()
        .addListener(
            InputEvent.class,
            new SoarEventListener() {
              @Override
              public void onEvent(SoarEvent event) {
                updateInput();
              }
            });

    env.getEvents()
        .addListener(
            OutputEvent.class,
            new SoarEventListener() {
              @Override
              public void onEvent(SoarEvent event) {
                doOutput((OutputEvent) event);
              }
            });
  }

  private void doFirstInput() {
    final InputBuilder ilBuilder = InputBuilder.create(env.getInputOutput());
    final InputBuilder olBuilder =
        InputBuilder.create(env.getInputOutput(), env.getInputOutput().getOutputLink());

    ilBuilder.push("console").add("time", 0).markWme("time").pop().push("agents").markId("agents");

    olBuilder.push("agents").markId("agents");

    timeWme = ilBuilder.getWme("time");
    timeAtStart = System.currentTimeMillis();

    // create structures for each agent
    for (ClientAgent ca : agentMap.values()) {
      ilBuilder
          .jump("agents")
          .push("agent")
          .add("id", ca.getId())
          .add("name", ca.getName())
          .push("commands")
          .markId("commands");

      olBuilder
          .jump("agents")
          .push("agent")
          .add("id", ca.getId())
          .add("name", ca.getName())
          .push("feedback")
          .markId("feedback")
          .pop()
          .push("input")
          .markId("input");

      ca.setEnvInCommands(ilBuilder.getId("commands"));
      ca.setEnvOutFeedback(olBuilder.getId("feedback"));
      ca.setEnvOutInput(olBuilder.getId("input"));
    }
  }

  private void updateInput() {
    InputWmes.update(timeWme, (System.currentTimeMillis() - timeAtStart) / 1000);

    for (ClientAgent agent : agentMap.values()) {
      synchronized (agent) {
        processPendingClientOutput(agent);
      }
    }
  }

  private void doOutput(OutputEvent event) {
    final List<OutputChange> changes = OutputChange.sortByTimeTag(event.getChanges());

    for (ClientAgent agent : agentMap.values()) {
      synchronized (agent) {
        for (OutputChange delta : changes) {
          final Wme wme = delta.getWme();
          if (delta.isAdded()
              && wme.getIdentifier() == agent.getEnvOutFeedback()
              && "add-wme".equals(wme.getAttribute().toString())) {
            agent.pushFeedback(createClientFeedbackWme(agent, wme));
          } else {
            agent.tryToPushInput(delta);
          }
        }
      }
    }
  }

  private Wme createClientFeedbackWme(ClientAgent agent, Wme wme) {
    // ^add-wme
    //   ^id    <id>
    //   ^attr  <attr>
    //   ^value <value>
    final Identifier addWme = wme.getValue().asIdentifier();

    final MatcherBuilder matcher = Wmes.matcher(env.getAgent());
    final Wme idWme = matcher.attr("id").find(addWme);
    final Wme attrWme = matcher.attr("attr").find(addWme);
    final Wme valueWme = matcher.attr("value").find(addWme);

    final Identifier convertedId =
        agent.getClientToEnv().inverse().get(idWme.getValue().asIdentifier());

    return new DummyWme(convertedId, attrWme.getValue(), valueWme.getValue());
  }

  private void processPendingClientOutput(ClientAgent agentContainer) {
    final Map<Identifier, Identifier> clientToEnv = agentContainer.getClientToEnv();
    final Queue<OutputChange> pendingOutputs = agentContainer.getPendingOutputs();
    final Map<Wme, InputWme> clientToEnvWmes = agentContainer.getClientToEnvWmes();

    final SymbolFactory syms = env.getSymbols();
    while (!pendingOutputs.isEmpty()) {
      final OutputChange delta = pendingOutputs.poll();
      if (delta != null) {
        final Wme deltaWme = delta.getWme();
        if (delta.isAdded()) {
          final Symbol deltaWmeValue = deltaWme.getValue();
          final Symbol myAttr = syms.importSymbol(deltaWme.getAttribute());
          Symbol myValue = null;
          if (deltaWmeValue.asIdentifier() == null) {
            myValue = syms.importSymbol(deltaWmeValue);
          } else {
            Identifier convertedId = clientToEnv.get(deltaWmeValue.asIdentifier());

            if (convertedId == null) {
              convertedId = syms.createIdentifier(deltaWmeValue.asIdentifier().getNameLetter());
              clientToEnv.put(deltaWmeValue.asIdentifier(), convertedId);
            }

            myValue = convertedId;
          }

          clientToEnvWmes.put(
              deltaWme,
              env.getInputOutput()
                  .addInputWme(clientToEnv.get(deltaWme.getIdentifier()), myAttr, myValue));
        } else {
          clientToEnvWmes.remove(deltaWme).remove();
        }
      }
    }
  }

  public ClientAgent createClient(String source)
      throws SoarException, InterruptedException, ExecutionException, TimeoutException {
    final int agentId = agentMap.size() + 1;
    final ClientAgent agent = new ClientAgent("a" + agentId, source, agentId);
    agentMap.put(agent.getName(), agent);
    return agent;
  }

  public ThreadedAgent getAgent() {
    return env;
  }

  public Collection<ClientAgent> getClients() {
    return agentMap.values();
  }

  // TODO: create common interface for ClientAgent and EnvironmentAgent
  // so can have common eval method cli can use
  public ThreadedAgent getThreadedAgent(String name) {
    if (name.equalsIgnoreCase("env")) return env;

    ClientAgent clientAgent = agentMap.get(name);
    return clientAgent != null ? clientAgent.getAgent() : null;
  }
}

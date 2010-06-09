package org.jsoar.soar2soar;

import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.events.InputEvent;
import org.jsoar.kernel.events.OutputEvent;
import org.jsoar.kernel.io.InputBuilder;
import org.jsoar.kernel.io.InputWme;
import org.jsoar.kernel.io.InputWmes;
import org.jsoar.kernel.io.OutputChange;
import org.jsoar.kernel.io.TimeInput;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.memory.DummyWme;
import org.jsoar.kernel.memory.Wmes;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.commands.SoarCommands;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.jsoar.util.events.SoarEvents;

import com.google.common.collect.Lists;

public class EnvironmentAgent
{
    private final Map<String, ClientAgent> agentMap = new HashMap<String, ClientAgent>();

    private final ThreadedAgent env;

    private Identifier agentsId;

    private InputWme timeWme;

    private Identifier agentsOutId;
    
    private long timeAtStart;

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
                        
                        timeAtStart = System.currentTimeMillis();

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
        
        env.getEvents().addListener(InputEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                updateInput();
                
            }
            
        });
        
        env.getEvents().addListener(OutputEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                doOutput((OutputEvent) event);
                
            }
            
        });
	}

    protected void doOutput(OutputEvent event)
    {
        ArrayList<OutputChange> changes = Lists.newArrayList(event.getChanges());
        Collections.sort(changes, new Comparator<OutputChange>() {

            @Override
            public int compare(OutputChange o1, OutputChange o2)
            {
                return o1.getWme().getTimetag() - o2.getWme().getTimetag();
            }
            
        });
        
        for ( OutputChange delta : changes) {
            
            for ( ClientAgent agent : agentMap.values()) {
                if ( agent.isMyEnvIdentifier(delta.getWme().getIdentifier())) {
                    agent.pushInput(delta);
                } else {
                	if (delta.getWme().getIdentifier()==agent.getEnvOutFeedback()) {
                		Identifier addWme = delta.getWme().getValue().asIdentifier();
                		
                		Wme idWme = Wmes.matcher(env.getAgent()).attr("id").find(addWme);
                		Wme attrWme = Wmes.matcher(env.getAgent()).attr("attr").find(addWme);
                		Wme valueWme = Wmes.matcher(env.getAgent()).attr("value").find(addWme);
                		
                		Identifier convertedId = agent.getClientToEnv().inverse().get(idWme.getValue().asIdentifier());
                		
                		agent.pushFeedback(new DummyWme(convertedId, attrWme.getValue(), valueWme.getValue()));
                	}
                }
            }
        }
    }

    protected void updateInput()
    {
        InputWmes.update(timeWme, (System.currentTimeMillis()-timeAtStart) / 1000);
        
    	for (ClientAgent agent : agentMap.values()) {
    		processAgentCommands(agent);
    	}
    }

    private void processAgentCommands(ClientAgent agentContainer) {
		    	
    	Map<Identifier, Identifier> clientToEnv = agentContainer.getClientToEnv();
    	Queue<OutputChange> pendingOutputs = agentContainer.getPendingOutputs();
    	Map<Wme, InputWme> clientToEnvWmes = agentContainer.getClientToEnvWmes();
    	
    	while (!pendingOutputs.isEmpty()) {
            OutputChange delta = pendingOutputs.poll();
            if (delta != null) {
                if (delta.isAdded()) {
                    
                    SymbolFactory syms = env.getSymbols();
                    Symbol myAttr = syms.importSymbol(delta.getWme().getAttribute());
                    Symbol myValue = null;
                    if (delta.getWme().getValue().asIdentifier()==null) {
                        myValue = syms.importSymbol(delta.getWme().getValue());
                    } else {
                        Identifier convertedId = clientToEnv.get(delta.getWme().getValue().asIdentifier());
                        
                        if (convertedId==null) {
                            convertedId = syms.createIdentifier(delta.getWme().getValue().asIdentifier().getNameLetter());
                            clientToEnv.put(delta.getWme().getValue().asIdentifier(), convertedId);
                            
                        }
                        
                        myValue = convertedId;
                    }
                    
                    clientToEnvWmes.put(delta.getWme(), env.getInputOutput().addInputWme(clientToEnv.get(delta.getWme().getIdentifier()), myAttr, myValue));
                    
                    
                } else {
                    
                	clientToEnvWmes.remove(delta.getWme()).remove();
                    
                }
            }
        }
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

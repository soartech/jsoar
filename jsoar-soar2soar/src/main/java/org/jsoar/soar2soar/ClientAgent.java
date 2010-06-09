package org.jsoar.soar2soar;

import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.jsoar.kernel.SoarException;
import org.jsoar.kernel.events.InputEvent;
import org.jsoar.kernel.events.OutputEvent;
import org.jsoar.kernel.io.InputWme;
import org.jsoar.kernel.io.OutputChange;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.SymbolFactory;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.commands.SoarCommands;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;

public class ClientAgent
{
    private final ThreadedAgent agent;

    private final int id;

    private Identifier envInCommands;

    private Identifier envOutFeedback;

    private Identifier envOutInput;
    
    private Map<Identifier, Identifier> envToClient = new HashMap<Identifier, Identifier>();
    public BiMap<Identifier, Identifier> getClientToEnv() {
		return clientToEnv;
	}

	public Queue<OutputChange> getPendingOutputs() {
		return pendingOutputs;
	}

	private BiMap<Identifier, Identifier> clientToEnv = HashBiMap.create();
    private Map<Wme, InputWme> envToClientWmes = new HashMap<Wme, InputWme>();
    private Map<Wme, InputWme> clientToEnvWmes = new HashMap<Wme, InputWme>();
    private Queue<OutputChange> pendingInputs = new LinkedBlockingQueue<OutputChange>();
    private Queue<OutputChange> pendingOutputs = new LinkedBlockingQueue<OutputChange>();
    private Queue<Wme> pendingFeedback = new LinkedBlockingQueue<Wme>();
    
    public boolean isMyEnvIdentifier(Identifier candidate) {
        return envToClient.containsKey(candidate);
    }
    
    public boolean isEnvIdentifier(Identifier candidate) {
    	return clientToEnv.containsKey(candidate);
    }
    
    public void pushInput(OutputChange delta) {
        pendingInputs.add(delta);
        
        final Identifier deltaValueAsId = delta.getWme().getValue().asIdentifier();
        if (delta.isAdded() && deltaValueAsId!=null) {
            if (!envToClient.containsKey(deltaValueAsId)) {
                envToClient.put(deltaValueAsId, null);
            }
        }
    }
    
    public void pushFeedback(Wme delta) {
    	pendingFeedback.add(delta);
    }
    
    public void pushOutput(OutputChange delta) {
        pendingOutputs.add(delta);
        
        final Identifier deltaValueAsId = delta.getWme().getValue().asIdentifier();
        if (delta.isAdded() && deltaValueAsId!=null) {
            if (!clientToEnv.containsKey(deltaValueAsId)) {
                clientToEnv.put(deltaValueAsId, null);
            }
        }
    }

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
        
        agent.getEvents().addListener(InputEvent.class, new SoarEventListener()
        {
            
            @Override
            public void onEvent(SoarEvent event)
            {
                doInput();
                
            }
        });
        
        agent.getEvents().addListener(OutputEvent.class, new SoarEventListener()
        {

			@Override
			public void onEvent(SoarEvent event) {
				// TODO Auto-generated method stub
				doOutput((OutputEvent) event);
			}
        	
        });
    }

    

	protected void doOutput(OutputEvent event) {
		// TODO Auto-generated method stub
		
		ArrayList<OutputChange> changes = Lists.newArrayList(event.getChanges());
        Collections.sort(changes, new Comparator<OutputChange>() {

            @Override
            public int compare(OutputChange o1, OutputChange o2)
            {
                return o1.getWme().getTimetag() - o2.getWme().getTimetag();
            }
            
        });
        
        for (OutputChange delta : changes) {
            
        	if (isEnvIdentifier(delta.getWme().getIdentifier())) {
        		pushOutput(delta);
        	}
        	
        }
	}

	protected void doInput()
    {
        while (!pendingInputs.isEmpty()) {
            OutputChange delta = pendingInputs.poll();
            if (delta != null) {
                if (delta.isAdded()) {
                    
                    SymbolFactory syms = agent.getSymbols();
                    Symbol myAttr = syms.importSymbol(delta.getWme().getAttribute());
                    Symbol myValue = null;
                    if (delta.getWme().getValue().asIdentifier()==null) {
                        myValue = syms.importSymbol(delta.getWme().getValue());
                    } else {
                        Identifier convertedId = envToClient.get(delta.getWme().getValue().asIdentifier());
                        
                        if (convertedId==null) {
                            convertedId = syms.createIdentifier(delta.getWme().getValue().asIdentifier().getNameLetter());
                            envToClient.put(delta.getWme().getValue().asIdentifier(), convertedId);
                            
                        }
                        
                        myValue = convertedId;
                    }
                    
                    envToClientWmes.put(delta.getWme(), agent.getInputOutput().addInputWme(envToClient.get(delta.getWme().getIdentifier()), myAttr, myValue));
                    
                    
                } else {
                    
                    envToClientWmes.remove(delta.getWme()).remove();
                    
                }
            }
        }
        
        while (!pendingFeedback.isEmpty()) {
        	Wme delta = pendingFeedback.poll();
        	if (delta!=null) {
        		SymbolFactory syms = agent.getSymbols();
                Symbol myAttr = syms.importSymbol(delta.getAttribute());
                Symbol myValue = syms.importSymbol(delta.getValue());
                
                agent.getInputOutput().addInputWme(delta.getIdentifier(), myAttr, myValue);
        	}
        }
        
    }

    public String getName()
    {
        return agent.getName();
    }

    public Map<Wme, InputWme> getClientToEnvWmes() {
		return clientToEnvWmes;
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
        
        clientToEnv.put(agent.getInputOutput().getOutputLink(), envInCommands);
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
        
        envToClient.put(envOutInput, agent.getInputOutput().getInputLink());
    }

    public int getId()
    {
        return id;
    }

}

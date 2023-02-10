package org.jsoar.soar2soar;

import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class ClientAgent
{
    private final ThreadedAgent agent;
    private final int id;
    private Identifier envOutFeedback;
    private final Map<Identifier, Identifier> envToClient = new HashMap<Identifier, Identifier>();
    private final BiMap<Identifier, Identifier> clientToEnv = HashBiMap.create();
    private final Map<Wme, InputWme> envToClientWmes = new HashMap<Wme, InputWme>();
    private final Map<Wme, InputWme> clientToEnvWmes = new HashMap<Wme, InputWme>();
    private final Queue<OutputChange> pendingInputs = new LinkedBlockingQueue<OutputChange>();
    private final Queue<OutputChange> pendingOutputs = new LinkedBlockingQueue<OutputChange>();
    private final Queue<Wme> pendingFeedback = new LinkedBlockingQueue<Wme>();
    
    public ClientAgent(String name, final String source, int id) throws SoarException, InterruptedException, ExecutionException, TimeoutException
    {
        this.id = id;
        agent = ThreadedAgent.create(name);
        agent.getPrinter().addPersistentWriter(new OutputStreamWriter(System.out));
        
        // It's best to source files and initialize the agent in the agent thread!
        agent.executeAndWait(() ->
        {
            SoarCommands.source(agent.getInterpreter(), getClass().getResource("env.defaults.soar"));
            SoarCommands.source(agent.getInterpreter(), source);
            return null;
        }, 20, TimeUnit.SECONDS);
        
        agent.getEvents().addListener(InputEvent.class, event -> doInput());
        agent.getEvents().addListener(OutputEvent.class, event -> doOutput((OutputEvent) event));
    }
    
    public BiMap<Identifier, Identifier> getClientToEnv()
    {
        return clientToEnv;
    }
    
    public Queue<OutputChange> getPendingOutputs()
    {
        return pendingOutputs;
    }
    
    private boolean isMyEnvIdentifier(Identifier candidate)
    {
        return envToClient.containsKey(candidate);
    }
    
    private boolean isEnvIdentifier(Identifier candidate)
    {
        return clientToEnv.containsKey(candidate);
    }
    
    public void tryToPushInput(OutputChange delta)
    {
        if(!isMyEnvIdentifier(delta.getWme().getIdentifier()))
        {
            return;
        }
        
        pendingInputs.add(delta);
        
        final Identifier deltaValueAsId = delta.getWme().getValue().asIdentifier();
        if(delta.isAdded() && deltaValueAsId != null)
        {
            if(!envToClient.containsKey(deltaValueAsId))
            {
                envToClient.put(deltaValueAsId, null);
            }
        }
    }
    
    public void pushFeedback(Wme delta)
    {
        pendingFeedback.add(delta);
    }
    
    private void tryToPushOutputToEnvironment(OutputChange delta)
    {
        if(!isEnvIdentifier(delta.getWme().getIdentifier()))
        {
            return;
        }
        
        pendingOutputs.add(delta);
        
        final Identifier deltaValueAsId = delta.getWme().getValue().asIdentifier();
        if(delta.isAdded() && deltaValueAsId != null)
        {
            if(!clientToEnv.containsKey(deltaValueAsId))
            {
                clientToEnv.put(deltaValueAsId, null);
            }
        }
    }
    
    private synchronized void doOutput(OutputEvent event)
    {
        for(OutputChange delta : OutputChange.sortByTimeTag(event.getChanges()))
        {
            tryToPushOutputToEnvironment(delta);
        }
    }
    
    private synchronized void doInput()
    {
        final SymbolFactory syms = agent.getSymbols();
        while(!pendingInputs.isEmpty())
        {
            final OutputChange delta = pendingInputs.poll();
            if(delta != null)
            {
                final Wme deltaWme = delta.getWme();
                if(delta.isAdded())
                {
                    final Symbol deltaValue = deltaWme.getValue();
                    final Symbol myAttr = syms.importSymbol(deltaWme.getAttribute());
                    Symbol myValue = null;
                    if(deltaValue.asIdentifier() == null)
                    {
                        myValue = syms.importSymbol(deltaWme.getValue());
                    }
                    else
                    {
                        Identifier convertedId = envToClient.get(deltaValue.asIdentifier());
                        
                        if(convertedId == null)
                        {
                            convertedId = syms.createIdentifier(deltaValue.asIdentifier().getNameLetter());
                            envToClient.put(deltaValue.asIdentifier(), convertedId);
                        }
                        
                        myValue = convertedId;
                    }
                    
                    envToClientWmes.put(deltaWme, agent.getInputOutput().addInputWme(envToClient.get(deltaWme.getIdentifier()), myAttr, myValue));
                    
                }
                else
                {
                    envToClientWmes.remove(deltaWme).remove();
                }
            }
        }
        
        while(!pendingFeedback.isEmpty())
        {
            final Wme delta = pendingFeedback.poll();
            if(delta != null)
            {
                final Symbol myAttr = syms.importSymbol(delta.getAttribute());
                final Symbol myValue = syms.importSymbol(delta.getValue());
                
                agent.getInputOutput().addInputWme(delta.getIdentifier(), myAttr, myValue);
            }
        }
        
    }
    
    public String getName()
    {
        return agent.getName();
    }
    
    public Map<Wme, InputWme> getClientToEnvWmes()
    {
        return clientToEnvWmes;
    }
    
    public ThreadedAgent getAgent()
    {
        return agent;
    }
    
    public void setEnvInCommands(Identifier envInCommands)
    {
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
    
    public void setEnvOutInput(Identifier envOutInput)
    {
        envToClient.put(envOutInput, agent.getInputOutput().getInputLink());
    }
    
    public int getId()
    {
        return id;
    }
    
}

/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on Jun 29, 2009
 */
package sml;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jsoar.kernel.events.InputEvent;
import org.jsoar.kernel.events.OutputEvent;
import org.jsoar.util.events.SoarEvent;
import org.jsoar.util.events.SoarEventListener;
import org.junit.Test;

import sml.Kernel.RhsFunctionInterface;


/**
 * @author ray
 */
public class AgentTest
{

    @Test public void testExecuteCommandLine()
    {
        final Kernel kernel = Kernel.CreateKernelInCurrentThread();
        final Agent agent = kernel.CreateAgent("testExecuteCommandLine");
        agent.ExecuteCommandLine("sp {test (state <s> ^superstate nil) --> (write |hi|)}");
        assertTrue("Expected command to load production", agent.IsProductionLoaded("test"));
    }
    
    @Test public void testConstructInputWithSML() throws Exception
    {
        final Kernel kernel = Kernel.CreateKernelInCurrentThread();
        final Agent agent = kernel.CreateAgent("testConstructInputWithSML");
        
        final AtomicBoolean matched = new AtomicBoolean(false);
        final RhsFunctionInterface match = new RhsFunctionInterface() {

            @Override
            public String rhsFunctionHandler(int eventID, Object data,
                    String agentName, String functionName, String argument)
            {
                matched.set(true);
                return null;
            }};
        
        kernel.AddRhsFunction("match", match, null);
        
        agent.agent.getEvents().addListener(InputEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                Identifier root = agent.CreateIdWME(agent.GetInputLink(), "root");
                agent.CreateStringWME(root, "name", "felix");
                agent.CreateIntWME(root, "age", 45);
                agent.CreateFloatWME(root, "gpa", 2.6);
            }});
        
        agent.ExecuteCommandLine("sp {test " +
        		"(state <s> ^superstate nil ^io.input-link.root <r>)" +
        		"(<r> ^name felix ^age 45 ^gpa 2.6)" +
        		"-->" +
        		"(<s> ^foo (exec match))}");
        agent.RunSelf(1);
        assertTrue(matched.get());
    }
    
    @Test public void testHandleOutputCommandFromSML()
    {
        final Kernel kernel = Kernel.CreateKernelInCurrentThread();
        final Agent agent = kernel.CreateAgent("testConstructInputWithSML");

        final AtomicReference<String> message = new AtomicReference<String>("");
        final AtomicBoolean success = new AtomicBoolean(false);
        agent.agent.getEvents().addListener(OutputEvent.class, new SoarEventListener() {

            @Override
            public void onEvent(SoarEvent event)
            {
                final Identifier ol = agent.GetOutputLink();
                if(ol == null)
                {
                    message.set("GetOutputLink() returned null");
                    return;
                }
                
                final int count = agent.GetNumberCommands();
                if(count != 1)
                {
                    message.set(String.format("GetNumberCommands(): Expected 1, got %d", count));
                    return;
                }
                
                final Identifier c = agent.GetCommand(0);
                if(c == null)
                {
                    message.set("GetCommand(0) returned null");
                    return;
                }
                if(!"command".equals(c.GetAttribute()))
                {
                    message.set(String.format("c.GetAttribute(): Expected 'command', got '%s'", c.GetAttribute()));
                    return;
                }
                final int numKids = c.GetNumberChildren();
                if(numKids != 4)
                {
                    message.set(String.format("c.GetNumberChildren(): Expected 4, got %d", numKids));
                    return;
                }
                for(int i = 0; i < numKids; ++i)
                {
                    final WMElement kid = c.GetChild(i);
                    if(kid == null)
                    {
                        message.set(String.format("c.GetChild(%d) returned null", i));
                    }
                    if("int".equals(kid.GetAttribute()))
                    {
                        if(23 != kid.ConvertToIntElement().GetValue())
                        {
                            message.set(String.format("Expected ^int 23, got ^int %d", kid.ConvertToIntElement().GetValue()));
                            return;
                        }
                    }
                    else if("float".equals(kid.GetAttribute()))
                    {
                        if(3.14159 != kid.ConvertToFloatElement().GetValue())
                        {
                            message.set(String.format("Expected ^float 3.14159, got ^float %f", kid.ConvertToFloatElement().GetValue()));
                            return;
                        }
                    }
                    else if("string".equals(kid.GetAttribute()))
                    {
                        if(!"a string".equals(kid.ConvertToStringElement().GetValue()))
                        {
                            message.set(String.format("Expected ^string |a string|, got ^string |%s|", kid.ConvertToStringElement().GetValue()));
                            return;
                        }
                    }
                    else if("object".equals(kid.GetAttribute()))
                    {
                        final Identifier v = kid.ConvertToIdentifier();
                        
                        final String link = v.GetParameterValue("link");
                        if(!agent.GetOutputLink().GetValueAsString().equals(link))
                        {
                            message.set(String.format("Expected ^object.link |%s|, got ^object.link |%s|", agent.GetOutputLink().GetValueAsString(), link));
                            return;
                        }
                        final String type = v.GetParameterValue("type");
                        if(!"foo".equals(type))
                        {
                            message.set(String.format("Expected ^object.type |foo|, got ^object.type |%s|", type));
                            return;
                        }
                    }
                    else
                    {
                        message.set(String.format("c.GetChild(%d) has unexpected attribute '%s'", i));
                        return;
                    }
                }
                
                success.set(true);
            }});
        
        agent.ExecuteCommandLine("sp {test " +
        		"(state <s> ^superstate nil ^io.output-link <ol>)" +
        		"-->" +
        		"(<ol> ^command <c>)" +
        		"(<c> ^int 23 ^float 3.14159 ^string |a string| ^object <obj>)" +
        		"(<obj> ^type foo ^link <ol>)}");
        agent.RunSelf(1);
        assertTrue(message.get(), success.get());
    }
}

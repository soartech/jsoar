package org.jsoar.debugger;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoadPluginCommandTest
{
    public static class TestPlugin implements JSoarDebuggerPlugin
    {
        
        @Override
        public void initialize(JSoarDebugger debugger, String[] args)
        {
            if(!(args.length == 2 && args[0].equals("foo") && args[1].equals("1")))
            {
                throw new RuntimeException("Did not get expected args");
            }
            
        }
        
    }
    
    @Mock
    JSoarDebugger debugger;
    
    @Test
    void testLoadPlugin() throws SoarException
    {
        Agent agent = new Agent();
        agent.getInterpreter().addCommand("load-plugin", new LoadPluginCommand(this.debugger));
        agent.getInterpreter().eval("load-plugin org.jsoar.debugger.LoadPluginCommandTest$TestPlugin foo 1");
    }
    
}

package org.jsoar.debugger.stopcommand;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import bibliothek.gui.dock.common.MultipleCDockableLayout;
import bibliothek.util.xml.XElement;

public class StopCommandViewLayout implements MultipleCDockableLayout
{
    /** the command of the stop command view */
    private String command;
    
    /**
     * Sets the command of the stop command view that is shown.
     * 
     * @param command the command of the pvd
     */
    public void setCommand(String command)
    {
        this.command = command;
    }
    
    /**
     * Gets the command of the stop command panel that is shown.
     * 
     * @return the command
     */
    public String getCommand()
    {
        return command;
    }
    
    public void readStream(DataInputStream in) throws IOException
    {
        // do nothing. this method is for binary layout files
        // command = in.readUTF();
    }
    
    public void readXML(XElement element)
    {
        command = element.getString();
    }
    
    public void writeStream(DataOutputStream out) throws IOException
    {
        // do nothing. this method is for binary layout files
        // out.writeUTF( command );
    }
    
    public void writeXML(XElement element)
    {
        element.setString(command);
    }
}

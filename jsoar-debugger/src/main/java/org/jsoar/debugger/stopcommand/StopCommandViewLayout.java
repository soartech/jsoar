package org.jsoar.debugger.stopcommand;

import bibliothek.gui.dock.common.MultipleCDockableLayout;
import bibliothek.gui.dock.themes.basic.BasicDockableDisplayerDecorator;
import bibliothek.util.xml.XElement;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class StopCommandViewLayout implements MultipleCDockableLayout
{
    /** the name of the pvd */
    private String name;

    /**
     * Sets the name of the pvd that is shown.
     * @param name the name of the pvd
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * Gets the name of the pvd that is shown.
     * @return the name
     */
    public String getName() {
        return name;
    }

    public void readStream( DataInputStream in ) throws IOException
    {
        //do nothing. this method is for binary layout files
//            name = in.readUTF();
    }

    public void readXML(XElement element ) {
        name = element.getString();
    }

    public void writeStream( DataOutputStream out ) throws IOException
    {
        //do nothing. this method is for binary layout files
//            out.writeUTF( name );
    }

    public void writeXML( XElement element ) {
        element.setString( name );
    }
}

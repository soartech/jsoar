package org.jsoar.kernel.rete;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jsoar.kernel.Agent;
import org.jsoar.kernel.SoarException;

public class ReteSerializer
{
    /**
     * Write a serialized version of the agent onto the output stream.
     * 
     * @param context the agent to serialize
     * @param os the output stream to write to
     * @throws IOException on general I/O errors (e.g., permission issues)
     */
    public static void saveRete(Agent context, OutputStream os) throws IOException
    {
        new ReteNetWriter(context).write(os);
    }

    /**
     * Construct a new agent based on a serialized agent. The new agent will be pre-loaded with productions
     * from the serialized agent, but will not be initialized.
     * 
     * @param is the InputStream containing the serialized agent.
     * @return an agent whose productions are loaded from the provided input stream.
     * @throws SoarException if the input stream contains an unrecognized rete file.
     * @throws IOException on general I/O errors (e.g., permission issues)
     */
    public static Agent createAgent(InputStream is) throws IOException, SoarException
    {
        Agent agent = new Agent();
        replaceRete(agent, is);
        return agent;
    }

    /**
     * Excise all productions from the target agent and load productions from the serialized agent.
     * 
     * @param context the agent to replace the productions of.
     * @param is InputStream containing the serialized agent.
     * @throws SoarException if the input stream contains an unrecognized rete file.
     * @throws IOException on general I/O errors (e.g., permission issues)
     */
    public static void replaceRete(Agent context, InputStream is) throws IOException, SoarException
    {
        // TODO: Maybe initialize works instead?
        context.reinitialize_soar();
//        Rete rete = Adaptables.adapt(context, Rete.class);
        new ReteNetReader(context).read(is);
    }
}

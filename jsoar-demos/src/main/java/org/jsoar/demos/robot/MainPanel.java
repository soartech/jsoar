/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 10, 2009
 */
package org.jsoar.demos.robot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.jsoar.debugger.util.SwingTools;
import org.jsoar.demos.robot.AsciiWorldLoader.Result;

public class MainPanel extends JPanel
{
    private static final long serialVersionUID = -8854842910096874773L;
    
    private World world;
    private WorldPanel worldPanel;
    private Timer timer;
    
    private Map<String, RobotAgent> agents = new HashMap<String, RobotAgent>();
    
    public MainPanel() throws IOException
    {
        super(new BorderLayout());
        
        this.worldPanel = new WorldPanel();
        loadWorld(MainPanel.class.getResource("/org/jsoar/demos/robot/default.world"));
        
        timer = new Timer(100, e ->
        {
            world.update(0.1);
            for(RobotAgent agent : agents.values())
            {
                agent.update();
            }
            worldPanel.repaint();
        });
        
        final JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        
        add(worldPanel, BorderLayout.CENTER);
        add(bar, BorderLayout.SOUTH);
        
        bar.add(new AbstractAction("Run")
        {
            
            private static final long serialVersionUID = -139687130503326330L;
            
            @Override
            public void actionPerformed(ActionEvent e)
            {
                start();
            }
        });
        
        bar.add(new AbstractAction("Pause")
        {
            
            private static final long serialVersionUID = -1100256461140715756L;
            
            @Override
            public void actionPerformed(ActionEvent e)
            {
                stop();
            }
        });
        bar.add(new AbstractAction("Fit")
        {
            
            private static final long serialVersionUID = 6423267961582949320L;
            
            @Override
            public void actionPerformed(ActionEvent e)
            {
                worldPanel.fit();
            }
        });
        
        final JCheckBox follow = new JCheckBox("Follow");
        follow.addActionListener(e ->
        {
            final Robot robot = !world.getRobots().isEmpty() ? world.getRobots().get(0) : null;
            worldPanel.setFollow(follow.isSelected() ? robot : null);
        });
        bar.add(follow);
        // agents.put(robot, new RobotAgent(robot));
        
        bar.add(new AbstractAction("Paste World")
        {
            
            private static final long serialVersionUID = -2328953530526643612L;
            
            @Override
            public void actionPerformed(ActionEvent e)
            {
                loadWorldFromClipboard();
            }
        });
        
        bar.add(new AbstractAction("Debug")
        {
            
            private static final long serialVersionUID = -8516939880295829579L;
            
            @Override
            public void actionPerformed(ActionEvent e)
            {
                debug();
            }
        });
    }
    
    /**
     * 
     */
    private void debug()
    {
        final Robot selection = worldPanel.getSelection();
        if(selection == null)
        {
            return;
        }
        
        final RobotAgent agent = agents.get(selection.name);
        if(agent == null)
        {
            return;
        }
        
        agent.debug();
    }
    
    /**
     * 
     */
    private void start()
    {
        timer.start();
        for(RobotAgent agent : agents.values())
        {
            agent.start();
        }
        
    }
    
    /**
     * 
     */
    private void stop()
    {
        timer.stop();
        for(RobotAgent agent : agents.values())
        {
            agent.stop();
        }
    }
    
    public void loadWorldFromClipboard()
    {
        try
        {
            final String ascii = Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor).toString();
            loadWorld(ascii);
        }
        catch(HeadlessException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch(UnsupportedFlavorException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch(IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public void loadWorld(URL url) throws IOException
    {
        setWorld(new AsciiWorldLoader().load(url));
    }
    
    public void loadWorld(String ascii) throws IOException
    {
        setWorld(new AsciiWorldLoader().load(new ByteArrayInputStream(ascii.getBytes())));
    }
    
    public void setWorld(Result loadResult)
    {
        this.world = loadResult.world;
        this.worldPanel.setWorld(this.world);
        this.worldPanel.fit();
        updateAgents(loadResult.config);
    }
    
    private void updateAgents(Properties config)
    {
        final Set<RobotAgent> deadAgents = new HashSet<RobotAgent>(agents.values());
        for(Robot robot : world.getRobots())
        {
            final RobotAgent existing = agents.get(robot.name);
            if(existing != null)
            {
                deadAgents.remove(existing);
                existing.setRobot(robot, config);
            }
            else
            {
                final RobotAgent newAgent = new RobotAgent();
                newAgent.setRobot(robot, config);
                agents.put(robot.name, newAgent);
            }
        }
        
        for(RobotAgent agent : deadAgents)
        {
            agents.values().remove(agent);
            agent.dispose();
        }
    }
    
    public class GTextArea extends JTextArea
    {
        private static final long serialVersionUID = 5765203814868709064L;
        
        int y = 0;
        Color transcolor;
        
        public GTextArea()
        {
            super(5, 10);
            this.setBackground(Color.GREEN);
            this.setOpaque(false);
            this.transcolor = new Color(1, 252, 1, 40);
        }
        
        @Override
        public void paintComponent(Graphics g)
        {
            g.setColor(this.transcolor);
            g.fillRect(0, 0, getWidth(), getHeight());
            super.paintComponent(g);
        }
    }
    
    public static void main(String[] args)
    {
        SwingTools.initializeLookAndFeel();
        
        SwingUtilities.invokeLater(() ->
        {
            JFrame f = new JFrame();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            try
            {
                final MainPanel contentPane = new MainPanel();
                f.setContentPane(contentPane);
                f.setSize(640, 640);
                f.setVisible(true);
                contentPane.worldPanel.fit();
            }
            catch(IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
    }
}

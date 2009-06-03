/*
 * Copyright (c) 2009 Dave Ray <daveray@gmail.com>
 *
 * Created on May 10, 2009
 */
package org.jsoar.demos.robot;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;



class MainPanel extends JPanel 
{
    private World world = new World();
    private WorldPanel worldPanel = new WorldPanel(world);
    private Timer timer;
    
    private Map<Robot, RobotAgent> agents = new HashMap<Robot, RobotAgent>();
    
    public MainPanel()
    {
        super(new BorderLayout());
        
        timer = new Timer(100, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                world.update(0.1);
                for(RobotAgent agent : agents.values())
                {
                    agent.update();
                }
                worldPanel.repaint();
            }});
        timer.start();
        
        final JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        
        add(worldPanel, BorderLayout.CENTER);
        add(bar, BorderLayout.SOUTH);
        
        bar.add(new AbstractAction("Run") {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                timer.start();
            }});
        
        bar.add(new AbstractAction("Pause") {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                timer.stop();
            }});
        
        Robot robot = new Robot(world, "a");
        robot.move(3, 2);
        robot.yaw = Math.toRadians(180.0);
        robot.speed = 0.1;
        robot.turnRate = Math.toRadians(12.0);
        
        world.addRobot(robot);

        agents.put(robot, new RobotAgent(robot));
    }
    
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run()
            {
                JFrame f = new JFrame();
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setContentPane(new MainPanel());
                f.setSize(640, 640);
                f.setVisible(true);
            }});
    }
}
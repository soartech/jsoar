/*
 * Copyright (c) 2008  Dave Ray <daveray@gmail.com>
 *
 * Created on Oct 23, 2008
 */
package org.jsoar.debugger;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.debugger.selection.SelectionProvider;
import org.jsoar.kernel.Agent;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;

import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.layout.graph.ForceDirectedLayout;
import prefuse.activity.Activity;
import prefuse.controls.DragControl;
import prefuse.controls.FocusControl;
import prefuse.controls.NeighborHighlightControl;
import prefuse.controls.PanControl;
import prefuse.controls.WheelZoomControl;
import prefuse.controls.ZoomControl;
import prefuse.controls.ZoomToFitControl;
import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Tuple;
import prefuse.data.event.TupleSetListener;
import prefuse.data.tuple.TupleSet;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.render.LabelRenderer;
import prefuse.util.ColorLib;
import prefuse.visual.EdgeItem;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;

/**
 * @author ray
 */
public class WorkingMemoryGraphView extends AbstractAdaptableView
{
    private static final long serialVersionUID = 3769825340223029388L;
    
    private static final String graph = "graph";
    private static final String nodes = "graph.nodes";
    private static final String edges = "graph.edges";

    private final LittleDebugger debugger;
    private Visualization m_vis;
    private Set<Wme> wmes = new HashSet<Wme>();
    private final Map<Identifier, Node> nodeMap = new HashMap<Identifier, Node>();
    private final Map<Wme, Edge> edgeMap = new HashMap<Wme, Edge>();
    private Graph g;

    private Display display;
    
    private boolean enabled = true;

    private FocusSetListener focusSetListener = new FocusSetListener();;

    
    /**
     * @param persistentId
     * @param title
     */
    public WorkingMemoryGraphView(LittleDebugger debugger)
    {
        super("workingMemory.graph", "Working Memory Graph");
        
        this.debugger = debugger;
        
        JPanel p = new JPanel(new BorderLayout());
        
        JToolBar bar = initToolbar();
        p.add(bar, BorderLayout.NORTH);
        
        m_vis = new Visualization();
        
        LabelRenderer lr = new LabelRenderer();
        lr.setRoundedCorner(8, 8);
        lr.setTextField("value");
        
        EdgeRenderer er = new EdgeRenderer() {

            /* (non-Javadoc)
             * @see prefuse.render.EdgeRenderer#render(java.awt.Graphics2D, prefuse.visual.VisualItem)
             */
            @Override
            public void render(Graphics2D g, VisualItem item)
            {
                super.render(g, item);
                
                EdgeItem ei = (EdgeItem) item;
                NodeItem source = ei.getSourceItem();
                NodeItem target = ei.getTargetItem();
                double x = (source.getX() + target.getX()) / 2.0;
                double y = (source.getY() + target.getY()) / 2.0;
                final Wme w = (Wme) ei.get("value");
                final String label = w.getAttribute() + " : " + w.getTimetag();
                x -= g.getFontMetrics().stringWidth(label) / 2.0;
                y += g.getFontMetrics().getAscent() / 2.0;
                g.drawString(label , (int) x, (int) y);
            }
            
        };
        final DefaultRendererFactory rf = new DefaultRendererFactory(lr, er);
        m_vis.setRendererFactory(rf);
        
        g = createEmptyGraph();
        m_vis.addGraph(graph, g);
        
        // fix selected focus nodes
        TupleSet focusGroup = m_vis.getGroup(Visualization.FOCUS_ITEMS); 
        focusGroup.addTupleSetListener(focusSetListener);

        // --------------------------------------------------------------------
        // create actions to process the visual data

//        int hops = 30;
//        final GraphDistanceFilter filter = new GraphDistanceFilter(graph, hops);
//        final JValueSlider slider = new JValueSlider("Distance", 0, hops, hops);
//        slider.addChangeListener(new ChangeListener() {
//            public void stateChanged(ChangeEvent e) {
//                filter.setDistance(slider.getValue().intValue());
//                m_vis.run("draw");
//            }
//        });
//        slider.setBackground(Color.WHITE);
//        slider.setPreferredSize(new Dimension(300,30));
//        slider.setMaximumSize(new Dimension(300,30));
//        p.add(slider, BorderLayout.SOUTH);

        ColorAction fill = new ColorAction(nodes, VisualItem.FILLCOLOR, ColorLib.rgb(200,200,255));
        fill.add(VisualItem.FIXED, ColorLib.rgb(255,100,100));
        fill.add(VisualItem.HIGHLIGHT, ColorLib.rgb(255,200,125));
        
        ActionList fillAndColors = new ActionList();
        fillAndColors.add(fill);
        fillAndColors.add(new ColorAction(nodes, VisualItem.STROKECOLOR, 0));
        fillAndColors.add(new ColorAction(nodes, VisualItem.TEXTCOLOR, ColorLib.rgb(0,0,0)));
        fillAndColors.add(new ColorAction(edges, VisualItem.FILLCOLOR, ColorLib.gray(200)));
        fillAndColors.add(new ColorAction(edges, VisualItem.STROKECOLOR, ColorLib.gray(200)));
        fillAndColors.add(new ColorAction(edges, VisualItem.TEXTCOLOR, ColorLib.rgb(0,0,0)));
        
        ActionList draw = new ActionList();
        //draw.add(filter);
        draw.add(fillAndColors);
        
        ActionList animate = new ActionList(Activity.INFINITY);
        final ForceDirectedLayout forceDirectedLayout = new ForceDirectedLayout(graph){

            /* (non-Javadoc)
             * @see prefuse.action.layout.graph.ForceDirectedLayout#getSpringLength(prefuse.visual.EdgeItem)
             */
            @Override
            protected float getSpringLength(EdgeItem e)
            {
                Symbol sym = (Symbol) e.getTargetNode().get("value");
                return sym.asIdentifier() != null ? 75 * 2.5f : 75;
            }
        };
        
        animate.add(forceDirectedLayout);
        animate.add(fillAndColors);
        animate.add(new RepaintAction());
        
        // finally, we register our ActionList with the Visualization.
        // we can later execute our Actions by invoking a method on our
        // Visualization, using the name we've chosen below.
        m_vis.putAction("draw", draw);
        m_vis.putAction("animate", animate);
        m_vis.runAfter("draw", "animate");
        
        // --------------------------------------------------------------------
        // set up a display to show the visualization
        
        display = new Display(m_vis);
        display.setSize(700,700);
        display.pan(200, 200);
        display.setForeground(Color.GRAY);
        display.setBackground(Color.WHITE);
        display.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        
        // main display controls
        display.addControlListener(new FocusControl(1));
        display.addControlListener(new DragControl());
        display.addControlListener(new PanControl());
        display.addControlListener(new ZoomControl());
        display.addControlListener(new WheelZoomControl());
        display.addControlListener(new ZoomToFitControl());
        display.addControlListener(new NeighborHighlightControl());

        // overview display
//        Display overview = new Display(vis);
//        overview.setSize(290,290);
//        overview.addItemBoundsListener(new FitOverviewListener());
        
        display.setForeground(Color.GRAY);
        display.setBackground(Color.WHITE);
        
        // --------------------------------------------------------------------        
        // launch the visualization
        
        m_vis.run("draw");
        
        p.add(display, BorderLayout.CENTER);
        //JForcePanel fpanel = new JForcePanel(forceDirectedLayout.getForceSimulator());
        //p.add(fpanel, BorderLayout.EAST);
        
        setContentPane(p);
    }
    
    /**
     * @return
     */
    private JToolBar initToolbar()
    {
        JToolBar bar = new JToolBar();
        bar.setFloatable(false);
        
        bar.add(new AbstractAction("Refresh", Images.REFRESH) {

            private static final long serialVersionUID = 4740692425788121219L;

            public void actionPerformed(ActionEvent e)
            {
                refresh(false);
            }});
        
        final JButton enabledButton = new JButton(enabled ? Images.PAUSE : Images.START);
        enabledButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e)
            {
                enabled = !enabled;
                if(enabled)
                {
                    enabledButton.setIcon(Images.PAUSE);
                    refresh(false);
                    m_vis.run("animate");
                }
                else
                {
                    enabledButton.setIcon(Images.START);
                    m_vis.cancel("animate");
                }
            }});
        
        bar.add(enabledButton);
        
        bar.add(new JLabel("        Pan: left-drag, Zoom: scroll, Center: right-click"));
        return bar;
    }

    public void refresh(boolean afterInitSoar)
    {
        synchronized(m_vis)
        {
            refreshInternal(afterInitSoar);
        }
    }
    
    private void refreshInternal(boolean afterInitSoar)
    {
        if(afterInitSoar)
        {
            m_vis.removeGroup(graph);
            m_vis.add(graph, g = createEmptyGraph());
            wmes.clear();
        }
        
        if(!enabled)
        {
            return;
        }
        
        final Agent agent = debugger.getAgentProxy().getAgent();
        Set<Wme> newWmes = debugger.getAgentProxy().execute(new Callable<Set<Wme>>() {
            public Set<Wme> call() throws Exception
            {
                return new HashSet<Wme>(agent.rete.getAllWmes());
            }});
        
        Set<Wme> added = new HashSet<Wme>();
        Set<Wme> removed = new HashSet<Wme>();
        
        Set<Identifier> possibleDeadNodes = new HashSet<Identifier>();
        for(Wme w : wmes)
        {
            Identifier valueAsId = w.getValue().asIdentifier();
            if(valueAsId != null)
            {
                possibleDeadNodes.add(valueAsId);
            }
            if(!newWmes.contains(w))
            {
                removed.add(w);
            }
        }
        for(Wme w : newWmes)
        {
            possibleDeadNodes.remove(w.getIdentifier());
            possibleDeadNodes.remove(w.getValue());
            if(!wmes.contains(w))
            {
                added.add(w);
            }
        }

        this.wmes = newWmes;
        
        for(Wme w : removed)
        {
            Edge e = edgeMap.remove(w);
            assert e != null && e.isValid();
            
            Node target = e.getTargetNode();
            g.removeEdge(e);
            if(w.getValue().asIdentifier() == null)
            {
                g.removeNode(target);
            }
        }
        
        for(Wme w : added)
        {
            final Identifier sourceSym = w.getIdentifier();
            
            Node source = nodeMap.get(sourceSym);
            final boolean sourceIsNew = source == null;
            if(source == null)
            {
                source = g.addNode();
                source.set("value", sourceSym);
                nodeMap.put(sourceSym, source);
            }
            
            Symbol value = w.getValue();
            Node target = value.asIdentifier() != null ? nodeMap.get(w.getValue()) : null;
            if(target == null)
            {
                target = g.addNode();
                target.set("value", value);
                if(value.asIdentifier() != null)
                {
                    nodeMap.put(value.asIdentifier(), target);
                }
                
                if(!sourceIsNew)
                {
                    NodeItem sourceItem = (NodeItem) m_vis.getVisualItem(nodes, source);
                    NodeItem targetItem = (NodeItem) m_vis.getVisualItem(nodes, target);
                    
                    final double x2 = sourceItem.getX();
                    targetItem.setX(x2);
                    final double y2 = sourceItem.getY();
                    targetItem.setY(y2);
                }
            }
            Edge e = g.addEdge(source, target);
            e.set("value", w);
            edgeMap.put(w, e);
        }
        
        for(Identifier s : possibleDeadNodes)
        {
            Node n = nodeMap.remove(s);
            if(n != null)
            {
                g.removeNode(n);
            }
        }
        
        //m_vis.setValue(edges, null, VisualItem.INTERACTIVE, Boolean.FALSE);
    }

    /**
     * @return
     */
    private Graph createEmptyGraph()
    {
        Graph g = new Graph(true);
        g.addColumn("value", Object.class);
        return g;
    }

    /* (non-Javadoc)
     * @see org.jsoar.debugger.AbstractAdaptableView#getAdapter(java.lang.Class)
     */
    @Override
    public Object getAdapter(Class<?> klass)
    {
        if(SelectionProvider.class.equals(klass))
        {
            return focusSetListener;
        }
        return super.getAdapter(klass);
    }

    private final class FocusSetListener implements TupleSetListener, SelectionProvider
    {
        private SelectionManager manager;
        public void tupleSetChanged(TupleSet ts, Tuple[] add, Tuple[] rem)
        {
            try
            {
                for ( int i=0; i<rem.length; ++i )
                    ((VisualItem)rem[i]).setFixed(false);
                for ( int i=0; i<add.length; ++i ) {
                    ((VisualItem)add[i]).setFixed(false);
                    ((VisualItem)add[i]).setFixed(true);
                }
                if ( ts.getTupleCount() == 0 ) {
                    ts.addTuple(rem[0]);
                    ((VisualItem)rem[0]).setFixed(false);
                }
            }
            catch(IllegalArgumentException e) 
            {
                // Stupid.
            }
            
            m_vis.run("draw");
            
            if(manager != null)
            {
                manager.fireSelectionChanged();
            }
        }
        
        /* (non-Javadoc)
         * @see org.jsoar.debugger.selection.SelectionProvider#activate(org.jsoar.debugger.selection.SelectionManager)
         */
        @Override
        public void activate(SelectionManager manager)
        {
            this.manager = manager;
        }

        /* (non-Javadoc)
         * @see org.jsoar.debugger.selection.SelectionProvider#deactivate()
         */
        @Override
        public void deactivate()
        {
            this.manager = null;
        }

        /* (non-Javadoc)
         * @see org.jsoar.debugger.selection.SelectionProvider#getSelectedObject()
         */
        @Override
        public Object getSelectedObject()
        {
            List<Object> selection = getSelection();
            return selection.isEmpty() ? null : selection.get(0);
        }

        /* (non-Javadoc)
         * @see org.jsoar.debugger.selection.SelectionProvider#getSelection()
         */
        @Override
        public List<Object> getSelection()
        {
            List<Object> result = new ArrayList<Object>();
            TupleSet focusGroup = m_vis.getGroup(Visualization.FOCUS_ITEMS);
            Iterator<?> it = focusGroup.tuples();
            while(it.hasNext())
            {
                VisualItem vi = (VisualItem) it.next();
                if(!vi.isValid())
                {
                    continue;
                }
                Object value = vi.get("value");
                result.add(value);
            }
            return result;
        }
    }

}

/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 16, 2010
 */
package org.jsoar.debugger.wm;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.OutputStreamWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.jsoar.kernel.RunType;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.SwingCompletionHandler;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.SwingTools;
import org.jsoar.util.commands.SoarCommands;

/**
 * @author ray
 */
public class WorkingMemoryTree extends JComponent
{
    private static final long serialVersionUID = 8031999540064492987L;

    private static final Font font = Font.decode("Arial-BOLD");
    private static final Font rootFont = Font.decode("Arial-BOLD").deriveFont(font.getSize() * 1.7f);
    private static final Stroke selectionStroke = new BasicStroke(2);
    private static final Stroke markerStroke = new BasicStroke(2);
    private static final Stroke newWmeStroke = new BasicStroke(2);

    private final Model model;
    
    private Color alternateRootFillColor = new Color(248, 248, 248);
    private Color idTextColor = Color.BLACK;
    private Color idFillColor = new Color(192, 192, 192);
    private Color currentIdFillColor = new Color(225, 225, 225);
    private Color stringTextColor = new Color(0, 200, 0);
    private Color numberTextColor = new Color(0, 0, 200);
    private Color otherTextColor = Color.YELLOW;
    private Color selectionColor = new Color(0, 0, 255);
    private Color selectionSubColor = new Color(232, 242, 254);
    
    private Symbol symbolUnderMouse = null;
    private final List<Wme> selectedWmes = new ArrayList<Wme>();
    
    private Point offset = new Point();
    private Point lastMouseDragPoint = null;
    
    private final CompletionHandler<Void> repaint = SwingCompletionHandler.newInstance(new CompletionHandler<Void>()
    {
        @Override
        public void finish(Void result)
        {
            repaint();
        }
    });
    
    public WorkingMemoryTree(ThreadedAgent agent)
    {
        this.model = new Model(agent);

        setFont(font);
        setBackground(Color.WHITE);
        //expandId('S', 1, 0, 0);
        
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e)
            {
                synchronized(model.lock)
                {
                    WorkingMemoryTree.this.mouseMoved(e);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e)
            {
                synchronized(model.lock)
                {
                    WorkingMemoryTree.this.mouseDragged(e);
                }
            }
        });
        addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e)
            {
                synchronized(model.lock)
                {
                    WorkingMemoryTree.this.mouseClicked(e);
                }
            }

            @Override
            public void mousePressed(MouseEvent e)
            {
                synchronized(model.lock)
                {
                    WorkingMemoryTree.this.mousePressed(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                synchronized(model.lock)
                {
                    WorkingMemoryTree.this.mouseReleased(e);
                }
            }
        });
    }
    
    
    private int getIndent()
    {
        return 26;
    }
    
    private int getRowHeight()
    {
        return 30;
    }
    
    private Row getRowAtPoint(Point p)
    {
        int rowNum = (p.y - offset.y) / getRowHeight();
        
        return rowNum < model.rows.size() ? model.rows.get(rowNum) : null;
    }
    
    private boolean isSelected(Wme wme)
    {
        return selectedWmes.contains(wme);
    }

    private boolean isRowSelected(WmeRow row)
    {
        for(WmeRow.Value v : row.values)
        {
            if(isSelected(v.wme))
            {
                return true;
            }
        }
        return false;
    }
    
    /* (non-Javadoc)
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        SwingTools.enableAntiAliasing(g);
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        
        final Graphics2D g2d = (Graphics2D) g;
        
        synchronized(model.lock)
        {
            paintComponentSafe(g2d);
        }
    }
    
    protected void paintComponentSafe(Graphics2D g2d)
    {
        final ArrayDeque<Integer> currentIdMarkers = new ArrayDeque<Integer>();
        
        RootRow lastRoot = null;
        boolean alternateRoot = true;
        
        for(Row row : model.rows)
        {
            final int y = offset.y + row.row * getRowHeight();
            
            final RootRow asRoot = row.asRoot();
            final WmeRow asWme = row.asWme();
            
            if(asRoot != null && asRoot != lastRoot)
            {
                lastRoot = asRoot;
                alternateRoot = !alternateRoot;
            }
            
            if(asRoot != null)
            {
                paintRootRow(asRoot, g2d, y);
                continue;
            }
            
            g2d.setFont(font);
            
            if(isRowSelected(asWme))
            {
                g2d.setColor(selectionSubColor);
                g2d.fillRect(0, y, getWidth(), getRowHeight());
            }
            else if(alternateRoot)
            {
                g2d.setColor(alternateRootFillColor);
                g2d.fillRect(0, y, getWidth(), getRowHeight());
            }
            
            
            final Integer lastMarker = currentIdMarkers.peekLast();
            boolean start = false;
            boolean end = false;
            if(asWme.id == symbolUnderMouse && (lastMarker == null || lastMarker < row.level))
            {
                start = true;
                currentIdMarkers.add(row.level);
            }
            else if(asWme.id != symbolUnderMouse && lastMarker != null && lastMarker >= row.level)
            {
                end = true;
                currentIdMarkers.pop();
            }
            final Stroke oldStroke = g2d.getStroke();
            g2d.setStroke(markerStroke);
            g2d.setColor(idFillColor);
            if(start)
            {
                final int x = row.level * getIndent();
                g2d.drawLine(x - 2, y, x + 3, y);
            }
            for(Integer i : currentIdMarkers)
            {
                final int x = i * getIndent();
                g2d.drawLine(x - 2, y, x - 2, y + getRowHeight());
            }
            if(end)
            {
                final int x = lastMarker * getIndent();
                g2d.drawLine(x - 2, y, x + 3, y);
            }
            g2d.setStroke(oldStroke);
            
            paintRow(g2d, asWme, y, currentIdMarkers);
            
            g2d.setColor(Color.BLACK);
            //g2d.drawLine(0, y, getWidth(), y);
        }
    }


    private void paintRootRow(final RootRow asRoot, Graphics2D g2d, final int y)
    {
        g2d.setColor(Color.BLACK);
        g2d.setFont(rootFont);
        final int rowHeight = getRowHeight();
        final String text = String.format("%s  (%d)", asRoot.id, asRoot.children.size());
        final Rectangle2D bounds = g2d.getFontMetrics().getStringBounds(text, g2d);
        bounds.setFrame(5, y + (rowHeight / 2 - bounds.getHeight() / 2), bounds.getWidth(), bounds.getHeight());
        
        g2d.drawString(text, (int) bounds.getX(), (int) (bounds.getY() + bounds.getHeight()));
        g2d.drawLine((int) bounds.getMaxX() + 10, (int) bounds.getMaxY(), 
                      getWidth() - 10, (int) bounds.getMaxY());
    }

    private void paintRow(Graphics2D g2d, WmeRow row, int y, ArrayDeque<Integer> currentIdMarkers)
    {
        final int startX = 5 + getIndent() * row.level; 
        final int xpad = 15;
        int x = startX;
        row.idBounds = paintSymbol(g2d, row.id, row.id.toString(), x, y);
        x += row.idBounds.getWidth() + xpad;
        row.attrBounds = paintSymbol(g2d, row.attr, "^" + row.attr.toString(), x, y);
        x += row.attrBounds.getWidth() + xpad;
        for(WmeRow.Value value : row.values)
        {
            final String s = value.wme.getValue() + (value.wme.isAcceptable() ? " +" : "");
            value.bounds = paintSymbol(g2d, value.wme.getValue(), s, x, y);
            if(isSelected(value.wme))
            {
                final Stroke oldStroke = g2d.getStroke();
                g2d.setStroke(selectionStroke);
                final int fillPad = 4;
                final int top = ((int) value.bounds.getY() - fillPad);
                final int height = (int) value.bounds.getHeight() + fillPad * 2;
                g2d.setColor(selectionColor);
                g2d.drawRoundRect((int) value.bounds.getX() - fillPad, top, 
                                  (int) value.bounds.getWidth() + fillPad * 2, height, 4, 4);
                g2d.setStroke(oldStroke);
            }
            
            if(value.expanded)
            {
                g2d.setColor(idFillColor);
                g2d.fillArc((int) value.bounds.getX(), (int) value.bounds.getMaxY(), 
                            (int) value.bounds.getWidth(), (int) (getRowHeight() - value.bounds.getHeight()), 
                            45, 90);
            }
            if(model.isNew(value))
            {
                final Stroke oldStroke = g2d.getStroke();
                g2d.setColor(Color.ORANGE);
                g2d.setStroke(newWmeStroke);
                g2d.drawLine((int) value.bounds.getMinX(), (int) value.bounds.getMaxY() + 4, 
                        (int) value.bounds.getMaxX(), (int) value.bounds.getMaxY() + 4);
                g2d.drawLine((int) value.bounds.getMinX(), (int) value.bounds.getMaxY() + 8, 
                        (int) value.bounds.getMaxX(), (int) value.bounds.getMaxY() + 8);
                g2d.setStroke(oldStroke);
            }
            x += value.bounds.getWidth() + xpad;
        }
        
        g2d.setColor(Color.BLACK);
        g2d.drawString(Integer.toString(row.row), getWidth() - 40, (int) (row.idBounds.getY() + row.idBounds.getHeight()));
    }
    
    private Rectangle2D paintSymbol(Graphics2D g2d, Symbol sym, String string, int x, int y)
    {
        final int rowHeight = getRowHeight();
        final Rectangle2D bounds = g2d.getFontMetrics().getStringBounds(string, g2d);
        bounds.setFrame(x, y + (rowHeight / 2 - bounds.getHeight() / 2), bounds.getWidth(), bounds.getHeight());
        
        final Identifier id = sym.asIdentifier();
        final Color textColor;
        if(id != null)
        {
            textColor = idTextColor;
        }
        else if(sym.asString() != null)
        {
            textColor = stringTextColor;
        }
        else if(sym.asDouble() != null || sym.asInteger() != null)
        {
            textColor = numberTextColor;
        }
        else
        {
            textColor = otherTextColor;
        }

        if(id != null)
        {
            g2d.setColor(id == symbolUnderMouse ? currentIdFillColor : idFillColor);
            final int fillPad = 3;
            g2d.fillRoundRect((int) bounds.getX() - fillPad, ((int) bounds.getY() - fillPad), 
                              (int) bounds.getWidth() + fillPad * 2, (int) bounds.getHeight() + fillPad * 2, 4, 4);
        }
        else if(sym == symbolUnderMouse)
        {
            final int fillPad = 3;
            g2d.setColor(textColor);
            g2d.drawRoundRect((int) bounds.getX() - fillPad, ((int) bounds.getY() - fillPad), 
                              (int) bounds.getWidth() + fillPad * 2, (int) bounds.getHeight() + fillPad * 2, 4, 4);
        }

        g2d.setColor(textColor);
        g2d.drawString(string, (int) bounds.getX(), (int) (bounds.getY() + bounds.getHeight()));
        
        return bounds;
    }
    
    private void mousePressed(MouseEvent e)
    {
        lastMouseDragPoint = e.getPoint();
    }
    
    private void mouseReleased(MouseEvent e)
    {
        lastMouseDragPoint = null;
    }
    
    private void mouseDragged(MouseEvent e)
    {
        final Point p = e.getPoint();
        final int dy = p.y - lastMouseDragPoint.y;
        offset.y = Math.min(0, offset.y + dy);
        final int totalHeight = model.rows.size() * getRowHeight();
        if(totalHeight < getHeight())
        {
            offset.y = 0;
        }
        if(totalHeight > getHeight() && offset.y + totalHeight < getHeight())
        {
            offset.y = getHeight() - totalHeight;
        }
        System.out.println(offset);
        
        repaint();
        lastMouseDragPoint = p;
    }
    
    private void mouseMoved(MouseEvent e)
    {
        final Point p = e.getPoint();
        final Row row = getRowAtPoint(p);
        boolean repaint = false;
        if(row != null && row.asWme() != null)
        {
            final WmeRow asWme = row.asWme();
            if(asWme.idBounds.contains(p))
            {
                repaint = symbolUnderMouse != asWme.id;
                symbolUnderMouse = asWme.id;
            }
            else if(asWme.attrBounds.contains(p))
            {
                repaint = symbolUnderMouse != asWme.attr;
                symbolUnderMouse = asWme.attr;
            }
            else
            {
                Symbol value = null;
                for(WmeRow.Value v : asWme.values)
                {
                    if(v.bounds.contains(p))
                    {
                        value = v.wme.getValue();
                        break;
                    }
                }
                repaint = symbolUnderMouse != value;
                symbolUnderMouse = value;
            }
        }
        else
        {
            repaint = symbolUnderMouse != null;
            symbolUnderMouse = null;
        }
       
        if(repaint)
        {
            repaint();
        }
    }
    
    private void mouseClicked(MouseEvent e)
    {
        final boolean singleClick = e.getClickCount() == 1;
        final boolean doubleClick = e.getClickCount() == 2;
        
        final Row row = getRowAtPoint(e.getPoint());
        if(row != null && row.asWme() != null)
        {
            final WmeRow asWme = row.asWme();
            for(WmeRow.Value value : asWme.values)
            {
                if(value.bounds.contains(e.getPoint()))
                {
                    if(SwingUtilities.isLeftMouseButton(e) && doubleClick)
                    {
                        rowValueDoubleClicked(value);
                    }
                    else if(SwingUtilities.isLeftMouseButton(e) && singleClick)
                    {
                        rowValueSingleClicked(e, value);
                    }
                    return;
                }
            }
            if(SwingUtilities.isLeftMouseButton(e))
            {
                selectedWmes.clear();
                repaint();
            }
            if(SwingUtilities.isLeftMouseButton(e) && doubleClick)
            {
                model.expandOrCollapseRow(asWme, repaint);
            }
        }
        else
        {
            if(SwingUtilities.isLeftMouseButton(e))
            {
                selectedWmes.clear();
                repaint();
            }
        }
    }

    private void rowValueSingleClicked(MouseEvent e, WmeRow.Value value)
    {
        if(e.isControlDown())
        {
            if(!selectedWmes.remove(value.wme))
            {
                selectedWmes.add(0, value.wme);
            }
        }
        else
        {
            selectedWmes.clear();
            selectedWmes.add(value.wme);
        }
        repaint();
    }

    private void rowValueDoubleClicked(WmeRow.Value value)
    {
        final Identifier valueId = value.wme.getValue().asIdentifier();
        if(valueId != null)
        {
            if(!value.expanded)
            {
                model.expandRow(value, repaint);
            }
            else
            {
                model.collapseRow(value, repaint);
            }
            repaint();
        }
    }
    
    public static void swingMain() throws Exception
    {
        final JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        final ThreadedAgent agent = ThreadedAgent.create();
        agent.getPrinter().addPersistentWriter(new OutputStreamWriter(System.out));
        agent.executeAndWait(new Callable<Void>() {
            @Override
            public Void call() throws Exception
            {
//                SoarCommands.source(agent.getInterpreter(), 
//                "C:\\Program Files\\Soar\\Soar-Suite-9.3.0-win-x86\\share\\soar\\Demos\\towers-of-hanoi\\towers-of-hanoi.soar");
                SoarCommands.source(agent.getInterpreter(), 
                "../jsoar-demos/demos/scripting/waterjugs-js.soar");
                agent.getTrace().setEnabled(Category.WM_CHANGES, true);
                agent.getAgent().runFor(1, RunType.DECISIONS);
                agent.getAgent().runFor(2, RunType.PHASES);
                return null;
            }
        }, 100000, TimeUnit.DAYS);
        
        final WorkingMemoryTree tree = new WorkingMemoryTree(agent);
        final JPanel panel = new JPanel(new BorderLayout());
        
        final JTextField idField = new JTextField("S1");
        idField.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                final Identifier id = Symbols.parseIdentifier(agent.getSymbols(), idField.getText());
                if(id != null)
                {
                    if(!tree.model.hasRoot(id))
                    {
                        tree.model.addRoot(id, tree.repaint);
                    }
                    else
                    {
                        tree.model.removeRoot(id, tree.repaint);
                    }
                }
            }
        });
        
        panel.add(idField, BorderLayout.NORTH);
        panel.add(tree, BorderLayout.CENTER);
        
        panel.add(new JButton(new AbstractAction("Step")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                agent.getAgent().runFor(1, RunType.PHASES);
                tree.model.update(tree.repaint);
            }
        }), BorderLayout.SOUTH);
        f.setContentPane(panel);
        f.setSize(800, 600);
        f.setVisible(true);
    }
    
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    swingMain();
                }
                catch (Exception e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }
}

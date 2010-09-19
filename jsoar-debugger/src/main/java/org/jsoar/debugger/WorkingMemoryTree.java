/*
 * Copyright (c) 2010 Dave Ray <daveray@gmail.com>
 *
 * Created on Sep 16, 2010
 */
package org.jsoar.debugger;

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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

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

    private static final Font font = Font.decode("Arial-BOLD").deriveFont(Font.BOLD);
    private static final Stroke selectionStroke = new BasicStroke(2);
    private static final Stroke markerStroke = new BasicStroke(2);
    private final ThreadedAgent agent;
    private final Set<Identifier> roots = new HashSet<Identifier>();
    private final ArrayList<Row> rows = new ArrayList<Row>();
    
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
    
    public WorkingMemoryTree(ThreadedAgent agent)
    {
        this.agent = agent;
        setFont(font);
        setBackground(Color.WHITE);
        //expandId('S', 1, 0, 0);
        
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e)
            {
                WorkingMemoryTree.this.mouseMoved(e);
            }

            @Override
            public void mouseDragged(MouseEvent e)
            {
                WorkingMemoryTree.this.mouseDragged(e);
            }
        });
        addMouseListener(new MouseAdapter(){

            @Override
            public void mouseClicked(MouseEvent e)
            {
                WorkingMemoryTree.this.mouseClicked(e);
            }

            @Override
            public void mousePressed(MouseEvent e)
            {
                WorkingMemoryTree.this.mousePressed(e);
            }

            @Override
            public void mouseReleased(MouseEvent e)
            {
                WorkingMemoryTree.this.mouseReleased(e);
            }
        });
    }
    
    public void addRoot(Identifier id)
    {
        if(roots.contains(id))
        {
            
        }
        else
        {
            roots.add(id);
            expandId(id, 0, 0);
        }
    }
    
    public void removeRoot(Identifier id)
    {
        if(roots.remove(id))
        {
            final ListIterator<Row> it = rows.listIterator();
            boolean inRoot = false;
            while(it.hasNext())
            {
                final Row row = it.next();
                if((row.level == 0 && row.id == id) || (row.level > 0 && inRoot))
                {
                    inRoot = true;
                    it.remove();
                }
                else if(row.level == 0 && inRoot)
                {
                    row.row = it.previousIndex();
                    break;
                }
            }
            
            while(it.hasNext())
            {
                it.next().row = it.previousIndex();
            }
            
            repaint();
        }
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
        
        return rowNum < rows.size() ? rows.get(rowNum) : null;
    }
    
    private boolean isSelected(Wme wme)
    {
        return selectedWmes.contains(wme);
    }

    private boolean isRowSelected(Row row)
    {
        for(RowValue v : row.values)
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
        final ArrayDeque<Integer> currentIdMarkers = new ArrayDeque<Integer>();
        
        Identifier lastRoot = null;
        boolean alternateRoot = true;
        
        for(Row row : rows)
        {
            final int y = offset.y + row.row * getRowHeight();
            
            if(row.level == 0 && row.id != lastRoot)
            {
                lastRoot = row.id;
                alternateRoot = !alternateRoot;
            }
            
            if(isRowSelected(row))
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
            if(row.id == symbolUnderMouse && (lastMarker == null || lastMarker < row.level))
            {
                start = true;
                currentIdMarkers.add(row.level);
            }
            else if(row.id != symbolUnderMouse && lastMarker != null && lastMarker >= row.level)
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
            
            paintRow(g2d, row, y, currentIdMarkers);
            
            g2d.setColor(Color.BLACK);
            //g2d.drawLine(0, y, getWidth(), y);
        }
    }

    private void paintRow(Graphics2D g2d, Row row, int y, ArrayDeque<Integer> currentIdMarkers)
    {
        final int startX = 5 + getIndent() * row.level; 
        final int xpad = 15;
        int x = startX;
        row.idBounds = paintSymbol(g2d, row.id, row.id.toString(), x, y);
        x += row.idBounds.getWidth() + xpad;
        row.attrBounds = paintSymbol(g2d, row.attr, "^" + row.attr.toString(), x, y);
        x += row.attrBounds.getWidth() + xpad;
        for(RowValue value : row.values)
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
    
    private void expandRow(Row row, RowValue v)
    {
        if(v.expanded)
        {
            return;
        }
        final Identifier valueId = v.wme.getValue().asIdentifier();
        if(valueId == null)
        {
            return;
        }
        expandId(valueId, row.level + 1, row.row + 1);
        v.expanded = true;
    }
    
    private void expandId(final Identifier id, final int level, final int insertAt)
    {
        final Callable<List<Row>> start = new Callable<List<Row>>()
        {
            @Override
            public List<Row> call() throws Exception
            {
                final List<Row> result = new ArrayList<Row>();
                if(id != null)
                {
                    final Map<Symbol, Row> rowMap = new HashMap<Symbol, Row>();
                    for(Iterator<Wme> it = id.getWmes(); it.hasNext();)
                    {
                        final Wme wme = it.next();
                        Row newRow = rowMap.get(wme.getAttribute());
                        if(newRow == null)
                        {
                            newRow = new Row(level, wme.getIdentifier(), wme.getAttribute());
                            newRow.row = insertAt + result.size();
                            result.add(newRow);
                            rowMap.put(wme.getAttribute(), newRow);
                        }
                        newRow.values.add(new RowValue(wme));
                    }
                }
                return result;
            }
        };
        final CompletionHandler<List<Row>> finish = new CompletionHandler<List<Row>>()
        {
            @Override
            public void finish(List<Row> result)
            {
                rows.addAll(insertAt, result);
                for(int i = insertAt + result.size(); i < rows.size(); i++)
                {
                    rows.get(i).row += result.size();
                }
                repaint();
            }
        };
        agent.execute(start, SwingCompletionHandler.newInstance(finish));
    }
    
    private void collapseRow(Row row, RowValue value)
    {
        if(!value.expanded)
        {
            return;
        }
        
        final ListIterator<Row> it = rows.listIterator(row.row + 1);
        boolean inSubRow = false;
        while(it.hasNext())
        {
            final Row subRow = it.next();
            if(subRow.id == value.wme.getValue() && subRow.level == row.level + 1)
            {
                it.remove();
                inSubRow = true;
            }
            else if(inSubRow && subRow.level > row.level + 1)
            {
                it.remove();
            }
            else if(inSubRow && subRow.level == row.level + 1)
            {
                inSubRow = false;
                subRow.row = it.previousIndex();
            }
            else if(subRow.level <= row.level)
            {
                subRow.row = it.previousIndex();
                break;
            }
            else
            {
                subRow.row = it.previousIndex();
            }
        }
        
        while(it.hasNext())
        {
            final Row trailingRow = it.next();
            trailingRow.row = it.previousIndex();
        }
        value.expanded = false;
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
        final int totalHeight = rows.size() * getRowHeight();
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
        if(row != null)
        {
            if(row.idBounds.contains(p))
            {
                repaint = symbolUnderMouse != row.id;
                symbolUnderMouse = row.id;
            }
            else if(row.attrBounds.contains(p))
            {
                repaint = symbolUnderMouse != row.attr;
                symbolUnderMouse = row.attr;
            }
            else
            {
                Symbol value = null;
                for(RowValue v : row.values)
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
        if(row != null)
        {
            for(RowValue value : row.values)
            {
                if(value.bounds.contains(e.getPoint()))
                {
                    if(SwingUtilities.isLeftMouseButton(e) && doubleClick)
                    {
                        rowValueDoubleClicked(row, value);
                    }
                    else if(SwingUtilities.isLeftMouseButton(e) && singleClick)
                    {
                        rowValueSingleClicked(e, row, value);
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
                expandOrCollapseRow(row);
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

    private void expandOrCollapseRow(Row row)
    {
        boolean expanded = false;
        for(RowValue v : row.values)
        {
            if(v.expanded)
            {
                expanded = true;
                break;
            }
        }
        
        if(expanded)
        {
            for(RowValue v : row.values)
            {
                collapseRow(row, v);
            }
        }
        else
        {
            for(RowValue v : row.values)
            {
                expandRow(row, v);
            }
        }
    }

    private void rowValueSingleClicked(MouseEvent e, Row row, RowValue value)
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

    private void rowValueDoubleClicked(final Row row, RowValue value)
    {
        final Identifier valueId = value.wme.getValue().asIdentifier();
        if(valueId != null)
        {
            if(!value.expanded)
            {
                expandRow(row, value);
            }
            else
            {
                collapseRow(row, value);
            }
            repaint();
        }
    }
    
    private static class Row
    {
        private final int level;
        private int row;
        
        private final Identifier id;
        private Rectangle2D idBounds;
        
        private final Symbol attr;
        private Rectangle2D attrBounds;
        
        private final List<RowValue> values = new ArrayList<RowValue>();

        public Row(int level, Identifier id, Symbol attr)
        {
            this.level = level;
            this.id = id;
            this.attr = attr;
        }
    }
    
    private static class RowValue
    {
        private final Wme wme;
        private boolean expanded;
        private Rectangle2D bounds;

        public RowValue(Wme wme)
        {
            this.wme = wme;
        }
    }
    
    public static void swingMain() throws Exception
    {
        final JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        final ThreadedAgent agent = ThreadedAgent.create();
        agent.executeAndWait(new Callable<Void>() {
            @Override
            public Void call() throws Exception
            {
                SoarCommands.source(agent.getInterpreter(), 
                        "C:\\Program Files\\Soar\\Soar-Suite-9.3.0-win-x86\\share\\soar\\Demos\\towers-of-hanoi\\towers-of-hanoi.soar");
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
                    if(!tree.roots.contains(id))
                    {
                        tree.addRoot(id);
                    }
                    else
                    {
                        tree.removeRoot(id);
                    }
                }
            }
        });
        
        panel.add(idField, BorderLayout.NORTH);
        panel.add(tree, BorderLayout.CENTER);
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

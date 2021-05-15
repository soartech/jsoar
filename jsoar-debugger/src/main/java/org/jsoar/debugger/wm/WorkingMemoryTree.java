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
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Rectangle2D;
import java.io.OutputStreamWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.jsoar.debugger.SwingCompletionHandler;
import org.jsoar.debugger.selection.SelectionManager;
import org.jsoar.debugger.selection.SelectionProvider;
import org.jsoar.debugger.syntax.Highlighter;
import org.jsoar.debugger.util.SwingTools;
import org.jsoar.kernel.RunType;
import org.jsoar.kernel.memory.Wme;
import org.jsoar.kernel.symbols.Identifier;
import org.jsoar.kernel.symbols.Symbol;
import org.jsoar.kernel.symbols.Symbols;
import org.jsoar.kernel.tracing.Trace.Category;
import org.jsoar.runtime.CompletionHandler;
import org.jsoar.runtime.ThreadedAgent;
import org.jsoar.util.commands.SoarCommands;

/**
 * @author ray
 */
public class WorkingMemoryTree extends JComponent
{
    private static final long serialVersionUID = 8031999540064492987L;

    private static final Stroke selectionStroke = new BasicStroke(2);
    private static final Stroke markerStroke = new BasicStroke(2);
    private static final Stroke newWmeStroke = new BasicStroke(3);
    private static final Color newWmeColor = Color.GREEN.darker();

    private final Model model;
    
    private final Font font;
    private final Font rootFont;
    private final Font rootNoteFont;
    private Color backgroundColor = Color.white; //new Color(255, 255, 240);
    private Color rootRowFillColor = new Color(70, 130, 180);
    private Color rootRowTextColor = Color.WHITE;
    private Color idTextColor = Color.BLACK;
    private Color idFillColor = new Color(192, 192, 192);
    private Color currentIdFillColor = new Color(225, 225, 225);
    private Color stringTextColor = new Color(148, 0, 211);
    private Color numberTextColor = new Color(0, 0, 200);
    private Color otherTextColor = Color.YELLOW;
    private Color selectionColor = new Color(0, 0, 255);
    private Color selectionSubColor = new Color(232, 242, 254);
    private Color markerColor = new Color(192, 192, 192);
    private Color stateIndicatorColor = new Color(0, 230, 0);
    private Color operatorIndicatorColor = new Color(255, 160, 122 /*255, 99, 71*/);
    
    private Symbol symbolUnderMouse = null;
    private final List<Wme> selectedWmes = new ArrayList<Wme>();
    private final Provider selectionProvider = new Provider();
    
    private int rowHeight = 26;
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
        setLayout(null);
        
        font = Font.decode("Arial-BOLD-11");
        rootFont = font.deriveFont(font.getSize() * 1.5f);
        rootNoteFont = rootFont.deriveFont(rootFont.getSize() * 0.7f);

        this.model = new Model(agent, getTreeLock());
        this.backgroundColor = Highlighter.getInstance(null).getPatterns().getBackground();

        setFont(font);
        setBackground(backgroundColor);
        setToolTipText("");
        
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
        addMouseWheelListener(e ->
        {
            synchronized (model.lock)
            {
                WorkingMemoryTree.this.mouseScrolled(e);
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
    
    /**
     * @return selection provider for the tree
     */
    public SelectionProvider getSelectionProvider() { return selectionProvider; }
    
    public void updateModel()
    {
        model.update(repaint);
    }
    
    public void addRoot(Object key)
    {
        model.addRoot(key, repaint);
    }
    
    public void removeRoot(Object key)
    {
        model.removeRoot(key, repaint);
    }
    
    private int getIndent()
    {
        return 26;
    }
    
    private int getRowHeight()
    {
        return rowHeight;
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
     * @see javax.swing.JComponent#getToolTipText(java.awt.event.MouseEvent)
     */
    @Override
    public String getToolTipText(MouseEvent event)
    {
        synchronized(model.lock)
        {
            final Point p = event.getPoint();
            final Row row = getRowAtPoint(p);
            if(row == null)
            {
                return "";
            }
            final WmeRow wmeRow = row.asWme();
            if(wmeRow != null)
            {
                for(WmeRow.Value v : wmeRow.values)
                {
                    if(v.bounds != null && v.bounds.contains(p))
                    {
                        return String.format("%#s, timetag: %d", v.wme, v.wme.getTimetag());
                    }
                }
                
                final StringBuilder b = new StringBuilder("<html>");
                final int n = wmeRow.values.size();
                b.append(String.format("<b>%s ^%s</b><br>", wmeRow.id, wmeRow.attr));
                b.append(n + " working memory element" + (n != 1 ? "s" : ""));
                int count = 0;
                for(WmeRow.Value v : wmeRow.values)
                {
                    if(count++ >= 15)
                    {
                        break;
                    }
                    b.append(String.format("<br>- %#s, timetag: %d", v.wme, v.wme.getTimetag()));
                }
                if(wmeRow.values.size() != count)
                {
                    b.append("<br> ... " + ((wmeRow.values.size() - count) + 1) + " more WMEs ...");
                }
                b.append("</html>");
                return b.toString();
            }
            return super.getToolTipText(event);
        }
    }


    /* (non-Javadoc)
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    @Override
    protected void paintComponent(Graphics g)
    {
        SwingTools.enableAntiAliasing(g);
        g.setFont(getFont());
        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());
        rowHeight = (int) (g.getFontMetrics().getHeight() * 1.6);
        
        final Graphics2D g2d = (Graphics2D) g;
        
        synchronized(model.lock)
        {
            paintComponentSafe(g2d);
        }
    }
    
    private void doRowLayout(Graphics2D g2d)
    {
        for(Row row : model.rows)
        {
            final int y = offset.y + row.row * getRowHeight();
            row.bounds = new Rectangle2D.Double(0, y, getWidth(), getRowHeight());
        }
    }
    
    private void paintComponentSafe(Graphics2D g2d)
    {
        doRowLayout(g2d);
        
        final ArrayDeque<Integer> currentIdMarkers = new ArrayDeque<Integer>();
        
        for(Row row : model.rows)
        {
            final RootRow asRoot = row.asRoot();
            final WmeRow asWme = row.asWme();
            
            if(asRoot != null)
            {
                paintRootRow(asRoot, g2d);
                continue;
            }
            
            g2d.setFont(font);
            
            if(isRowSelected(asWme))
            {
                g2d.setColor(selectionSubColor);
                g2d.fill(row.bounds);
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
            g2d.setColor(markerColor);
            final int y = (int) row.bounds.getY();
            if(start)
            {
                final int x = row.level * getIndent() + 2;
                g2d.drawLine(x, y, x + 5, y);
            }
            for(Integer i : currentIdMarkers)
            {
                final int x = i * getIndent() + 2;
                g2d.drawLine(x, y, x, y + getRowHeight());
            }
            if(end)
            {
                final int x = lastMarker * getIndent() + 2;
                g2d.drawLine(x, y, x + 5, y);
            }
            g2d.setStroke(oldStroke);
            
            paintRow(g2d, asWme);
        }
    }

    private Rectangle2D getCenteredTextBounds(Graphics2D g2d, String text, int x, int y)
    {
        final int rowHeight = getRowHeight();
        final Rectangle2D bounds = g2d.getFontMetrics().getStringBounds(text, g2d);
        bounds.setFrame(x, y + (rowHeight / 2 - bounds.getHeight() / 2), bounds.getWidth(), bounds.getHeight());
        return bounds;
    }
    
    private void paintRootRow(final RootRow asRoot, Graphics2D g2d)
    {
        final Identifier id = asRoot.getId();
        g2d.setFont(rootFont);
        
        final String text;
        if(id == asRoot.key)
        {
            text = String.format("%s", id);
        }
        else
        {
            text = String.format("%s %s", asRoot.key, id != null ? id : "");
        }
        
        final Rectangle2D bounds = getCenteredTextBounds(g2d, text, 5, (int) asRoot.bounds.getY());
        
        g2d.setColor(rootRowFillColor);
        g2d.fillRect((int) asRoot.bounds.getX(), (int) asRoot.bounds.getY(), 
                (int) asRoot.bounds.getWidth(), (int) asRoot.bounds.getHeight() - 2);
        
//        g2d.fillRoundRect((int) asRoot.bounds.getX() + 2, (int) asRoot.bounds.getY() + 2, 
//                          (int) asRoot.bounds.getWidth() - 4, (int) asRoot.bounds.getHeight() - 4, 
//                          6, 6);
        g2d.setColor(rootRowTextColor);
        g2d.drawString(text, (int) bounds.getX(), 
                      (int) (bounds.getMaxY() - g2d.getFontMetrics().getDescent()));
        
        String notes = "";
        if(model.isInputLink(asRoot.getId()))
        {
            notes += " (input-link)  ";
        }
        else if(model.isOutputLink(asRoot.getId()))
        {
            notes += "(output-link)  ";
        }
        
        if(id != null)
        {
            final int childCount = asRoot.children.size();
            notes += childCount + " child" + (childCount == 1 ? "" : "ren");
        }
        else
        {
            notes += "Not found";
        }
        
        if(!notes.isEmpty())
        {
            g2d.setFont(rootNoteFont);
            final Rectangle2D noteBounds = getCenteredTextBounds(g2d, text, 5, (int) asRoot.bounds.getY());
            g2d.drawString(notes, (int) bounds.getMaxX() + 10, 
                          (int) (noteBounds.getMaxY() - g2d.getFontMetrics().getDescent()));
        }
        
        if(asRoot.deleteButton.getParent() == null)
        {
            add(asRoot.deleteButton);
            validate();
            asRoot.deleteButton.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    remove(asRoot.deleteButton);
                    validate();
                    model.removeRoot(asRoot.key, repaint);
                }
            });
        }
        asRoot.deleteButton.setBounds((int) asRoot.bounds.getMaxX() - 30, (int) (asRoot.bounds.getCenterY() - 20 / 2), 20, 20);
    }

    private void paintRow(Graphics2D g2d, WmeRow row)
    {
        final int y = (int) row.bounds.getY();
        final int xpad = 15;
        final int startX = 10 + getIndent() * row.level; 
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
                paintSelectionIndicator(g2d, value);
            }
            
            if(model.isNew(value))
            {
                paintNewValueIndicator(g2d, value);
            }
            
            if(value.expanded)
            {
                paintExpandedIdIndicator(g2d, value);
            }
            
            x += value.bounds.getWidth() + xpad;
        }
        
        // Debug
//        g2d.setColor(Color.BLACK);
//        g2d.drawString(Integer.toString(row.row), getWidth() - 40, (int) row.idBounds.getMaxY());
    }

    private void paintSelectionIndicator(Graphics2D g2d, WmeRow.Value value)
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

    private void paintExpandedIdIndicator(Graphics2D g2d, WmeRow.Value value)
    {
        g2d.setColor(idFillColor);
        g2d.fillArc((int) value.bounds.getX(), (int) value.bounds.getMaxY(), 
                    (int) value.bounds.getWidth(), (int) (getRowHeight() - value.bounds.getHeight()), 
                    45, 90);
    }

    private void paintNewValueIndicator(Graphics2D g2d, WmeRow.Value value)
    {
        final Stroke oldStroke = g2d.getStroke();
        // Underline...
        //g2d.setColor(Color.ORANGE);
        //g2d.setStroke(newWmeStroke);
        //g2d.drawLine((int) value.bounds.getMinX(), (int) value.bounds.getMaxY() + 5, 
        //             (int) value.bounds.getMaxX(), (int) value.bounds.getMaxY() + 5);
        
        // "plus" in upper right corner
        g2d.setColor(newWmeColor);
        g2d.setStroke(newWmeStroke);
        final Point c = new Point((int) value.bounds.getMaxX() + 2, (int) value.bounds.getMinY() - 2);
        g2d.drawLine(c.x - 3, c.y, c.x + 3, c.y);
        g2d.drawLine(c.x, c.y - 3, c.x, c.y + 3);
        
        g2d.setStroke(oldStroke);
    }
    
    private Color getSymbolTextColor(Symbol sym)
    {
        if(sym.asIdentifier() != null)
        {
            return idTextColor;
        }
        else if(sym.asString() != null)
        {
            return stringTextColor;
        }
        else if(sym.asDouble() != null || sym.asInteger() != null)
        {
            return numberTextColor;
        }
        else
        {
            return otherTextColor;
        }
    }
    
    private Rectangle2D paintSymbol(Graphics2D g2d, Symbol sym, String string, int x, int y)
    {
        final Rectangle2D bounds = getCenteredTextBounds(g2d, string, x, y);
        
        final Color textColor = getSymbolTextColor(sym);

        final Identifier id = sym.asIdentifier();
        if(id != null)
        {
            g2d.setColor(sym == symbolUnderMouse ? currentIdFillColor : idFillColor);
            final int fillPad = 3;
            final int leftX = (int) bounds.getX() - fillPad;
            final int topY = (int) bounds.getY() - fillPad;
            final int h = (int) bounds.getHeight() + fillPad * 2;
            final int w = (int) bounds.getWidth() + fillPad * 2;
            g2d.fillRoundRect(leftX, topY, 
                              w, h, 4, 4);
            
            if(id.isGoal())
            {
                g2d.setColor(stateIndicatorColor);
                //g2d.fillArc(leftX - fillPad, topY - fillPad, fillPad * 2, fillPad * 2, 0, 360);
                g2d.fillArc(leftX + 2, topY + 2, 
                            w - 4, h - 4, 0, 360);
            }
            else if(id.isOperator())
            {
                g2d.setColor(operatorIndicatorColor);
                //g2d.fillArc(leftX - fillPad, topY - fillPad, fillPad * 2, fillPad * 2, 0, 360);
                g2d.fillArc(leftX + 2, topY + 2, 
                            w - 4, h - 4, 0, 360);
                
            }
        }
        else if(sym == symbolUnderMouse)
        {
            final int fillPad = 3;
            g2d.setColor(textColor);
            g2d.drawRoundRect((int) bounds.getX() - fillPad, ((int) bounds.getY() - fillPad), 
                              (int) bounds.getWidth() + fillPad * 2, (int) bounds.getHeight() + fillPad * 2, 4, 4);
        }

        g2d.setColor(textColor);
        g2d.drawString(string, (int) bounds.getX(), (int) (bounds.getMaxY() - g2d.getFontMetrics().getDescent()));
        
        return bounds;
    }

    private void mouseScrolled(MouseWheelEvent e)
    {
        final int dy = getRowHeight() * -e.getWheelRotation();
        offset.y = Math.min(0, offset.y + dy);
        final int totalHeight = model.rows.size() * getRowHeight();
        if (totalHeight < getHeight()) {
            offset.y = 0;
        }
        if (totalHeight > getHeight() && offset.y + totalHeight < getHeight()) {
            offset.y = getHeight() - totalHeight;
        }

        repaint();
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
            if(asWme.idBounds != null && asWme.idBounds.contains(p))
            {
                repaint = symbolUnderMouse != asWme.id;
                symbolUnderMouse = asWme.id;
            }
            else if(asWme.attrBounds != null && asWme.attrBounds.contains(p))
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
                selectionProvider.selectionChanged();
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
                selectionProvider.selectionChanged();
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
        selectionProvider.selectionChanged();
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
    
    private class Provider implements SelectionProvider
    {
        private SelectionManager manager;
        
        public void selectionChanged()
        {
            if(manager != null)
            {
                manager.fireSelectionChanged();
            }
        }
        
        @Override
        public void activate(SelectionManager manager)
        {
            this.manager = manager;
        }

        @Override
        public void deactivate()
        {
            this.manager = null;
        }

        @Override
        public Object getSelectedObject()
        {
            return !selectedWmes.isEmpty() ? selectedWmes.get(0) : null;
        }

        @Override
        public List<Object> getSelection()
        {
            return new ArrayList<Object>(selectedWmes);
        }
    }
    
    public static void swingMain() throws InterruptedException, ExecutionException, TimeoutException
    {
        final JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        final ThreadedAgent agent = ThreadedAgent.create();
        agent.getPrinter().addPersistentWriter(new OutputStreamWriter(System.out));
        agent.executeAndWait(new Callable<Void>() {
            @Override
            public Void call() throws Exception
            {
                SoarCommands.source(agent.getInterpreter(), 
                "C:\\Program Files\\Soar\\Soar-Suite-9.3.0-win-x86\\share\\soar\\Demos\\towers-of-hanoi\\towers-of-hanoi.soar");
//                agent.getInterpreter().eval(
//                "sp {test (state <s> ^superstate nil) --> (<s> ^<s> <s>)" +
//                "(<s> ^55 hi) (<s> ^(random-float) yum)}");
//                SoarCommands.source(agent.getInterpreter(), 
//                "../jsoar-demos/demos/scripting/waterjugs-js.soar");
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
                final String idString = idField.getText().trim();
                final Object idOrVar;
                if(!idString.startsWith("<"))
                {
                    idOrVar = Symbols.parseIdentifier(agent.getSymbols(), idString);
                }
                else
                {
                    idOrVar = idString;
                }
                if(idOrVar != null)
                {
                    if(!tree.model.hasRoot(idOrVar))
                    {
                        tree.model.addRoot(idOrVar, tree.repaint);
                    }
                    else
                    {
                        tree.model.removeRoot(idOrVar, tree.repaint);
                    }
                }
            }
        });
        
        panel.add(idField, BorderLayout.NORTH);
        panel.add(tree, BorderLayout.CENTER);
        
        panel.add(new JButton(new AbstractAction("Step")
        {
            private static final long serialVersionUID = -3480585677989449955L;

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
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                catch (ExecutionException | TimeoutException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
    }
}
